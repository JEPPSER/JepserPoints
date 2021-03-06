package jepserpoints;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Performance {

	private File file;

	private double od = 0;
	private double cs = 0;
	private double ar = 0;
	private double timeMultiplier = 1;

	private boolean hidden = false;
	private boolean flashlight = false;

	public Performance(File file) {
		this.file = file;
	}

	public double calculatePP(Mod[] mods, int combo, double acc, int missCount) {
		try {
			Scanner scan = new Scanner(file, "UTF-8");
			String str = scan.nextLine();
			while (!str.contains("[HitObjects]")) {
				if (str.startsWith("CircleSize:")) {
					cs = Double.parseDouble(str.split(":")[1]);
				} else if (str.startsWith("OverallDifficulty:")) {
					od = Double.parseDouble(str.split(":")[1]);
				} else if (str.startsWith("ApproachRate:")) {
					ar = Double.parseDouble(str.split(":")[1]);
				}
				str = scan.nextLine();
			}

			for (int i = 0; i < mods.length; i++) {
				if (mods[i] == Mod.doubletime) {
					useDoubleTime();
				} else if (mods[i] == Mod.hardrock) {
					useHardRock();
				} else if (mods[i] == Mod.hidden) {
					hidden = true;
				} else if (mods[i] == Mod.flashlight) {
					flashlight = true;
				}
			}

			ArrayList<Double> allNotes = new ArrayList<Double>();

			long time = 0;
			int oldX = 0;
			int oldY = 0;
			int maxCombo = 0;
			int accCombo = 0;
			double hardestNote = 0;

			// Reading hitobjects.
			for (int i = 0; scan.hasNextLine(); i++) {
				String[] parts = scan.nextLine().split(",");
				maxCombo++;
				if (parts.length >= 8) {
					maxCombo += Integer.parseInt(parts[6]);
				} else if (parts.length < 8) {
					accCombo++;
				}

				// Skip spinners
				if (!parts[3].equals("12")) {
					// Get time/speed.
					long nwTime = Long.valueOf(parts[2]);
					long deltaTime = nwTime - time;
					time = nwTime;
					deltaTime /= timeMultiplier;

					// Get spacing between last object and current object.
					double spacing = 0;
					int x = Integer.valueOf(parts[0]);
					int y = Integer.valueOf(parts[1]);
					if (i > 0) {
						spacing = new Point(oldX, oldY).distance(new Point(x, y));
						spacing = spacing / (109 - 9 * cs);
					}

					// Calculating aim difficulty with spacing and angle.
					double aimDifficulty = 0;
					if (i > 1) {
						aimDifficulty = Math.pow(spacing, 1) * 5;
					}

					// Small, very simple difficulty increase if object is
					// slider.
					if (parts.length > 5 && parts[5].contains("|")) {
						aimDifficulty *= 1;
					}

					// Adding speed consideration
					double speedDifficulty = 0;
					speedDifficulty = 100 / Math.pow(deltaTime, 2.7);

					double noteDifficulty = speedDifficulty * aimDifficulty;

					// Set old x and y values for future calculations.
					oldX = x;
					oldY = y;
					allNotes.add(noteDifficulty);

					if (noteDifficulty > hardestNote) {
						hardestNote = noteDifficulty;
					}
				}
			}

			double p95 = getTotalPPValue(allNotes, 95.0, 0, maxCombo, maxCombo, accCombo);
			double p98 = getTotalPPValue(allNotes, 98.0, 0, maxCombo, maxCombo, accCombo);
			double p99 = getTotalPPValue(allNotes, 99.0, 0, maxCombo, maxCombo, accCombo);
			double p100 = getTotalPPValue(allNotes, 100.0, 0, maxCombo, maxCombo, accCombo);

			System.out.println(file.getName());
			System.out.println(ar + ", " + od + ", " + cs);
			System.out.println("95%: " + Math.round(p95) + "  98%: " + Math.round(p98) + "  99%: " + Math.round(p99)
					+ "  100%: " + Math.round(p100));
			System.out.println(hardestNote * 1000);
			System.out.println();

			if (combo == -1) {
				combo = maxCombo;
			}
			scan.close();
			return getTotalPPValue(allNotes, acc, missCount, combo, maxCombo, accCombo);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return 0;
		}
	}

	private double getTotalPPValue(ArrayList<Double> list, double acc, int missCount, int maxCombo, int potMaxCombo,
			int accCombo) {
		double difficulty = 0;
		Collections.sort(list);

		// The hardest notes will be added to form the total value.
		for (int i = 0; i < list.size(); i++) {
			if (i < 80) {
				difficulty += list.get(list.size() - 1 - i) * Math.pow(0.96, i);
			} else {
				difficulty += list.get(list.size() - 1 - i) * 0.01;
			}
		}

		// Adjustment to match values of ppv2.
		difficulty *= 2200;

		double lengthBonus = 0.95 + 0.3 * Math.min(1.0, (double) potMaxCombo / 2000.0);
		difficulty *= lengthBonus;

		// Miss reduction
		difficulty *= Math.pow(0.97, missCount);

		// Combo scaling
		if (maxCombo > 0) {
			difficulty *= Math.min(Math.pow(potMaxCombo, 0.8) / Math.pow(maxCombo, 0.8), 1.0);
		}

		// AR difficulty adjustments
		double arFactor = 1.0;
		if (ar > 10.33) {
			arFactor += 0.4 * (ar - 10.33);
		} else if (ar < 8.0) {
			if (hidden) {
				arFactor += 0.02 * (8.0 - ar);
			} else {
				arFactor += 0.01 * (8.0 - ar);
			}
		}

		difficulty *= arFactor;

		if (hidden) {
			difficulty *= 1.12;
		}

		if (flashlight) {
			difficulty *= 1.0 + (double) (potMaxCombo / 1500.0);
		}

		// Calculate accuracy
		double accValue = Math.pow(1.57, od) * Math.pow(acc / 100, 30) * 2.83;
		accValue *= Math.min(1.15, Math.pow(accCombo / 1000.0, 0.25));
		difficulty += accValue;

		return difficulty;
	}

	private void useDoubleTime() {
		od = msToOd(odToMs(od) * 2 / 3);
		timeMultiplier = 1.5;
		ar = msToAr(arToMs(ar) * 2 / 3.0);
	}

	private void useHardRock() {
		od = Math.min(10, od * 1.4);
		cs *= 1.3;
		ar = Math.min(10, ar * 1.4);
	}

	// Stole this from Tillerino hehehehe xd
	private double odToMs(double od) {
		return 79.5 - Math.ceil(6 * od);
	}

	// This as well hehehehe
	private double msToOd(double ms) {
		return (79.5 - ms) / 6;
	}

	private double arToMs(double ar) {
		if (ar < 5)
			return 1800 - ar * 120;
		return 1200 - 150 * (ar - 5);
	}

	private double msToAr(double ms) {
		if (ms > 1200)
			return (1800 - ms) / 120;
		return (1200 - ms) / 150 + 5;
	}

	private double getAngle(int x1, int y1, int x2, int y2, int x3, int y3) {
		double a = new Point(x3, y3).distance(new Point(x2, y2));
		double b = new Point(x3, y3).distance(new Point(x1, y1));
		double c = new Point(x1, y1).distance(new Point(x2, y2));
		double cos = (a * a + c * c - b * b) / (2 * c * a);
		cos = (double) Math.round(cos * 1000) / (double) 1000;
		double angle = 180 - Math.toDegrees(Math.acos(cos));
		return angle;
	}
}
