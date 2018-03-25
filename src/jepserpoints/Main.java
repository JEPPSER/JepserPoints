package jepserpoints;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Main {
	
	private static double od = 0;

	public static void main(String[] args) {
		File[] files = new File("res").listFiles();
		for (int file = 0; file < files.length; file += 1) {
			try {
				Scanner scan = new Scanner(files[file], "UTF-8");
				double cs = 0;
				String str = scan.nextLine();
				while (!str.contains("[HitObjects]")) {
					if(str.startsWith("CircleSize:")){
						cs = Double.parseDouble(str.split(":")[1]);
					} else if(str.startsWith("OverallDifficulty:")){
						od = Double.parseDouble(str.split(":")[1]);
					}
					str = scan.nextLine();
				}

				ArrayList<Double> allNotes = new ArrayList<Double>();

				double hardestNote = 0;
				long time = 0;
				double oldStreamValue = 0;
				int oldX = 0;
				int oldY = 0;
				int oldoldX = 0;
				int oldoldY = 0;
				double oldSpacing = 0;
				double oldAngle = 0;
				int numberOfObjects = 0;
				int maxCombo = 0;
				int accCombo = 0;

				// Reading hitobjects.
				for (int i = 0; scan.hasNextLine(); i++) {
					String[] parts = scan.nextLine().split(",");
					maxCombo++;
					if(parts.length >= 8){
						maxCombo += Integer.parseInt(parts[6]);
					} else if(parts.length < 8){
						accCombo++;
					}

					// Skip spinners
					if (!parts[3].equals("12")) {
						numberOfObjects++;
						// Get time/speed.
						long nwTime = Long.valueOf(parts[2]);
						long deltaTime = nwTime - time;
						time = nwTime;

						// Get spacing between last object and current object.
						double spacing = 0;
						int x = Integer.valueOf(parts[0]);
						int y = Integer.valueOf(parts[1]);
						if (i > 0) {
							spacing = new Point(oldX, oldY).distance(new Point(x, y));
							spacing = spacing / (109 - 9 * cs);
						}

						// Get angle from last object to current object.
						double angle = 140;
						if (i > 1) {
							angle = getAngle(oldoldX, oldoldY, oldX, oldY, x, y);
						}

						// Get jump/stream values.
						double jumpValue = (double) (deltaTime - 83) / (double) 104;
						if (jumpValue > 1) {
							jumpValue = 1;
						} else if (jumpValue < 0) {
							jumpValue = 0;
						}
						double streamValue = 1 - jumpValue;

						// Calculating aim difficulty with spacing and angle.
						double aimDifficulty = 0;
						if (i > 1) {
							if (streamValue - oldStreamValue > -0.1 && streamValue - oldStreamValue < 0.1) {
								aimDifficulty = Math.pow(spacing, 0.95) * (3 + (180.0 - angle) / 100.0);
							} else {
								aimDifficulty = Math.pow(spacing, 0.95) * 3;
							}
							if(angle > 80 && angle < 100){
								aimDifficulty *= 1.5;
							}
						}

						// Calculating irregularity difficulty.
						double irrDifficulty = 1;

						// Spacing irregularity
						if (streamValue - oldStreamValue > -0.1 && streamValue - oldStreamValue < 0.1) {
							if (oldSpacing > spacing) {
								if (oldSpacing == 0) {
									oldSpacing = 0.01;
								}
								irrDifficulty = 1 + (1 - (spacing / oldSpacing)) * 0.4;
							} else {
								if (spacing == 0) {
									spacing = 0.01;
								}
								irrDifficulty = 1 + (1 - (oldSpacing / spacing)) * 0.4;
							}
						}

						// Angle irregularity
						if (streamValue - oldStreamValue > -0.1 && streamValue - oldStreamValue < 0.1) {
								irrDifficulty *= 1 + (Math.abs(angle - oldAngle) / 180) * 0.4;
						}

						aimDifficulty *= irrDifficulty;
						
						// Small, very simple difficulty increase if object is slider.
						if(parts.length > 5 && parts[5].contains("|")){
							aimDifficulty *= 1.05;
						}

						// Adding speed consideration
						double speedDifficulty = 0;
						speedDifficulty = 100 / Math.pow(deltaTime, 2.7);

						double noteDifficulty = speedDifficulty * aimDifficulty;

						if (noteDifficulty > hardestNote) {
							hardestNote = noteDifficulty;
						}

						// Set old x and y values for future calculations.
						oldoldX = oldX;
						oldoldY = oldY;
						oldX = x;
						oldY = y;
						oldStreamValue = streamValue;
						oldSpacing = spacing;
						oldAngle = angle;
						allNotes.add(noteDifficulty);
					}
				}

				double p95 = getTotalPPValue(allNotes, 95.0, 0, maxCombo, maxCombo, accCombo);
				double p98 = getTotalPPValue(allNotes, 98.0, 0, maxCombo, maxCombo, accCombo);
				double p99 = getTotalPPValue(allNotes, 99.0, 0, maxCombo, maxCombo, accCombo);
				double p100 = getTotalPPValue(allNotes, 100.0, 0, maxCombo, maxCombo, accCombo);
				
				System.out.println(files[file].getName());
				System.out.println("95%: " + Math.round(p95) + "  98%: " + Math.round(p98) + "  99%: " + Math.round(p99) + "  100%: " + Math.round(p100));
				System.out.println();
				scan.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private static double getTotalPPValue(ArrayList<Double> list, double acc, int missCount, int maxCombo, int potMaxCombo, int accCombo) {
		double difficulty = 0;
		Collections.sort(list);
		for (int i = 0; i < list.size() && i < 60; i++) {
			difficulty += list.get(list.size() - 1 - i) * Math.pow(1, i);
		}
		
		// Adjustment to match values of ppv2.
		difficulty *= 1600;
		
		// Calculate length bonus
		double lengthBonus = 0.95 + 0.1 * Math.min(1.0, (double) maxCombo / 2000.0) +
				(maxCombo > 2000 ? Math.log10((double) maxCombo / 2000.0) * 0.5 : 0.0);
		difficulty *= lengthBonus;

		// Miss reduction
		difficulty *= Math.pow(0.97, missCount);
		
		// Combo scaling
		if(maxCombo > 0){
			difficulty *= Math.min(Math.pow(potMaxCombo, 0.8) / Math.pow(maxCombo, 0.8), 1.0);
		}
		
		// Calculate accuracy
		double accValue = Math.pow(1.52163, od) * Math.pow(acc/100, 24) * 2.83;
		accValue *= Math.min(1.15, Math.pow(accCombo / 1000.0, 0.3));
		difficulty += accValue;
		
		return difficulty;
	}

	private static double getAngle(int x1, int y1, int x2, int y2, int x3, int y3) {
		double a = new Point(x3, y3).distance(new Point(x2, y2));
		double b = new Point(x3, y3).distance(new Point(x1, y1));
		double c = new Point(x1, y1).distance(new Point(x2, y2));
		double cos = (a * a + c * c - b * b) / (2 * c * a);
		cos = (double) Math.round(cos * 1000) / (double) 1000;
		double angle = 180 - Math.toDegrees(Math.acos(cos));
		return angle;
	}
}
