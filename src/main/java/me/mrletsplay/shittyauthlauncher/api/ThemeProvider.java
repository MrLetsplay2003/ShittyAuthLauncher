package me.mrletsplay.shittyauthlauncher.api;

import java.util.Collection;

import org.pf4j.ExtensionPoint;

public interface ThemeProvider extends ExtensionPoint {

	public Collection<Theme> getThemes();

}
