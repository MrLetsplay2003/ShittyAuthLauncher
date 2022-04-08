package me.mrletsplay.shittyauthlauncher.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
import me.mrletsplay.mrcore.json.converter.JSONConverter;
import me.mrletsplay.shittyauthlauncher.DialogHelper;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncherSettings;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;
import me.mrletsplay.shittyauthpatcher.util.LibraryPatcher;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;
import me.mrletsplay.shittyauthpatcher.version.MinecraftVersion;

public class LaunchHelper {
	
	private static Task<Void> downloadFiles(Map<File, String> toDownload) {
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				int i = 0;
				for(Map.Entry<File, String> dl : toDownload.entrySet()) {
					if(isCancelled()) return null;
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
	
	private static Task<List<File>> loadLibraries(MinecraftVersion version, JSONObject meta, File tempFolder) throws IOException {
		return new CombinedTask<List<File>>() {
			
			@Override
			protected List<File> call() throws Exception {
				File minecraftJar = new File(ShittyAuthLauncherSettings.getGameDataPath(), "versions/" + version.getId() + "/" + version.getId() + ".jar");
				if(!minecraftJar.exists()) {
					System.out.println("Downloading " + minecraftJar + "...");
					String downloadURL = meta.getJSONObject("downloads").getJSONObject("client").getString("url");
					HttpRequest.createGet(downloadURL).execute().transferTo(minecraftJar);
				}
				
				ServerConfiguration servers = ShittyAuthLauncherSettings.getServers();
				
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
							toDownload.put(libFile, nativeLib.getString("url"));
						}
						nativeLibs.add(libFile);
					}
				}
				
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
				
				if(version.isOlderThan(MinecraftVersion.getVersion("1.7.6"))) {
					// New skins API was introduced in release 1.7.6
					File patchServers = new File(minecraftJar.getParentFile(), "patch-servers.json");
					
					boolean forcePatch = ShittyAuthLauncherSettings.isAlwaysPatchMinecraft();
					try {
						ServerConfiguration conf = JSONConverter.decodeObject(new JSONObject(Files.readString(patchServers.toPath())), ServerConfiguration.class);
						if(!conf.equals(servers)) forcePatch = true;
					}catch(IOException e) {
						forcePatch = true;
					}
					
					if(forcePatch) {
						System.out.println("Forcibly repatching Minecraft");
					}
					
					File out = new File(minecraftJar.getParentFile(), "patched-" + minecraftJar.getName());
					if(!out.exists() || forcePatch) {
						updateMessage("Patching minecraft");
						LibraryPatcher.patchMinecraft(minecraftJar.toPath(), out.toPath(), servers);
						Files.writeString(patchServers.toPath(), servers.toJSON().toString());
					}
					minecraftJar = out;
				}
				libs.add(minecraftJar);
				System.out.println("Minecraft jar: " + minecraftJar.getAbsolutePath());
				
				if(authLibFile != null) {
					File patchServers = new File(authLibFile.getParentFile(), "patch-servers.json");
					
					boolean forcePatch = ShittyAuthLauncherSettings.isAlwaysPatchMinecraft();
					try {
						ServerConfiguration conf = JSONConverter.decodeObject(new JSONObject(Files.readString(patchServers.toPath())), ServerConfiguration.class);
						if(!conf.equals(servers)) forcePatch = true;
					}catch(IOException e) {
						forcePatch = true;
					}
					
					if(forcePatch) {
						System.out.println("Forcibly repatching authlib");
					}
					
					File out = new File(authLibFile.getParentFile(), "patched-" + authLibFile.getName());
					if(!out.exists() || ShittyAuthLauncherSettings.isAlwaysPatchAuthlib()) {
						updateMessage("Patching authlib");
						LibraryPatcher.patchAuthlib(authLibFile.toPath(), out.toPath(), ShittyAuthLauncherSettings.getSkinHost(), servers);
						Files.writeString(patchServers.toPath(), servers.toJSON().toString());
					}
					libs.add(out);
					System.out.println("Using authlib at: " + out.getAbsolutePath());
				}else {
					System.out.println("Couldn't find authlib");
				}
				
				return libs;
			}
		};
	}
	
	private static Task<File> loadAssets(JSONObject meta, File assetsFolder) throws IOException {
		return new CombinedTask<File>() {
			
			@Override
			protected File call() throws Exception {
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
				
				String assetId = assetIndex.getString("id");
				File assetsDownloadFolder = new File(assetsFolder, "objects");
				boolean legacyAssets = assetId.equals("legacy");
				boolean pre16Assets = assetId.equals("pre-1.6");
				
				if(legacyAssets) assetsDownloadFolder = new File(assetsFolder, "virtual/legacy");
				if(pre16Assets) assetsDownloadFolder = new File(ShittyAuthLauncherSettings.getGameDataPath(), "resources");
				
				assetsDownloadFolder.mkdirs();
				
				Map<File, String> toDownload = new HashMap<>();
				JSONObject objects = index.getJSONObject("objects");
				for(String name : objects.keySet()) {
					JSONObject obj = objects.getJSONObject(name);
					String hash = obj.getString("hash");
					String path = hash.substring(0, 2) + "/" + hash;
					File objFile = new File(assetsDownloadFolder, (legacyAssets || pre16Assets) ? name : path);
					if(!objFile.exists()) {
						toDownload.put(objFile, "http://resources.download.minecraft.net/" + path);
					}
				}
				
				runOther(downloadFiles(toDownload));
				
				return (legacyAssets || pre16Assets) ? assetsDownloadFolder : assetsFolder;
			}
		};
	}
	
	public static void launch(MinecraftVersion version) {
		try {
			if(ShittyAuthLauncherSettings.getLoginData() == null) {
				DialogHelper.showWarning("You need to log in first");
				return;
			}
			
			ServerConfiguration servers = ShittyAuthLauncherSettings.getServers();
			
			File keyFile = new File("shittyauthlauncher/yggdrasil_session_pubkey.der");
			if(!keyFile.exists()) {
				boolean b = DialogHelper.showYesNo("You don't have a public key file yet.\nAttempt to download it from the session server?\n\nNote: Without a key file, skins won't work");
				if(b) {
					HttpGet g = HttpRequest.createGet(servers.sessionServer + "/yggdrasil_session_pubkey.der");
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
					File tempFolder = new File(ShittyAuthLauncherSettings.getGameDataPath(), UUID.randomUUID().toString());
					try {
						File metaFile = new File(ShittyAuthLauncherSettings.getGameDataPath(), "versions/" + version.getId() + "/" + version.getId() + ".json");
						JSONObject meta = version.loadMetadata(metaFile);
						
						List<File> libs = runOther(loadLibraries(version, meta, tempFolder));
						if(isCancelled()) return null;
						
						File assetsFolder = new File(ShittyAuthLauncherSettings.getGameDataPath(), "assets");
						assetsFolder = runOther(loadAssets(meta, assetsFolder));
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
						
						List<String> gameArgs = new ArrayList<>();
						if(meta.has("arguments")) {
							gameArgs.addAll(meta.getJSONObject("arguments").getJSONArray("game").stream()
									.filter(s -> s instanceof String)
									.map(s -> (String) s)
									.collect(Collectors.toList()));
						}else {
							gameArgs.addAll(Arrays.asList(meta.getString("minecraftArguments").split(" ")));
						}
						
						Map<String, String> params = new HashMap<>();
						params.put("auth_player_name", data.getUsername());
						params.put("version_name", version.getId());
						params.put("game_directory", ShittyAuthLauncherSettings.getGameDataPath());
						params.put("assets_root", assetsFolder.getAbsolutePath());
						params.put("assets_index_name", meta.getString("assets"));
						params.put("auth_uuid", data.getUuid());
						params.put("auth_access_token", data.getAccessToken());
						params.put("version_type", meta.getString("type"));
						params.put("user_type", "mojang");
						params.put("auth_session", data.getAccessToken());
						params.put("user_properties", "{}");
						params.put("game_assets", assetsFolder.getAbsolutePath() + "/");
						
						for(int i = 0; i < gameArgs.size(); i++) {
							String arg = gameArgs.get(i);
							gameArgs.set(i, params.entrySet().stream()
									.filter(e -> arg.equals("${" + e.getKey() + "}"))
									.map(e -> e.getValue())
									.findFirst().orElse(arg));
						}
						
						gameArgs.addAll(0, Arrays.asList(
								requiresOldJava ? ShittyAuthLauncherSettings.getOldJavaPath() : ShittyAuthLauncherSettings.getNewJavaPath(),
								"-Djava.library.path=" + tempFolder.getAbsolutePath(),
								"-Dminecraft.api.auth.host=" + servers.authServer,
								"-Dminecraft.api.account.host=" + servers.accountsServer,
								"-Dminecraft.api.session.host=" + servers.sessionServer,
								"-Dminecraft.api.services.host=" + servers.servicesServer,
								"-cp", classPath,
								meta.getString("mainClass")
						));
						
						ProcessBuilder b = new ProcessBuilder(gameArgs);
						b.directory(new File(ShittyAuthLauncherSettings.getGameDataPath()));
						return new Pair<>(b, tempFolder);
					}catch(Exception e) {
						if(tempFolder.exists()) tempFolder.delete();
						throw e;
					}
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
