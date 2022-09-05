package me.mrletsplay.shittyauthlauncher;

import javafx.scene.Parent;
import javafx.scene.Scene;

public class Theming {

	public static void updateTheme(String theme) {
		updateTheme(ShittyAuthLauncher.stage.getScene());
		updateTheme(ShittyAuthLauncher.settingsStage.getScene());
	}

	public static void updateTheme(Parent p) {
		String theme = ShittyAuthLauncherSettings.getTheme();
		p.getStylesheets().removeIf(ss -> ss.startsWith("/include/theme-"));
		p.getStylesheets().add("/include/theme-" + theme + ".css");
	}

	public static void updateTheme(Scene s) {
		String theme = ShittyAuthLauncherSettings.getTheme();
		s.getStylesheets().removeIf(ss -> ss.startsWith("/include/theme-"));
		s.getStylesheets().add("/include/theme-" + theme + ".css");
	}

}
