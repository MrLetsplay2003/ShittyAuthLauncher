package me.mrletsplay.shittyauthlauncher;

import javafx.scene.Parent;
import javafx.scene.Scene;
import me.mrletsplay.shittyauthlauncher.api.Theme;

public class Theming {

	public static Theme getDefaultTheme() {
		return ShittyAuthLauncherPlugins.getDefaultsProvider().getDefaultTheme();
	}

	public static void updateTheme(Theme theme) {
		updateTheme(ShittyAuthLauncher.stage.getScene(), theme);
		updateTheme(ShittyAuthLauncher.settingsStage.getScene(), theme);
	}

	public static void updateTheme(Parent p, Theme theme) {
		p.getStylesheets().clear();
		p.getStylesheets().addAll(theme.getStylesheets());
	}

	public static void updateTheme(Scene s, Theme theme) {
		s.getStylesheets().clear();
		s.getStylesheets().addAll(theme.getStylesheets());
	}

}
