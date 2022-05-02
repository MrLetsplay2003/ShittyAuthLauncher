package me.mrletsplay.shittyauthlauncher.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthpatcher.version.meta.MetadataLoadException;
import me.mrletsplay.shittyauthpatcher.version.meta.VersionMetadata;

public class ImportedVersion {
	
	private String id;
	private File metaFile;

	public ImportedVersion(File metaFile) {
		this.metaFile = metaFile;
		this.id = loadMetadata().getId();
	}
	
	public String getId() {
		return id;
	}
	
	public File getMetaFile() {
		return metaFile;
	}
	
	public VersionMetadata loadMetadata() {
		JSONObject meta;
		try {
			meta = new JSONObject(Files.readString(metaFile.toPath()));
		} catch (IOException e) {
			throw new MetadataLoadException(e);
		}
		
		return new VersionMetadata(meta);
	}

}
