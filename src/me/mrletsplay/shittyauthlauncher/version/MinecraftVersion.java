package me.mrletsplay.shittyauthlauncher.version;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
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
import me.mrletsplay.shittyauthlauncher.util.CombinedTask;
import me.mrletsplay.shittyauthlauncher.util.LaunchException;

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
	
	private Task<Void> downloadFiles(Map<File, String> toDownload) {
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				int i = 0;
				for(Map.Entry<File, String> dl : toDownload.entrySet()) {
					if(isCancelled()) return null;
//					System.out.println("Downloading " + dl.getKey() + "...");
					updateMessage("(" + i + "/" + toDownload.size() + ") Downloading " + dl.getKey() + "...");
					try {
						HttpRequest.createGet(dl.getValue()).execute().transferTo(dl.getKey());
					} catch (IOException e) {
						throw new LaunchException(e);
					}
					updateProgress(++i, toDownload.size());
				}
				
				return null;
			}
		};
	}
	
	public JSONObject loadMetadata() throws IOException {
		File metaFile = new File(ShittyAuthLauncherSettings.getGameDataPath(), "versions/" + id + "/" + id + ".json");
		if(!metaFile.exists()) {
			System.out.println("Downloading " + metaFile + "...");
			HttpRequest.createGet(url).execute().transferTo(metaFile);
		}
		
		JSONObject meta;
		try {
			meta = new JSONObject(Files.readString(metaFile.toPath()));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return meta;
	}
	
	private Task<List<File>> loadLibraries(JSONObject meta, File tempFolder) throws IOException {
		return new CombinedTask<List<File>>() {
			
			@Override
			protected List<File> call() throws Exception {
				File minecraftJar = new File(ShittyAuthLauncherSettings.getGameDataPath(), "versions/" + id + "/" + id + ".jar");
				if(!minecraftJar.exists()) {
					System.out.println("Downloading " + minecraftJar + "...");
					String downloadURL = meta.getJSONObject("downloads").getJSONObject("client").getString("url");
					HttpRequest.createGet(downloadURL).execute().transferTo(minecraftJar);
				}
				
				File authLibFile = null;
				String os = System.getProperty("os.name").toLowerCase().contains("windows") ? "windows" : "linux";
				List<File> libs = new ArrayList<>();
				List<File> nativeLibs = new ArrayList<>();
				Map<File, String> toDownload = new HashMap<>();
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
						
						File libFile = new File(ShittyAuthLauncherSettings.getGameDataPath(), "libraries/" + path);
						
						if(!libFile.exists()) {
							toDownload.put(libFile, artifact.getString("url"));
						}
						
						if(name.startsWith("com.mojang:authlib")) {
							authLibFile = libFile;
							continue;
						}
						
						libs.add(libFile);
					}
					
					JSONObject natives = lib.optJSONObject("natives").orElse(null);
					if(natives != null) {
						if(!natives.has(os)) continue;
						JSONObject nativeLib = lib.getJSONObject("downloads").getJSONObject("classifiers").getJSONObject(natives.getString(os).replace("${arch}", "64"));
						String nativesPath = nativeLib.getString("path");
						File libFile = new File(ShittyAuthLauncherSettings.getGameDataPath(), "libraries/" + nativesPath);
						if(!libFile.exists()) {
//							System.out.println("Downloading " + libFile + "...");
//							HttpRequest.createGet(nativeLib.getString("url")).execute().transferTo(libFile);
							toDownload.put(libFile, nativeLib.getString("url"));
						}
						nativeLibs.add(libFile);
					}
				}
				
				libs.add(minecraftJar);
				
				runOther(downloadFiles(toDownload));
				if(isCancelled()) return null;
				
				updateMessage("Extracting native libs");
				for(File n : nativeLibs) {
					if(isCancelled()) return null;
					try {
						ZIPFileUtils.unzipFile(n, tempFolder);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if(authLibFile != null) {
					authLibFile = LibraryModifier.patchAuthlib(authLibFile, MinecraftVersion.this);
					libs.add(authLibFile);
					System.out.println("Using authlib at: " + authLibFile.getAbsolutePath());
				}else {
					System.out.println("Couldn't find authlib");
				}
				
				return libs;
			}
		};
	}
	
	private Task<Void> loadAssets(JSONObject meta, File assetsFolder) throws IOException {
		return new CombinedTask<Void>() {
			
			@Override
			protected Void call() throws Exception {
				File indexesFolder = new File(assetsFolder, "indexes");
				indexesFolder.mkdirs();
				
				JSONObject assetIndex = meta.getJSONObject("assetIndex");
				
				File indexFile = new File(indexesFolder, assetIndex.getString("id") + ".json");
				if(!indexFile.exists()) {
					System.out.println("Downloading " + indexFile + "...");
					updateMessage("Downloading " + indexFile + "...");
					HttpRequest.createGet(assetIndex.getString("url")).execute().transferTo(indexFile);
				}
				
				JSONObject index;
				try {
					index = new JSONObject(Files.readString(indexFile.toPath()));
				} catch (IOException e) {
					throw new LaunchException(e);
				}
				
				File objectsFolder = new File(assetsFolder, "objects");
				objectsFolder.mkdirs();
				
				Map<File, String> toDownload = new HashMap<>();
				JSONObject objects = index.getJSONObject("objects");
				for(String name : objects.keySet()) {
					JSONObject obj = objects.getJSONObject(name);
					String hash = obj.getString("hash");
					String path = hash.substring(0, 2) + "/" + hash;
					File objFile = new File(objectsFolder, path);
					if(!objFile.exists()) {
						toDownload.put(objFile, "http://resources.download.minecraft.net/" + path);
					}
				}
				
				runOther(downloadFiles(toDownload));
				return null;
			}
		};
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
						String msg = r.getErrorResponse() != null ? r.getErrorResponse() : String.valueOf(r.getException());
						if(r.getException() != null) r.getException().printStackTrace();
						DialogHelper.showError("Failed to download key file:\n" + msg);
						return;
					}
					
					r.transferTo(keyFile);
				}
			}
			
			Dialog<Void> d = new Dialog<>();
			d.setTitle("Launching game");
			d.initOwner(ShittyAuthLauncher.stage);
			d.setResizable(true);
			Label label = new Label("Loading...");
			label.setPrefWidth(500);
			ProgressBar pb = new ProgressBar(0);
			pb.setMinWidth(ProgressBar.USE_COMPUTED_SIZE);
			pb.setMinHeight(ProgressBar.USE_COMPUTED_SIZE);
			pb.setPrefWidth(500);
			GridPane content = new GridPane();
			content.setMaxWidth(Double.MAX_VALUE);
			content.setMaxHeight(Double.MAX_VALUE);
			content.add(label, 0, 0);
			content.add(pb, 0, 1);
			content.setPadding(new Insets(10, 10, 10, 10));
			content.setVgap(10);
			d.getDialogPane().setContent(content);
			d.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
			d.getDialogPane().getScene().getWindow().sizeToScene();
			d.show();
			
			Task<Pair<ProcessBuilder, File>> launch = new CombinedTask<>() {
				
				@Override
				protected Pair<ProcessBuilder, File> call() throws Exception {
					JSONObject meta = loadMetadata();
					
					File tempFolder = new File(ShittyAuthLauncherSettings.getGameDataPath(), UUID.randomUUID().toString());
					List<File> libs = runOther(loadLibraries(meta, tempFolder));
					if(isCancelled()) return null;
					
					File assetsFolder = new File(ShittyAuthLauncherSettings.getGameDataPath(), "assets");
					runOther(loadAssets(meta, assetsFolder));
					if(isCancelled()) return null;
					
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
							"--assetsDir", assetsFolder.getAbsolutePath(),
							"--assetIndex", meta.getString("assets"),
							"--userType", "mojang",
							"--versionType", "release",
							"--userProperties", "{}" /* For old versions */);
					b.directory(new File(ShittyAuthLauncherSettings.getGameDataPath()));
					return new Pair<>(b, tempFolder);
				}
			};
			launch.progressProperty().addListener(v -> pb.setProgress(launch.getProgress()));
			launch.messageProperty().addListener(v -> label.setText(launch.getMessage()));
			d.setOnCloseRequest(event -> launch.cancel());
			launch.setOnFailed(event -> {
				d.hide();
				DialogHelper.showError("Failed to launch", launch.getException());
			});
			launch.setOnSucceeded(event -> {
				d.hide();
				Pair<ProcessBuilder, File> pair;
				try {
					pair = launch.get();
				} catch (InterruptedException | ExecutionException e1) {
					e1.printStackTrace();
					return;
				}
				new Thread(() -> {
					try {
						Process p = pair.getKey().inheritIO().start();
						Platform.runLater(() -> {
							ShittyAuthLauncher.stage.setIconified(true);
						});
						p.waitFor();
						IOUtils.deleteFile(pair.getValue());
						Platform.runLater(() -> {
							ShittyAuthLauncher.stage.setIconified(false);
						});
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}).start();
			});
			new Thread(launch).start();
		}catch(Exception e) {
			DialogHelper.showError("Failed to launch", e);
		}
	}
	
}
