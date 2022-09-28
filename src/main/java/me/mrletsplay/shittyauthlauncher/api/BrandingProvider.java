package me.mrletsplay.shittyauthlauncher.api;

import org.pf4j.ExtensionPoint;

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
	 * @return The name of the game/modpack, used for the "Play ..." button
	 */
	public String getGameName();

}
