package me.mrletsplay.shittyauthlauncher.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncherSettings;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;
import me.mrletsplay.shittyauthlauncher.auth.MinecraftAccount;
import me.mrletsplay.shittyauthlauncher.util.dialog.DialogHelper;
import me.mrletsplay.shittyauthpatcher.util.LibraryPatcher;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;
import me.mrletsplay.shittyauthpatcher.version.MinecraftVersion;
import me.mrletsplay.shittyauthpatcher.version.meta.AssetIndex;
import me.mrletsplay.shittyauthpatcher.version.meta.DownloadableFile;
import me.mrletsplay.shittyauthpatcher.version.meta.JavaVersion;
import me.mrletsplay.shittyauthpatcher.version.meta.Library;
import me.mrletsplay.shittyauthpatcher.version.meta.VersionMetadata;

public class LaunchHelper {
	
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(?<name>[a-z_]+)\\}");
	
	public static Task<Void> downloadFiles(Map<File, String> toDownload) {
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
	
	private static boolean matchesRuleOS(OS os, JSONObject ruleOS) {
		return (!ruleOS.has("name") || ruleOS.getString("name").equals(os.getType().getRuleName()))
				&& (!ruleOS.has("version") || Pattern.compile(ruleOS.getString("version")).matcher(os.getVersion()).matches())
				&& (!ruleOS.has("arch") || ruleOS.getString("arch").equals(os.getArch()));
	}
	
	private static boolean checkRules(JSONArray rules) {
		OS os = OS.getCurrentOS();
		Boolean allow = null;
		for(Object r : rules) {
			JSONObject rule = (JSONObject) r;
			boolean a = rule.getString("action").equals("allow");
			
			if(!rule.has("os")) {
				if(rule.has("features")) continue; // We don't care about features right now (e.g. is_demo_user, has_custom_resolution)
				if(allow == null) allow = a;
				continue;
			}
			
			JSONObject ruleOS = rule.getJSONObject("os");
			if(matchesRuleOS(os, ruleOS)) allow = a;
		}
		
		return allow != null && allow;
	}
	
	private static Task<List<File>> loadLibraries(MinecraftVersion version, VersionMetadata meta, File tempFolder, MinecraftAccount account, GameInstallation installation) throws IOException {
		return new CombinedTask<List<File>>() {
			
			@Override
			protected List<File> call() throws Exception {
				File minecraftJar = new File(installation.gameDirectory, "versions/" + version.getId() + "/" + version.getId() + ".jar");
				if(!minecraftJar.exists() || minecraftJar.length() == 0) {
					System.out.println("Downloading " + minecraftJar + "...");
					String downloadURL = meta.getClientDownloadURL();
					HttpRequest.createGet(downloadURL).execute().transferTo(minecraftJar);
				}
				
				ServerConfiguration servers = account.getServers();
				
				File authLibFile = null;
				
				String os = OS.getCurrentOS().getType().getRuleName();
				
				List<File> libs = new ArrayList<>();
				List<File> nativeLibs = new ArrayList<>();
				Map<File, String> toDownload = new HashMap<>();
				for(Library lib : meta.getLibraries()) {
					String name = lib.getName();
					String path = lib.getGeneratedPath();
					File libFile = new File(installation.gameDirectory, "libraries/" + path);
					
					JSONArray rules = lib.getRules();
					if(rules != null && !checkRules(rules)) continue;
					
					DownloadableFile artifact = lib.getArtifactDownload();
					if(artifact != null) {
						if(!libFile.exists() || libFile.length() == 0) {
							toDownload.put(libFile, artifact.getURL());
						}
					}
					
					DownloadableFile natives = lib.getNativesDownload(os);
					if(natives != null) {
						File nativesFile = new File(installation.gameDirectory, "libraries/" + natives.getPath());
						if(!nativesFile.exists()) {
							toDownload.put(nativesFile, natives.getURL());
						}
						if(!nativeLibs.contains(nativesFile)) nativeLibs.add(nativesFile);
					}
					
					if(name.startsWith("com.mojang:authlib")) {
						authLibFile = libFile;
					}else {
						if(!libs.contains(libFile)) libs.add(libFile);
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
					if(!out.exists() || forcePatch) {
						updateMessage("Patching authlib");
						LibraryPatcher.patchAuthlib(authLibFile.toPath(), out.toPath(), servers);
						Files.writeString(patchServers.toPath(), servers.toJSON().toString());
					}
					libs.add(out);
					System.out.println("Using authlib at: " + out.getAbsolutePath());
				}else {
					System.out.println("Couldn't find authlib");
				}
				
				System.out.println("Libraries: " + libs);
				
				return libs;
			}
		};
	}
	
	private static Task<File> loadAssets(VersionMetadata meta, File assetsFolder, GameInstallation installation) throws IOException {
		return new CombinedTask<File>() {
			
			@Override
			protected File call() throws Exception {
				File indexesFolder = new File(assetsFolder, "indexes");
				indexesFolder.mkdirs();
				
				AssetIndex assetIndex = meta.getAssetIndex();

				String assetId = assetIndex.getID();
				File indexFile = new File(indexesFolder, assetId + ".json");
				if(!indexFile.exists()) {
					System.out.println("Downloading " + indexFile + "...");
					updateMessage("Downloading " + indexFile + "...");
					HttpRequest.createGet(assetIndex.getURL()).execute().transferTo(indexFile);
				}
				
				JSONObject index;
				try {
					index = new JSONObject(Files.readString(indexFile.toPath()));
				} catch (IOException e) {
					throw new LaunchException(e);
				}
				
				File assetsDownloadFolder = new File(assetsFolder, "objects");
				boolean legacyAssets = assetId.equals("legacy");
				boolean pre16Assets = assetId.equals("pre-1.6");
				
				if(legacyAssets) assetsDownloadFolder = new File(assetsFolder, "virtual/legacy");
				if(pre16Assets) assetsDownloadFolder = new File(installation.gameDirectory, "resources");
				
				assetsDownloadFolder.mkdirs();
				
				Map<File, String> toDownload = new HashMap<>();
				JSONObject objects = index.getJSONObject("objects");
				for(String name : objects.keySet()) {
					JSONObject obj = objects.getJSONObject(name);
					String hash = obj.getString("hash");
					String path = hash.substring(0, 2) + "/" + hash;
					File objFile = new File(assetsDownloadFolder, (legacyAssets || pre16Assets) ? name : path);
					if(!objFile.exists()) {
						toDownload.put(objFile, ShittyAuthLauncher.mirror.assets_url + path);
					}
				}
				
				runOther(downloadFiles(toDownload));
				
				return (legacyAssets || pre16Assets) ? assetsDownloadFolder : assetsFolder;
			}
		};
	}
	
	public static void launch(MinecraftVersion version, MinecraftAccount account, GameInstallation installation) {
		// TODO: check for already running Minecraft instance
		try {
			ServerConfiguration servers = account.getServers();
			
			File keyFile = new File("shittyauthlauncher/yggdrasil_session_pubkey.der");
			if(!keyFile.exists()) {
				boolean b = DialogHelper.showYesNo("You don't have a public key file yet.\nAttempt to download it from the session server? (only available if using ShittyAuthServer)\n\nNote: Without a key file, skins won't work");
				if(b) {
					HttpGet g = HttpRequest.createGet(servers.sessionServer + "/yggdrasil_session_pubkey.der");
					HttpResult r = g.execute();
					if(!r.isSuccess()) {
						DialogHelper.showError("Failed to download key file" + (r.getErrorResponse() != null ? ":\n" + r.getErrorResponse() : ""), r.getException());
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
					File tempFolder = new File(installation.gameDirectory, UUID.randomUUID().toString());
					try {
						File metaFile = new File(installation.gameDirectory, "versions/" + version.getId() + "/" + version.getId() + ".json");
						
						VersionMetadata meta;
						if(!version.isImported()) {
							meta = version.loadMetadata(metaFile);
						}else {
							ImportedVersion imported = ShittyAuthLauncher.controller.getImportedVersion(installation.id, version.getId());
							if(imported == null) {
								throw new VersionNotFoundException("The version '" + installation.lastVersionId + "' is not currently installed in the selected installation");
							}
							meta = imported.loadMetadata();
						}
						
						List<File> libs = runOther(loadLibraries(version, meta, tempFolder, account, installation));
						if(isCancelled()) {
							IOUtils.deleteFile(tempFolder);
							return null;
						}
						
						File assetsFolder = new File(installation.gameDirectory, "assets");
						assetsFolder = runOther(loadAssets(meta, assetsFolder, installation));
						if(isCancelled()) {
							IOUtils.deleteFile(tempFolder);
							return null;
						}
						
						String javaPath = installation.javaPath;
						if(javaPath == null) {
							JavaVersion javaVersion = meta.getJavaVersion();
							String jvmName = javaVersion.getComponent();
							int majorVersion = javaVersion.getMajorVersion();
							
							File javaExecutable;
							if(ShittyAuthLauncherSettings.isUseAdoptium()) {
								File jvmFolder = new File(installation.gameDirectory, "runtime/adoptium-" + jvmName);
								javaExecutable = runOther(AdoptiumAPI.downloadJRE(majorVersion, jvmFolder));
							}else {
								File jvmFolder = new File(installation.gameDirectory, "runtime/" + jvmName);
								javaExecutable = runOther(JVMVersion.downloadJRE(JVMVersion.getVersion(jvmName), jvmFolder));
							}
							
							if(isCancelled()) {
								IOUtils.deleteFile(tempFolder);
								return null;
							}
							
							javaPath = javaExecutable.getAbsolutePath().toString().replace("\\", "/");
						}
						
						String libSeparator = System.getProperty("os.name").toLowerCase().contains("windows") ? ";" : ":";
						String classPath = libs.stream().map(f -> f.getAbsolutePath().replace("\\", "/")).collect(Collectors.joining(libSeparator));
				
						List<String> gameArgs = new ArrayList<>();
						List<String> jvmArgs = new ArrayList<>();
						
						meta.getGameArguments().forEach(a -> {
							if(a.getRules() != null && !checkRules(a.getRules())) return;
							gameArgs.addAll(a.getValue());
						});
						
						meta.getJVMArguments().forEach(a -> {
							if(a.getRules() != null && !checkRules(a.getRules())) return;
							jvmArgs.addAll(a.getValue());
						});
						
						if(installation.jvmArgs != null) jvmArgs.addAll(installation.jvmArgs);
						
						LoginData data = account.getLoginData();
						
						Map<String, String> params = new HashMap<>();
						params.put("auth_player_name", data.getUsername());
						params.put("version_name", version.getId());
						params.put("game_directory", installation.gameDirectory.replace("\\", "/"));
						params.put("assets_root", assetsFolder.getAbsolutePath().replace("\\", "/"));
						params.put("assets_index_name", meta.getAssets());
						params.put("auth_uuid", data.getUUID());
						params.put("auth_access_token", data.getAccessToken());
						params.put("version_type", meta.getType());
						params.put("user_type", "mojang");
						params.put("auth_session", data.getAccessToken());
						params.put("user_properties", "{}");
						params.put("game_assets", assetsFolder.getAbsolutePath().replace("\\", "/") + "/");
						params.put("natives_directory", tempFolder.getAbsolutePath().replace("\\", "/"));
						params.put("launcher_name", "ShittyAuthLauncher");
						params.put("launcher_version", "69.420");
						params.put("classpath", classPath);
						params.put("auth_xuid", "nope"); // XBox UID
						params.put("clientid", "nope");
						params.put("library_directory", new File(installation.gameDirectory, "libraries").getAbsolutePath());
						params.put("classpath_separator", libSeparator);
						
						System.out.println("Java path: " + javaPath);
						
						if(OS.getCurrentOS().getType() == OSType.MACOS && !jvmArgs.contains("-XstartOnFirstThread")) {
							gameArgs.add(0, "-XstartOnFirstThread");
						}
						
						if(meta.usesLegacyArgs()) {
							jvmArgs.addAll(Arrays.asList(
									"-Djava.library.path=" + tempFolder.getAbsolutePath().replace("\\", "/"),
									"-cp", classPath
							));
						}
						
						jvmArgs.add("-Dfml.ignoreInvalidMinecraftCertificates=true"); // Forge complains if we patch the Minecraft
						
						System.out.println("JVM args: " + jvmArgs);
						System.out.println("Game args: " + gameArgs);
						
						replacePlaceholders(gameArgs, params);
						replacePlaceholders(jvmArgs, params);
						
						List<String> fullArgs = new ArrayList<>();
						fullArgs.add(javaPath);
						fullArgs.addAll(jvmArgs);
						fullArgs.add(meta.getMainClass());
						fullArgs.addAll(gameArgs);
						
						System.out.println(fullArgs);
						
						ProcessBuilder b = new ProcessBuilder(fullArgs);
						b.directory(new File(installation.gameDirectory));
						return new Pair<>(b, tempFolder);
					}catch(Exception e) {
						IOUtils.deleteFile(tempFolder);
						throw e;
					}
				}
			};
			launch.progressProperty().addListener(v -> pb.setProgress(launch.getProgress()));
			launch.messageProperty().addListener(v -> label.setText(launch.getMessage()));
			d.setOnCloseRequest(event -> launch.cancel());
			launch.setOnFailed(event -> {
				d.hide();
				
				Throwable e = launch.getException();
				if(e instanceof VersionNotFoundException) {
					DialogHelper.showError(e.getMessage());
				}else {
					DialogHelper.showError("Failed to launch", launch.getException());
				}
			});
			launch.setOnSucceeded(event -> {
				d.hide();
				Pair<ProcessBuilder, File> pair;
				try {
					pair = launch.get();
					if(pair == null) return;
				} catch (InterruptedException | ExecutionException e1) {
					e1.printStackTrace();
					return;
				}
				new Thread(() -> {
					try {
						ShittyAuthLauncher.controller.clearLog();
						Process p = pair.getKey().start();
						if(ShittyAuthLauncherSettings.isMinimizeLauncher()) {
							Platform.runLater(() -> {
								ShittyAuthLauncher.stage.setIconified(true);
							});
						}
						while(p.isAlive()) {
							writeToLog(p.getInputStream());
							writeToLog(p.getErrorStream());
							Thread.sleep(100);
						}
						writeToLog(p.getInputStream());
						writeToLog(p.getErrorStream());
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
	
	private static void replacePlaceholders(List<String> args, Map<String, String> placeholders) {
		for(int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			Matcher m = PLACEHOLDER_PATTERN.matcher(arg);
			StringBuilder sb = new StringBuilder();
			while(m.find()) {
				String val = placeholders.get(m.group("name"));
				if(val == null) {
					System.out.println("Missing parameter: " + val + ", skipping!");
				}else {
					m.appendReplacement(sb, val);
				}
			}
			m.appendTail(sb);
			args.set(i, sb.toString());
		}
	}
	
	private static void writeToLog(InputStream in) throws IOException {
		byte[] bytes = new byte[in.available()];
		int len = in.read(bytes);
		ShittyAuthLauncher.controller.appendLog(new String(bytes, 0, len, StandardCharsets.UTF_8));
	}

}
