package me.mrletsplay.shittyauthlauncher;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import me.mrletsplay.shittyauthlauncher.api.Theme;

public class ShittyAuthLauncher extends Application {

	public static final Logger LOGGER = LoggerFactory.getLogger(ShittyAuthLauncher.class);

	public static Stage stage;
	public static Stage settingsStage;
	public static ShittyAuthController controller;
	private static ShittyAuthSettingsController settingsController;

	@Override
	public void start(Stage primaryStage) throws Exception {
		ShittyAuthLauncherPlugins.load();
		URL url = ShittyAuthLauncher.class.getResource("/include/ui/launcher.fxml");

		stage = primaryStage;
		stage.setWidth(720);
		stage.setHeight(480);
		stage.setOnCloseRequest(e -> exit());

		FXMLLoader l = new FXMLLoader(url);
		Parent pr = l.load(url.openStream());
		controller = l.getController();
		controller.init();

		URL url2 = ShittyAuthLauncher.class.getResource("/include/ui/settings.fxml");

		FXMLLoader l2 = new FXMLLoader(url2);
		Parent settings = l2.load(url2.openStream());
		settingsController = l2.getController();

		Scene settingsScene = new Scene(settings);

		settingsStage = new Stage();
		settingsStage.setTitle(ShittyAuthLauncherPlugins.getBrandingProvider().getLauncherBrand() + " - Settings");
		settingsStage.setScene(settingsScene);
		settingsStage.initOwner(stage);
		settingsStage.setOnShown(event -> {
			settingsStage.setMinWidth(settingsStage.getWidth());
			settingsStage.setMinHeight(settingsStage.getHeight());
		});

		settingsStage.setOnShowing(event -> {
			settingsController.update();
			settingsStage.sizeToScene();
		});

		settingsStage.setOnCloseRequest(e -> {
			settingsStage.hide();
		});

		primaryStage.getIcons().add(new Image(ShittyAuthLauncherPlugins.getIconProvider().loadLauncherIcon()));

		Scene sc = new Scene(pr, 720, 480);
		primaryStage.setTitle(ShittyAuthLauncherPlugins.getBrandingProvider().getLauncherBrand());
		primaryStage.setMinWidth(720);
		primaryStage.setMinHeight(480);
		primaryStage.setScene(sc);
		primaryStage.show();

		String themeID = ShittyAuthLauncherSettings.getTheme();
		Theme theme = themeID == null ? Theming.getDefaultTheme() : ShittyAuthLauncherPlugins.getTheme(themeID);
		if(theme == null) {
			ShittyAuthLauncher.LOGGER.warn("Couldn't find theme '" + themeID + "'. Falling back to default theme");
			theme = Theming.getDefaultTheme();
		}

		if(theme != null) Theming.updateTheme(theme);
	}

	public static void addTab(String name, Node content) {
		Platform.runLater(() -> controller.addTab(name, content));
	}

	public static void exit() {
		ShittyAuthLauncherPlugins.unload();
	}

}
