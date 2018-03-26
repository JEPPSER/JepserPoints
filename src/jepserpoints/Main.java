package jepserpoints;

import java.io.File;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Main extends Application{

	public static void main(String[] args) {
		// For testing many maps at once, used for debugging and adjustments.
		File[] files = new File("res").listFiles();
		for (int file = 0; file < files.length; file += 1) {
			Performance p = new Performance(files[file]);
			Mod[] mods = new Mod[4];
			p.calculatePP(mods, -1, 100.0, 0);
		}
		launch();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		VBox root = new VBox();
		root.setSpacing(5);
		root.setPadding(new Insets(5, 5, 5, 5));
		
		FileChooser chooser = new FileChooser();
		
		HBox getFiles = new HBox();
		TextField fileText = new TextField();
		Button fileBtn = new Button("Choose File");
		getFiles.getChildren().addAll(fileText, fileBtn);
		fileBtn.setOnAction(e -> {
			fileText.setText(chooser.showOpenDialog(primaryStage).getAbsolutePath());
		});
		
		HBox getAcc = new HBox();
		TextField acc = new TextField();
		Text accText = new Text("Accuracy ");
		getAcc.getChildren().addAll(accText, acc);
		
		HBox getCombo = new HBox();
		TextField combo = new TextField();
		Text comboText = new Text("Combo ");
		Text fcText = new Text(" fc ");
		CheckBox fccb = new CheckBox();
		
		HBox getMisses = new HBox();
		TextField misses = new TextField();
		misses.setText("0");
		Text missesText = new Text("Misses ");
		getMisses.getChildren().addAll(missesText, misses);
		
		getCombo.getChildren().addAll(comboText, combo, fcText, fccb);
		fccb.setOnAction(e -> {
			if(fccb.isSelected()){
				combo.setEditable(false);
				combo.setDisable(true);
				misses.setText("0");
				misses.setEditable(false);
				misses.setDisable(true);
			} else {
				combo.setEditable(true);
				combo.setDisable(false);
				misses.setEditable(true);
				misses.setDisable(false);
			}
		});
		
		HBox dtBox = new HBox();
		CheckBox dt = new CheckBox();
		Text dtText = new Text("doubletime ");
		dtBox.getChildren().addAll(dtText, dt);
		
		HBox hrBox = new HBox();
		CheckBox hr = new CheckBox();
		Text hrText = new Text("hardrock ");
		hrBox.getChildren().addAll(hrText, hr);
		
		HBox hdBox = new HBox();
		CheckBox hd = new CheckBox();
		Text hdText = new Text("hidden ");
		hdBox.getChildren().addAll(hdText, hd);

		HBox flBox = new HBox();
		CheckBox fl = new CheckBox();
		Text flText = new Text("flashlight ");
		flBox.getChildren().addAll(flText, fl);

		HBox calcBox = new HBox();
		calcBox.setSpacing(10);
		Button calc = new Button("Calculate pp");
		Text ppText = new Text("pp: ");
		calcBox.getChildren().addAll(calc, ppText);
		
		calc.setOnAction(e ->{
			Performance p = new Performance(new File(fileText.getText()));
			Mod[] mods = new Mod[4];
			if(dt.isSelected()){
				mods[0] = Mod.doubletime;
			}
			if(hr.isSelected()){
				mods[1] = Mod.hardrock;
			}
			if(hd.isSelected()){
				mods[2] = Mod.hidden;
			}
			if(fl.isSelected()){
				mods[3] = Mod.flashlight;
			}
			int c = 0;
			if(fccb.isSelected()){
				c = -1;
			} else {
				c = Integer.parseInt(combo.getText());
			}
			int m = Integer.parseInt(misses.getText());
			double a = Double.parseDouble(acc.getText());
			String pp = String.valueOf(p.calculatePP(mods, c, a, m));
			ppText.setText("pp: " + pp);
		});
		
		root.getChildren().addAll(getFiles, getAcc, getCombo, getMisses, dtBox, hrBox, hdBox, flBox, calcBox);
		
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.show();
	}
}
