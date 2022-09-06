package me.mrletsplay.shittyauthlauncher.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.concurrent.Task;
import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;

public class JVMVersion {

	public static final List<JVMVersion> VERSIONS = new ArrayList<>();

	static {
		JSONObject obj = HttpRequest.createGet("https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json").execute().asJSONObject();

		// TODO: Check architecture
		String osName;
		switch(OS.getCurrentOS().getType()) {
			default:
			case LINUX:
				osName = "linux";
				break;
			case MACOS:
				osName = "mac-os";
				break;
			case WINDOWS:
				osName = "windows-x64";
				break;
		}

		JSONObject jvms = obj.getJSONObject(osName);
		for(String jvmName : jvms.keySet()) {
			JSONArray arr = jvms.getJSONArray(jvmName);
			if(arr.size() == 0) continue;
			JSONObject jvm = arr.getJSONObject(0);
			String versionName = jvm.getJSONObject("version").getString("name");
			String manifestURL = jvm.getJSONObject("manifest").getString("url");
			VERSIONS.add(new JVMVersion(jvmName, versionName, manifestURL));
		}
	}

	private String name;
	private String versionName;
	private String manifestURL;

	public JVMVersion(String name, String versionName, String manifestURL) {
		this.name = name;
		this.versionName = versionName;
		this.manifestURL = manifestURL;
	}
	public static List<JVMVersion> getVersions() {
		return VERSIONS;
	}

	public String getName() {
		return name;
	}

	public String getVersionName() {
		return versionName;
	}

	public String getManifestURL() {
		return manifestURL;
	}

	public static JVMVersion getVersion(String name) {
		return VERSIONS.stream()
				.filter(v -> v.getName().equals(name))
				.findFirst().orElse(null);
	}

	public static Task<File> downloadJRE(JVMVersion version, File folder) {
		return new CombinedTask<File>() {

			@Override
			protected File call() throws Exception {
				File manifestFile = new File(folder, "manifest.json");
				if(!manifestFile.exists()) {
					try {
						HttpRequest.createGet(version.getManifestURL()).execute().transferTo(manifestFile);
					} catch (IOException e) {
						throw new LaunchException(e);
					}
				}

				JSONObject manifest;
				try {
					manifest = new JSONObject(Files.readString(manifestFile.toPath()));
				} catch (IOException e) {
					throw new LaunchException(e);
				}

				Map<File, String> filesToDownload = new HashMap<>();
				List<Path> executableFiles = new ArrayList<>();

				JSONObject files = manifest.getJSONObject("files");
				for(String name : files.keySet()) {
					JSONObject file = files.getJSONObject(name);
					if(!file.getString("type").equals("file")) continue;
					File downloadPath = new File(folder, name);
					if(downloadPath.exists()) continue;
					filesToDownload.put(downloadPath, file.getJSONObject("downloads").getJSONObject("raw").getString("url"));
					if(file.getBoolean("executable")) {
						executableFiles.add(downloadPath.toPath());
					}
				}

				runOther(LaunchHelper.downloadFiles(filesToDownload));

				if(OS.getCurrentOS().getType() != OSType.WINDOWS) {
					executableFiles.forEach(p -> {
						Set<PosixFilePermission> perms = new HashSet<>();
						perms.add(PosixFilePermission.OWNER_EXECUTE);
						perms.add(PosixFilePermission.OWNER_READ);
						perms.add(PosixFilePermission.OWNER_WRITE);
						try {
							Files.setPosixFilePermissions(p, perms);
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
				}

				return new File(folder, OS.getCurrentOS().getType().getJavaPath());
			}
		};
	}

}
