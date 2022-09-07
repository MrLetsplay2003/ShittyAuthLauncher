package me.mrletsplay.shittyauthlauncher.api;

import java.util.List;

import org.pf4j.ExtensionPoint;

import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;

public interface MirrorProvider extends ExtensionPoint {

	public List<DownloadsMirror> getMirrors();

}
