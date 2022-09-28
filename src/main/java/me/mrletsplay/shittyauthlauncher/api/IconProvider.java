package me.mrletsplay.shittyauthlauncher.api;

import java.io.InputStream;

import org.pf4j.ExtensionPoint;

import javafx.scene.image.Image;

public interface IconProvider extends ExtensionPoint {

	/**
	 * @return An {@link InputStream} to the launcher's icon. Must be compatible with JavaFX {@link Image}
	 */
	public InputStream loadLauncherIcon();

	/**
	 * @return An {@link InputStream} to the default icon for new installations. Must be compatible with JavaFX {@link Image}
	 */
	public InputStream loadDefaultInstallationIcon();

	/**
	 * @return An {@link InputStream} to the default icon for new accounts. Must be compatible with JavaFX {@link Image}
	 */
	public InputStream loadDefaultAccountIcon();

}
