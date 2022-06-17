package me.mrletsplay.shittyauthlauncher;

import java.io.File;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ShittyAuthLauncher extends Application {

	public static Stage stage;
	public static Stage settingsStage;
	public static ShittyAuthController controller;
	private static ShittyAuthSettingsController settingsController;

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
		settingsController = l2.getController();
		settingsController.init();

		Scene settingsScene = new Scene(settings);

		settingsStage = new Stage();
		settingsStage.setTitle(ShittyAuthLauncherSettings.LAUNCHER_BRAND + " - Settings");
		settingsStage.setScene(settingsScene);
		settingsStage.initOwner(stage);
		settingsStage.sizeToScene();
		settingsStage.setOnShown(event -> {
			settingsController.update();
			settingsStage.setMinWidth(settings.prefWidth(-1));
			settingsStage.setMinHeight(settings.prefHeight(-1));
			settingsStage.setWidth(settings.prefWidth(-1));
			settingsStage.setHeight(settings.prefHeight(-1));
		});

		settingsStage.setOnCloseRequest(e -> {
			settingsStage.hide();
		});

		URL iconURL = ShittyAuthLauncher.class.getResource("/include/icon.png");
		if(iconURL == null) iconURL = new File("./include/icon.png").toURI().toURL();

		primaryStage.getIcons().add(new Image(iconURL.openStream()));

		Scene sc = new Scene(pr, 720, 480);
		primaryStage.setTitle(ShittyAuthLauncherSettings.LAUNCHER_BRAND);
		primaryStage.setMinWidth(720);
		primaryStage.setMinHeight(480);
		primaryStage.setScene(sc);
		primaryStage.show();
	}

}
