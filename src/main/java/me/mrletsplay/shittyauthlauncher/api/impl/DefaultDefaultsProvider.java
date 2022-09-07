package me.mrletsplay.shittyauthlauncher.api.impl;

import me.mrletsplay.shittyauthlauncher.api.DefaultsProvider;
import me.mrletsplay.shittyauthlauncher.api.Theme;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;

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

}
