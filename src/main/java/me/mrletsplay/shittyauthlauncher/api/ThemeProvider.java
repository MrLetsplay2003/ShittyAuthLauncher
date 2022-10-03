package me.mrletsplay.shittyauthlauncher.api;

import java.util.Collection;

import org.pf4j.ExtensionPoint;

public interface ThemeProvider extends ExtensionPoint {

	/**
	 * @return The themes to add to the list of {@link Theme}s the user can select in the launcher
	 */
	public Collection<Theme> getThemes();

}
