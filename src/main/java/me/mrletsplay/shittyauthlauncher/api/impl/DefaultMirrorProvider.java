package me.mrletsplay.shittyauthlauncher.api.impl;

import java.util.Collections;
import java.util.List;

import me.mrletsplay.shittyauthlauncher.api.MirrorProvider;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;

public class DefaultMirrorProvider implements MirrorProvider {

	public static final DefaultMirrorProvider INSTANCE = new DefaultMirrorProvider();

	private List<DownloadsMirror> mirrors = Collections.singletonList(DownloadsMirror.MOJANG);

	protected DefaultMirrorProvider() {}

	@Override
	public List<DownloadsMirror> getMirrors() {
		return mirrors;
	}

}
