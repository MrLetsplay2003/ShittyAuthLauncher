package me.mrletsplay.shittyauthlauncher;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class ShittyAuthSettingsController {

	@FXML
	private CheckBox checkboxAlwaysPatchAuthlib;

	@FXML
	private CheckBox checkboxUseAdoptium;

	@FXML
	private CheckBox checkboxAlwaysPatchMinecraft;

	@FXML
	private CheckBox checkboxMinimizeLauncher;

	@FXML
	void buttonSave(ActionEvent event) {
		ShittyAuthLauncherSettings.setUseAdoptium(checkboxUseAdoptium.isSelected());
		ShittyAuthLauncherSettings.setAlwaysPatchAuthlib(checkboxAlwaysPatchAuthlib.isSelected());
		ShittyAuthLauncherSettings.setAlwaysPatchMinecraft(checkboxAlwaysPatchMinecraft.isSelected());
		ShittyAuthLauncherSettings.setMinimizeLauncher(checkboxMinimizeLauncher.isSelected());
		ShittyAuthLauncherSettings.save();
		ShittyAuthLauncher.settingsStage.hide();
	}

	@FXML
	void buttonCancel(ActionEvent event) {
		ShittyAuthLauncher.settingsStage.hide();
	}

	public void update() {
		checkboxUseAdoptium.setSelected(ShittyAuthLauncherSettings.isUseAdoptium());
		checkboxAlwaysPatchAuthlib.setSelected(ShittyAuthLauncherSettings.isAlwaysPatchAuthlib());
		checkboxAlwaysPatchMinecraft.setSelected(ShittyAuthLauncherSettings.isAlwaysPatchMinecraft());
		checkboxMinimizeLauncher.setSelected(ShittyAuthLauncherSettings.isMinimizeLauncher());
	}

}
