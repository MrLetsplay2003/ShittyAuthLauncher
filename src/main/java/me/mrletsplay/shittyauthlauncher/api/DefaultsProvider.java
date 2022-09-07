package me.mrletsplay.shittyauthlauncher.api;

import org.pf4j.ExtensionPoint;

import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;

public interface DefaultsProvider extends ExtensionPoint {

	public Theme getDefaultTheme();

	public DownloadsMirror getDefaultMirror();

}
