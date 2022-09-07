package me.mrletsplay.shittyauthlauncher.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import me.mrletsplay.mrcore.json.JSONType;
import me.mrletsplay.mrcore.json.converter.JSONConstructor;
import me.mrletsplay.mrcore.json.converter.JSONConvertible;
import me.mrletsplay.mrcore.json.converter.JSONListType;
import me.mrletsplay.mrcore.json.converter.JSONValue;
import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncherPlugins;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncherSettings;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;
import me.mrletsplay.shittyauthpatcher.version.ImportedMinecraftVersion;
import me.mrletsplay.shittyauthpatcher.version.VersionsList;
import me.mrletsplay.shittyauthpatcher.version.VersionsLoadException;

public class GameInstallation implements JSONConvertible {

	public static final String DEFAULT_IMAGE_DATA;

	static {
		try(InputStream in = ShittyAuthLauncherPlugins.getBrandingProvider().loadIcon()) {
			byte[] bytes = in.readAllBytes();
			DEFAULT_IMAGE_DATA = Base64.getEncoder().encodeToString(bytes);
		} catch (IOException e) {
			throw new FriendlyException(e);
		}
	}

	@JSONValue
	public InstallationType type = InstallationType.CUSTOM;

	@JSONValue
	public String id;

	@JSONValue
	public String name;

	@JSONValue
	public String imageData;

	@JSONValue
	public String gameDirectory;

	@JSONValue
	public String javaPath;

	@JSONValue
	@JSONListType(JSONType.STRING)
	public List<String> jvmArgs;

	@JSONValue
	public String lastVersionId;

	@JSONValue
	public String mirror;

	private VersionsList versions;

	@JSONConstructor
	public GameInstallation() {}

	public GameInstallation(InstallationType type, String id, String name, String imageData, String gameDirectory, String javaPath, List<String> jvmArgs, String lastVersionId) {
		this.type = type;
		this.id = id;
		this.name = name;
		this.imageData = imageData;
		this.gameDirectory = gameDirectory;
		this.javaPath = javaPath;
		this.jvmArgs = jvmArgs;
		this.lastVersionId = lastVersionId;
	}

	private VersionsList loadVersions() {
		VersionsList v = new VersionsList();
		try {
			v.addVersions(getMirror().getVersions());
		}catch(VersionsLoadException e) {
			ShittyAuthLauncher.LOGGER.error("Failed to load version from mirror '" + getMirror().getName() + "'", e);
		}
		v.addVersions(loadVersions(v));
		return v;
	}

	private List<ImportedMinecraftVersion> loadVersions(VersionsList list) {
		File gameDir = new File(gameDirectory);
		File versionsFolder = new File(gameDir, "versions");
		if(!versionsFolder.exists()) return Collections.emptyList();
		ShittyAuthLauncher.LOGGER.info("Loading versions from " + gameDir.getAbsolutePath() + "...");
		List<ImportedMinecraftVersion> versions = new ArrayList<>();
		for(File v : versionsFolder.listFiles()) {
			if(!v.isDirectory()) continue;
			File jsonFile = new File(v, v.getName() + ".json");
			if(!jsonFile.exists()) continue;
			versions.add(new ImportedMinecraftVersion(list, jsonFile));
		}
		return versions;
	}

	public void updateVersions() {
		versions = loadVersions();
	}

	public VersionsList getVersions() {
		if(versions == null) versions = loadVersions();
		return versions;
	}

	public DownloadsMirror getMirror() {
		DownloadsMirror m = ShittyAuthLauncherSettings.getMirror(mirror);
		if(m == null) {
			System.err.println("Installation '" + name + "' references invalid mirror '" + mirror + "'. Falling back to default Mojang mirror");
			return ShittyAuthLauncherPlugins.getDefaultsProvider().getDefaultMirror();
		}
		return m;
	}

	@Override
	public String toString() {
		return name;
	}

}
