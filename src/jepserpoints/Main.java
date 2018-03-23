package jepserpoints;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Main {

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
					}
					str = scan.nextLine();
				}

				ArrayList<Double> allNotes = new ArrayList<Double>();

				double difficulty = 0;
				double hardestNote = 0;
				long time = 0;
				double oldStreamValue = 0;
				double oldoldStreamValue = 0;
				int oldX = 0;
				int oldY = 0;
				int oldoldX = 0;
				int oldoldY = 0;
				double oldSpacing = 0;
				double oldAngle = 0;
				int numberOfObjects = 0;

				// Reading hitobjects.
				for (int i = 0; scan.hasNextLine(); i++) {
					String[] parts = scan.nextLine().split(",");

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
								aimDifficulty = Math.pow(spacing, 1) * (3 + (180.0 - angle) / 100.0);
							} else {
								aimDifficulty = Math.pow(spacing, 1) * 3;
							}
							if(angle > 80 && angle < 100){
								aimDifficulty *= 1.3;
							}
						}

						// Calculating irregularity difficulty.
						double irrDifficulty = 1;

						if (streamValue - oldStreamValue > -0.1 && streamValue - oldStreamValue < 0.1) {
							// Spacing irregularity
							if (oldSpacing > spacing) {
								if (oldSpacing == 0) {
									oldSpacing = 0.01;
								}
								irrDifficulty = 1 + (1 - (spacing / oldSpacing)) * 0.3;
							} else {
								if (spacing == 0) {
									spacing = 0.01;
								}
								irrDifficulty = 1 + (1 - (oldSpacing / spacing)) * 0.3;
							}
						}

						if (streamValue - oldStreamValue > -0.1 && streamValue - oldStreamValue < 0.1
								&& oldStreamValue - oldoldStreamValue > -0.1
								&& oldStreamValue - oldoldStreamValue < 0.1) {
							// Angle irregularity
							if (oldAngle > angle) {
								if (oldAngle == 0) {
									oldAngle = 0.01;
								}
								irrDifficulty *= 1 + (1 - (angle / oldAngle)) * 0;
							} else {
								if (angle == 0) {
									angle = 0.01;
								}
								irrDifficulty *= 1 + (1 - (oldAngle / angle)) * 0;
							}
						}

						aimDifficulty *= irrDifficulty;

						// Adding speed consideration
						double speedDifficulty = 0;
						speedDifficulty = 100 / Math.pow(deltaTime, 2.5);

						double noteDifficulty = speedDifficulty * aimDifficulty;

						if (noteDifficulty > hardestNote) {
							hardestNote = noteDifficulty;
						}

						// Set old x and y values for future calculations.
						oldoldX = oldX;
						oldoldY = oldY;
						oldX = x;
						oldY = y;
						oldoldStreamValue = oldStreamValue;
						oldStreamValue = streamValue;
						oldSpacing = spacing;
						oldAngle = angle;
						allNotes.add(noteDifficulty);
						//System.out.println(noteDifficulty*1000 + ", " + streamValue + ", " + spacing + ", " + angle + ", " + irrDifficulty);
					}
				}

				difficulty = getTotalPPValue(allNotes, numberOfObjects);
				System.out.println(files[file].getName());
				System.out.println("pp for fc: " + Math.round(difficulty));
				System.out.println("Hardest note: " + hardestNote + "\n");
				scan.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private static double getTotalPPValue(ArrayList<Double> list, int num) {
		double difficulty = 0;
		Collections.sort(list);
		for (int i = 0; i < list.size() && i < 70; i++) {
			difficulty += list.get(list.size() - 1 - i) * Math.pow(1, i);
		}
		difficulty *= 850;
		
		double lengthBonus = 0.95 + 0.1 * Math.min(1.0, (double) num / 2000.0) +
				(num > 2000 ? Math.log10((double) num / 2000.0) * 0.5 : 0.0);
		
		difficulty *= lengthBonus;
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
