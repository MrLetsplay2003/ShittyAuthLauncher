package me.mrletsplay.shittyauthlauncher;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class ShittyAuthSettingsController {

    @FXML
    private TextField inputGameDataPath;

    @FXML
    private TextField inputMinecraftPath;

    @FXML
    private TextField inputNewJavaPath;

    @FXML
    private TextField inputOldJavaPath;

    @FXML
    private TextField inputAuthServerURL;

    @FXML
    private TextField inputAccountServerURL;

    @FXML
    private TextField inputSessionServerURL;

    @FXML
    private TextField inputServicesServerURL;

    @FXML
    private CheckBox checkboxAlwaysPatch;

    @FXML
    void buttonSave(ActionEvent event) {
    	ShittyAuthLauncherSettings.setMinecraftPath(getString(inputMinecraftPath));
    	ShittyAuthLauncherSettings.setGameDataPath(getString(inputGameDataPath));
    	ShittyAuthLauncherSettings.setNewJavaPath(getString(inputNewJavaPath));
    	ShittyAuthLauncherSettings.setOldJavaPath(getString(inputOldJavaPath));
    	ShittyAuthLauncherSettings.setAuthServerURL(getString(inputAuthServerURL));
    	ShittyAuthLauncherSettings.setAccountServerURL(getString(inputAccountServerURL));
    	ShittyAuthLauncherSettings.setSessionServerURL(getString(inputSessionServerURL));
    	ShittyAuthLauncherSettings.setServicesServerURL(getString(inputServicesServerURL));
    	ShittyAuthLauncherSettings.setAlwaysPatchAuthlib(checkboxAlwaysPatch.isSelected());
    	ShittyAuthLauncherSettings.save();
    	ShittyAuthLauncher.settingsStage.hide();
    }
    
    private String getString(TextField textField) {
    	String txt = textField.getText();
    	if(txt.isBlank()) txt = null;
    	return txt;
    }

    @FXML
    void buttonCancel(ActionEvent event) {
    	ShittyAuthLauncher.settingsStage.hide();
    }
    
    public void update() {
    	inputGameDataPath.setText(ShittyAuthLauncherSettings.getGameDataPath());
    	inputMinecraftPath.setText(ShittyAuthLauncherSettings.getMinecraftPath());
    	inputNewJavaPath.setText(ShittyAuthLauncherSettings.getNewJavaPath());
    	inputOldJavaPath.setText(ShittyAuthLauncherSettings.getOldJavaPath());
    	inputAuthServerURL.setText(ShittyAuthLauncherSettings.getAuthServerURL());
    	inputAccountServerURL.setText(ShittyAuthLauncherSettings.getAccountServerURL());
    	inputSessionServerURL.setText(ShittyAuthLauncherSettings.getSessionServerURL());
    	inputServicesServerURL.setText(ShittyAuthLauncherSettings.getServicesServerURL());
    	checkboxAlwaysPatch.setSelected(ShittyAuthLauncherSettings.isAlwaysPatchAuthlib());
    }

}
