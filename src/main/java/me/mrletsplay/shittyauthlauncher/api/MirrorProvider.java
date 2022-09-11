package me.mrletsplay.shittyauthlauncher.api;

import java.util.List;

import org.pf4j.ExtensionPoint;

import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;

public interface MirrorProvider extends ExtensionPoint {

	/**
	 * @return The mirrors to add to the list of mirrors the user can select in the launcher
	 */
	public List<DownloadsMirror> getMirrors();

}
