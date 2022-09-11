package me.mrletsplay.shittyauthlauncher.api;

import org.pf4j.ExtensionPoint;

import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public interface DefaultsProvider extends ExtensionPoint {

	/**
	 * @return The default theme for the launcher
	 */
	public Theme getDefaultTheme();

	/**
	 * @return The default mirror for the launcher
	 */
	public DownloadsMirror getDefaultMirror();

	/**
	 * Defines the default server configuration to use. <code>null</code> means the user will always have to set the servers manually.
	 * @return The default server configuration
	 */
	public ServerConfiguration getDefaultServerConfiguration();

	/**
	 * @return Whether to allow custom server configurations. {@link #getDefaultServerConfiguration()} must return a non-null value if this method returns <code>false</code>
	 */
	public boolean allowCustomServerConfigurations();

}
