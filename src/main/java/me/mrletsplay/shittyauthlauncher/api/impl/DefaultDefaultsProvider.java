package me.mrletsplay.shittyauthlauncher.api.impl;

import me.mrletsplay.shittyauthlauncher.api.DefaultsProvider;
import me.mrletsplay.shittyauthlauncher.api.Theme;
import me.mrletsplay.shittyauthlauncher.locale.Locale;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public class DefaultDefaultsProvider implements DefaultsProvider {

	public static final DefaultDefaultsProvider INSTANCE = new DefaultDefaultsProvider();

	protected DefaultDefaultsProvider() {}

	@Override
	public Theme getDefaultTheme() {
		return DefaultThemeProvider.NO_THEME;
	}

	@Override
	public DownloadsMirror getDefaultMirror() {
		return DownloadsMirror.MOJANG;
	}

	@Override
	public ServerConfiguration getDefaultServerConfiguration() {
		return null;
	}

	@Override
	public boolean allowCustomServerConfigurations() {
		return true;
	}

	@Override
	public Locale getDefaultLocale() {
		return DefaultLocaleProvider.INSTANCE.getLocales().get(0);
	}

}
