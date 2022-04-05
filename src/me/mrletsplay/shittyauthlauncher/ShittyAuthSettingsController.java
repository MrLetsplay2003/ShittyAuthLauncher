package me.mrletsplay.shittyauthlauncher;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public class ShittyAuthSettingsController {

    @FXML
    private TextField inputGameDataPath;

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
    private TextField inputSkinHost;

    @FXML
    private CheckBox checkboxAlwaysPatchAuthlib;
    
    @FXML
    private CheckBox checkboxAlwaysPatchMinecraft;

    @FXML
    void buttonSave(ActionEvent event) {
    	ShittyAuthLauncherSettings.setGameDataPath(getString(inputGameDataPath));
    	ShittyAuthLauncherSettings.setNewJavaPath(getString(inputNewJavaPath));
    	ShittyAuthLauncherSettings.setOldJavaPath(getString(inputOldJavaPath));
    	ShittyAuthLauncherSettings.setServers(new ServerConfiguration(
    			getString(inputAuthServerURL),
    			getString(inputAccountServerURL),
    			getString(inputSessionServerURL),
    			getString(inputServicesServerURL)));
    	ShittyAuthLauncherSettings.setSkinHost(getString(inputSkinHost));
    	ShittyAuthLauncherSettings.setAlwaysPatchAuthlib(checkboxAlwaysPatchAuthlib.isSelected());
    	ShittyAuthLauncherSettings.setAlwaysPatchMinecraft(checkboxAlwaysPatchMinecraft.isSelected());
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
    	inputNewJavaPath.setText(ShittyAuthLauncherSettings.getNewJavaPath());
    	inputOldJavaPath.setText(ShittyAuthLauncherSettings.getOldJavaPath());
    	
    	ServerConfiguration servers = ShittyAuthLauncherSettings.getServers();
    	inputAuthServerURL.setText(servers.authServer);
    	inputAccountServerURL.setText(servers.accountsServer);
    	inputSessionServerURL.setText(servers.sessionServer);
    	inputServicesServerURL.setText(servers.servicesServer);
    	inputSkinHost.setText(ShittyAuthLauncherSettings.getSkinHost());
    	checkboxAlwaysPatchAuthlib.setSelected(ShittyAuthLauncherSettings.isAlwaysPatchAuthlib());
    	checkboxAlwaysPatchMinecraft.setSelected(ShittyAuthLauncherSettings.isAlwaysPatchMinecraft());
    }

}
