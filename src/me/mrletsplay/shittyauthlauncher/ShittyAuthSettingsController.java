package me.mrletsplay.shittyauthlauncher;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class ShittyAuthSettingsController {

	@FXML
	private TextField inputNewJavaPath;
	
	@FXML
	private TextField inputOldJavaPath;
	
	@FXML
	private TextField inputSkinHost;
	
	@FXML
	private CheckBox checkboxAlwaysPatchAuthlib;
	
	@FXML
	private CheckBox checkboxAlwaysPatchMinecraft;
	
	@FXML
	private CheckBox checkboxMinimizeLauncher;
	
	@FXML
	void buttonSave(ActionEvent event) {
		ShittyAuthLauncherSettings.setNewJavaPath(getString(inputNewJavaPath));
		ShittyAuthLauncherSettings.setOldJavaPath(getString(inputOldJavaPath));
		ShittyAuthLauncherSettings.setAlwaysPatchAuthlib(checkboxAlwaysPatchAuthlib.isSelected());
		ShittyAuthLauncherSettings.setAlwaysPatchMinecraft(checkboxAlwaysPatchMinecraft.isSelected());
		ShittyAuthLauncherSettings.setMinimizeLauncher(checkboxMinimizeLauncher.isSelected());
		ShittyAuthLauncherSettings.save();
		ShittyAuthLauncher.settingsStage.hide();
	}

	private String getString(TextField textField) {
		String txt = textField.getText();
		if(txt == null || txt.isBlank()) txt = null;
		return txt;
	}

	@FXML
	void buttonCancel(ActionEvent event) {
		ShittyAuthLauncher.settingsStage.hide();
	}
	
	public void update() {
		inputNewJavaPath.setText(ShittyAuthLauncherSettings.getNewJavaPath());
		inputOldJavaPath.setText(ShittyAuthLauncherSettings.getOldJavaPath());
		
		checkboxAlwaysPatchAuthlib.setSelected(ShittyAuthLauncherSettings.isAlwaysPatchAuthlib());
		checkboxAlwaysPatchMinecraft.setSelected(ShittyAuthLauncherSettings.isAlwaysPatchMinecraft());
		checkboxMinimizeLauncher.setSelected(ShittyAuthLauncherSettings.isMinimizeLauncher());
	}

}
