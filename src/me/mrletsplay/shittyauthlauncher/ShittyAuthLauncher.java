package me.mrletsplay.shittyauthlauncher;

import java.io.File;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import me.mrletsplay.shittyauthlauncher.version.MinecraftVersion;

public class ShittyAuthLauncher extends Application {

	public static Stage stage;
	public static Stage settingsStage;
	public static ShittyAuthController controller;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		URL url = ShittyAuthLauncher.class.getResource("/include/launcher.fxml");
		if(url == null) url = new File("./include/launcher.fxml").toURI().toURL();
		
		stage = primaryStage;
		stage.setWidth(720);
		stage.setHeight(480);
		
		FXMLLoader l = new FXMLLoader(url);
		Parent pr = l.load(url.openStream());
		controller = l.getController();
		controller.init();

		URL url2 = ShittyAuthLauncher.class.getResource("/include/settings.fxml");
		if(url2 == null) url2 = new File("./include/settings.fxml").toURI().toURL();
		
		FXMLLoader l2 = new FXMLLoader(url2);
		Parent settings = l2.load(url2.openStream());
		ShittyAuthSettingsController settingsController = l2.getController();
		
		Scene settingsScene = new Scene(settings);
		
		settingsStage = new Stage();
		settingsStage.setTitle("ShittyAuthLauncher - Settings");
		settingsStage.setScene(settingsScene);
		settingsStage.initOwner(stage);
		settingsStage.setResizable(false);
//		settingsStage.setWidth(720);
//		settingsStage.setHeight(480);
		settingsStage.sizeToScene();
		settingsStage.setOnShown(event -> {
			settingsController.update();
		});
		
		settingsStage.setOnCloseRequest(e -> {
			settingsStage.hide();
		});
		
		URL iconURL = ShittyAuthLauncher.class.getResource("/include/icon.png");
		if(iconURL == null) iconURL = new File("./include/icon.png").toURI().toURL();

		primaryStage.getIcons().add(new Image(iconURL.openStream()));
		
		Scene sc = new Scene(pr, 720, 480);
//		primaryStage.setOnCloseRequest(event -> exit());
		primaryStage.setTitle("JJack");
		primaryStage.setResizable(false);
		primaryStage.setScene(sc);
		primaryStage.show();
	}

}
