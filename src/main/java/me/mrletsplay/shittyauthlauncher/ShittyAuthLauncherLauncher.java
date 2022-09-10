package me.mrletsplay.shittyauthlauncher;

import java.io.File;

import javafx.application.Application;

public class ShittyAuthLauncherLauncher { // Launches the launcher :)

	public static void main(String[] args) {
		if(System.getProperty("shittyauthlauncher.log-dir") == null)
			System.setProperty("shittyauthlauncher.log-dir", new File(ShittyAuthLauncherSettings.DATA_PATH, "logs").getAbsolutePath());

		Application.launch(ShittyAuthLauncher.class, args);
	}

}
