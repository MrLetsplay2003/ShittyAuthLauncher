package me.mrletsplay.shittyauthlauncher.util;

import me.mrletsplay.mrcore.json.converter.JSONConstructor;
import me.mrletsplay.mrcore.json.converter.JSONConvertible;
import me.mrletsplay.mrcore.json.converter.JSONValue;

public class GameInstallation implements JSONConvertible {
	
	@JSONValue
	public InstallationType type = InstallationType.CUSTOM;
	
	@JSONValue
	public String id;

	@JSONValue
	public String name;
	
	@JSONValue
	public String gameDirectory;
	
	@JSONValue
	public String javaPath;
	
	@JSONValue
	public String lastVersionId;
	
	@JSONConstructor
	public GameInstallation() {}
	
	public GameInstallation(InstallationType type, String id, String name, String gameDirectory, String javaPath, String lastVersionId) {
		this.type = type;
		this.id = id;
		this.name = name;
		this.gameDirectory = gameDirectory;
		this.javaPath = javaPath;
		this.lastVersionId = lastVersionId;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
