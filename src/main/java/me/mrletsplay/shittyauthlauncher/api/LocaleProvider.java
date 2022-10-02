package me.mrletsplay.shittyauthlauncher.api;

import java.util.List;

import org.pf4j.ExtensionPoint;

import me.mrletsplay.shittyauthlauncher.locale.Locale;

public interface LocaleProvider extends ExtensionPoint {

	/**
	 * @return A list of {@link Locale}s that can be selected in the launcher
	 */
	public List<Locale> getLocales();

}
