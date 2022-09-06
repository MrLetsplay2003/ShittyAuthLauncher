package me.mrletsplay.shittyauthlauncher;

import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import me.mrletsplay.shittyauthlauncher.api.Theme;

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
	private ComboBox<Theme> comboBoxTheme;

	@FXML
	private ListView<String> listViewPlugins;

	@FXML
	void buttonSave(ActionEvent event) {
		ShittyAuthLauncherSettings.setUseAdoptium(checkboxUseAdoptium.isSelected());
		ShittyAuthLauncherSettings.setAlwaysPatchAuthlib(checkboxAlwaysPatchAuthlib.isSelected());
		ShittyAuthLauncherSettings.setAlwaysPatchMinecraft(checkboxAlwaysPatchMinecraft.isSelected());
		ShittyAuthLauncherSettings.setMinimizeLauncher(checkboxMinimizeLauncher.isSelected());
		Theme theme = comboBoxTheme.getValue();
		ShittyAuthLauncherSettings.setTheme(theme == Theming.getDefaultTheme() ? null : theme.getID());
		ShittyAuthLauncherSettings.save();
		ShittyAuthLauncher.settingsStage.hide();
		Theming.updateTheme(theme);
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
		comboBoxTheme.setItems(FXCollections.observableArrayList(ShittyAuthLauncherPlugins.getThemes()));
		String theme = ShittyAuthLauncherSettings.getTheme();
		Theme th = ShittyAuthLauncherPlugins.getTheme(theme);
		if(th == null) th = Theming.getDefaultTheme();
		listViewPlugins.setItems(FXCollections.observableArrayList(ShittyAuthLauncherPlugins.getPlugins().stream()
			.map(p -> String.format("%s (version %s) by %s", p.getPluginId(), p.getDescriptor().getVersion(), p.getDescriptor().getProvider()))
			.collect(Collectors.toList())));
		comboBoxTheme.setValue(th);
	}

}
