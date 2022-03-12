package me.mrletsplay.shittyauthlauncher;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
    void buttonSave(ActionEvent event) {
    	String minecraftPath = inputMinecraftPath.getText();
    	String gameDataPath = inputGameDataPath.getText();
    	String newJavaPath = inputNewJavaPath.getText();
    	String oldJavaPath = inputOldJavaPath.getText();
    	if(minecraftPath.isBlank()) minecraftPath = null;
    	if(gameDataPath.isBlank()) gameDataPath = null;
    	if(newJavaPath.isBlank()) newJavaPath = null;
    	if(oldJavaPath.isBlank()) oldJavaPath = null;
    	ShittyAuthLauncherSettings.setMinecraftPath(minecraftPath);
    	ShittyAuthLauncherSettings.setGameDataPath(gameDataPath);
    	ShittyAuthLauncherSettings.setNewJavaPath(newJavaPath);
    	ShittyAuthLauncherSettings.setOldJavaPath(oldJavaPath);
    	ShittyAuthLauncher.settingsStage.hide();
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
    }

}
