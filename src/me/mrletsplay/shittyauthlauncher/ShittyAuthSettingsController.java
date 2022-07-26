package me.mrletsplay.shittyauthlauncher;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

public class ShittyAuthSettingsController {

	private static final String DEFAULT_THEME = "default";
	private static final String[] THEMES = {DEFAULT_THEME, "dark"};

	@FXML
	private CheckBox checkboxAlwaysPatchAuthlib;

	@FXML
	private CheckBox checkboxUseAdoptium;

	@FXML
	private CheckBox checkboxAlwaysPatchMinecraft;

	@FXML
	private CheckBox checkboxMinimizeLauncher;

	@FXML
	private ComboBox<String> comboBoxTheme;

	@FXML
	void buttonSave(ActionEvent event) {
		ShittyAuthLauncherSettings.setUseAdoptium(checkboxUseAdoptium.isSelected());
		ShittyAuthLauncherSettings.setAlwaysPatchAuthlib(checkboxAlwaysPatchAuthlib.isSelected());
		ShittyAuthLauncherSettings.setAlwaysPatchMinecraft(checkboxAlwaysPatchMinecraft.isSelected());
		ShittyAuthLauncherSettings.setMinimizeLauncher(checkboxMinimizeLauncher.isSelected());
		String theme = comboBoxTheme.getValue();
		ShittyAuthLauncherSettings.setTheme(DEFAULT_THEME.equals(theme) ? null : theme);
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
		comboBoxTheme.setItems(FXCollections.observableArrayList(THEMES));
		String theme = ShittyAuthLauncherSettings.getTheme();
		if(theme == null) theme = DEFAULT_THEME;
		comboBoxTheme.setValue(theme);
	}

}
