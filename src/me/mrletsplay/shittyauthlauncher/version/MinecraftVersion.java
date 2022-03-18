package me.mrletsplay.shittyauthlauncher.version;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javafx.application.Platform;
import me.mrletsplay.mrcore.http.HttpGet;
import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.http.HttpResult;
import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.mrcore.io.ZIPFileUtils;
import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.converter.JSONConstructor;
import me.mrletsplay.mrcore.json.converter.JSONConverter;
import me.mrletsplay.mrcore.json.converter.JSONConvertible;
import me.mrletsplay.mrcore.json.converter.JSONValue;
import me.mrletsplay.shittyauthlauncher.DialogHelper;
import me.mrletsplay.shittyauthlauncher.LibraryModifier;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncherSettings;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;

public class MinecraftVersion implements JSONConvertible {
	
	public static final List<MinecraftVersion> VERSIONS = new ArrayList<>();
	public static final MinecraftVersion LATEST_RELEASE, LATEST_SNAPSHOT;
	
	static {
		JSONObject obj = HttpRequest.createGet("http://launchermeta.mojang.com/mc/game/version_manifest.json").execute().asJSONObject();
		
		for(Object o : obj.getJSONArray("versions")) {
			VERSIONS.add(JSONConverter.decodeObject((JSONObject) o, MinecraftVersion.class));
		}
		
		JSONObject latest = obj.getJSONObject("latest");
		
		LATEST_RELEASE = VERSIONS.stream()
				.filter(v -> v.getId().equals(latest.getString("release")))
				.findFirst().orElse(VERSIONS.get(0));
		LATEST_SNAPSHOT = VERSIONS.stream()
				.filter(v -> v.getId().equals(latest.getString("snapshot")))
				.findFirst().orElse(VERSIONS.get(0));
	}

	@JSONValue
	private String id;
	
	@JSONValue
	private MinecraftVersionType type;
	
	@JSONValue
	private String url;
	
	@JSONConstructor
	private MinecraftVersion() {}
	
	public String getId() {
		return id;
	}

	public MinecraftVersionType getType() {
		return type;
	}

	public String getURL() {
		return url;
	}

	@Override
	public String toString() {
		return id;
	}
	
	public void launch() {
		try {
			if(ShittyAuthLauncherSettings.getLoginData() == null) {
				DialogHelper.showWarning("You need to log in first");
				return;
			}
			
			File keyFile = new File("shittyauthlauncher/yggdrasil_session_pubkey.der");
			if(!keyFile.exists()) {
				boolean b = DialogHelper.showYesNo("You don't have a public key file yet.\nAttempt to download it from the session server?\n\nNote: Without a key file, skins won't work");
				if(b) {
					HttpGet g = HttpRequest.createGet(ShittyAuthLauncherSettings.getSessionServerURL() + "/yggdrasil_session_pubkey.der");
					HttpResult r = g.execute();
					if(!r.isSuccess()) {
						DialogHelper.showError("Failed to download key file:\n" + r.getErrorResponse());
						return;
					}
					
					r.transferTo(keyFile);
				}
			}
			
			File metaFile = new File(ShittyAuthLauncherSettings.getMinecraftPath(), "versions/" + id + "/" + id + ".json");
			if(!metaFile.exists()) {
				DialogHelper.showWarning("You need to start MC " + id + " at least once through the official launcher");
				return;
			}
			
			JSONObject meta;
			try {
				meta = new JSONObject(Files.readString(metaFile.toPath()));
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			File minecraftJar = new File(ShittyAuthLauncherSettings.getMinecraftPath(), "versions/" + id + "/" + id + ".jar");
			if(!minecraftJar.exists()) {
				DialogHelper.showWarning("You need to start MC " + id + " at least once through the official launcher");
				return;
			}
			
			File authLibFile = null;
			String os = System.getProperty("os.name").toLowerCase().contains("windows") ? "windows" : "linux";
			List<File> libs = new ArrayList<>();
			List<File> nativeLibs = new ArrayList<>();
			for(Object o : meta.getJSONArray("libraries")) {
				JSONObject lib = (JSONObject) o;
				String name = lib.getString("name");
				
				JSONArray rules = lib.optJSONArray("rules").orElse(null);
				Boolean allow = null;
				if(rules != null) {
					for(Object r : rules) {
						JSONObject rule = (JSONObject) r;
						String ruleOS = rule.optJSONObject("os").map(obj -> obj.getString("name")).orElse(null);
						boolean a = rule.getString("action").equals("allow");
						if(ruleOS == null) {
							if(allow == null) allow = a;
						}else if(ruleOS.equals(os)) {
							allow = a;
						}
					}
				}else {
					// No rules = allow
					allow = true;
				}
				
				if(allow == null || !allow) continue;
				
				JSONObject downloads = lib.getJSONObject("downloads");
				
				if(downloads.has("artifact")) {
					JSONObject artifact = downloads.getJSONObject("artifact");
					String path = artifact.getString("path");
					
					if(name.startsWith("com.mojang:authlib")) {
						try {
							authLibFile = LibraryModifier.patchAuthlib(new File(ShittyAuthLauncherSettings.getMinecraftPath(), "libraries/" + path), this);
						} catch (IOException e) {
							e.printStackTrace();
						}
						continue;
					}
					libs.add(new File(ShittyAuthLauncherSettings.getMinecraftPath(), "libraries/" + path));
				}
				
				JSONObject natives = lib.optJSONObject("natives").orElse(null);
				if(natives != null) {
					if(!natives.has(os)) continue;
					String nativesPath = lib.getJSONObject("downloads").getJSONObject("classifiers").getJSONObject(natives.getString(os).replace("${arch}", "64")).getString("path");
					nativeLibs.add(new File(ShittyAuthLauncherSettings.getMinecraftPath(), "libraries/" + nativesPath));
				}
			}
			
			libs.add(minecraftJar);
			if(authLibFile != null) libs.add(authLibFile);
			
			File tempFolder = new File(ShittyAuthLauncherSettings.getGameDataPath(), UUID.randomUUID().toString());
			for(File n : nativeLibs) {
				try {
					ZIPFileUtils.unzipFile(n, tempFolder);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(authLibFile != null) {
				System.out.println("Using authlib at: " + authLibFile.getAbsolutePath());
			}else {
				System.out.println("Couldn't find authlib");
			}
			
			LoginData data = ShittyAuthLauncherSettings.getLoginData();
			String libSeparator = System.getProperty("os.name").toLowerCase().contains("windows") ? ";" : ":";
			String classPath = libs.stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(libSeparator));
	
			boolean requiresOldJava = true;
			if(meta.containsKey("javaVersion")) {
				int major = meta.getJSONObject("javaVersion").getInt("majorVersion");
				if(major > 8) requiresOldJava = false;
			}
			System.out.println("Requires old Java? " + requiresOldJava);
			
			ProcessBuilder b = new ProcessBuilder(
					requiresOldJava ? ShittyAuthLauncherSettings.getOldJavaPath() : ShittyAuthLauncherSettings.getNewJavaPath(),
					"-Djava.library.path=" + tempFolder.getAbsolutePath(),
					"-Dminecraft.api.auth.host=" + ShittyAuthLauncherSettings.getAuthServerURL(),
					"-Dminecraft.api.account.host=" + ShittyAuthLauncherSettings.getAccountServerURL(),
					"-Dminecraft.api.session.host=" + ShittyAuthLauncherSettings.getSessionServerURL(),
					"-Dminecraft.api.services.host=" + ShittyAuthLauncherSettings.getServicesServerURL(),
					"-cp", classPath,
					meta.getString("mainClass"),
					"--version", id,
					"--accessToken", data.getAccessToken(),
					"--username", data.getUsername(),
					"--uuid", data.getUuid(),
					"--gameDir", ShittyAuthLauncherSettings.getGameDataPath(),
					"--assetsDir", ShittyAuthLauncherSettings.getMinecraftPath() + "/assets",
					"--assetIndex", meta.getString("assets"),
					"--userType", "mojang",
					"--versionType", "release",
					"--userProperties", "{}" /* For old versions */);
			b.directory(new File(ShittyAuthLauncherSettings.getMinecraftPath()));
			new Thread(() -> {
				try {
					Process p = b.inheritIO().start();
					Platform.runLater(() -> {
						ShittyAuthLauncher.stage.setIconified(true);
					});
					p.waitFor();
					IOUtils.deleteFile(tempFolder);
					Platform.runLater(() -> {
						ShittyAuthLauncher.stage.setIconified(false);
					});
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}).start();
		}catch(Exception e) {
			DialogHelper.showError("Failed to launch", e);
		}
	}
	
}
