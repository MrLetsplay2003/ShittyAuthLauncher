package me.mrletsplay.shittyauthlauncher.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import me.mrletsplay.mrcore.json.converter.JSONConstructor;
import me.mrletsplay.mrcore.json.converter.JSONConvertible;
import me.mrletsplay.mrcore.json.converter.JSONValue;
import me.mrletsplay.mrcore.misc.FriendlyException;

public class GameInstallation implements JSONConvertible {
	
	public static final String DEFAULT_IMAGE_DATA;
	
	static {
		try(InputStream in = GameInstallation.class.getResourceAsStream("/include/icon.png")) {
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
	public String lastVersionId;
	
	@JSONConstructor
	public GameInstallation() {}
	
	public GameInstallation(InstallationType type, String id, String name, String imageData, String gameDirectory, String javaPath, String lastVersionId) {
		this.type = type;
		this.id = id;
		this.name = name;
		this.imageData = imageData;
		this.gameDirectory = gameDirectory;
		this.javaPath = javaPath;
		this.lastVersionId = lastVersionId;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
