package me.mrletsplay.shittyauthlauncher.api;

import java.io.InputStream;

import org.pf4j.ExtensionPoint;

import javafx.scene.image.Image;

public interface BrandingProvider extends ExtensionPoint {

	/**
	 * @return The launcher brand, used in the UI as well as requests to authentication servers
	 */
	public String getLauncherBrand();

	/**
	 * @return The launcher version, used in requests to authentication servers
	 */
	public String getLauncherVersion();

	/**
	 * @return An {@link InputStream} to the launcher's icon. Must be compatible with JavaFX {@link Image}
	 */
	public InputStream loadIcon();

}
