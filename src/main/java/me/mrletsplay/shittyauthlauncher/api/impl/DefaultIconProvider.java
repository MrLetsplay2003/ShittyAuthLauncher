package me.mrletsplay.shittyauthlauncher.api.impl;

import java.io.InputStream;

import me.mrletsplay.shittyauthlauncher.api.IconProvider;

public class DefaultIconProvider implements IconProvider {

	public static final DefaultIconProvider INSTANCE = new DefaultIconProvider();

	protected DefaultIconProvider() {}

	@Override
	public InputStream loadLauncherIcon() {
		return DefaultIconProvider.class.getResourceAsStream("/include/icon.png");
	}

	@Override
	public InputStream loadDefaultInstallationIcon() {
		return loadLauncherIcon();
	}

	@Override
	public InputStream loadDefaultAccountIcon() {
		return loadLauncherIcon();
	}

}
