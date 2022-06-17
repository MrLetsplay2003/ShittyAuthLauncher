package me.mrletsplay.shittyauthlauncher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.config.ConfigLoader;
import me.mrletsplay.mrcore.config.FileCustomConfig;
import me.mrletsplay.mrcore.config.mapper.JSONObjectMapper;
import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.converter.DeserializationOption;
import me.mrletsplay.mrcore.json.converter.JSONConverter;
import me.mrletsplay.mrcore.json.converter.SerializationOption;
import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.shittyauthlauncher.auth.MinecraftAccount;
import me.mrletsplay.shittyauthlauncher.util.GameInstallation;
import me.mrletsplay.shittyauthlauncher.util.InstallationType;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public class ShittyAuthLauncherSettings {

	public static final String DATA_PATH = "shittyauthlauncher";
	public static final String LAUNCHER_BRAND = "ShittyAuthLauncher";
	public static final String LAUNCHER_VERSION = "69.420";

	public static final String
		DEFAULT_MINECRAFT_CONTAINER = System.getProperty("os.name").toLowerCase().contains("windows") ? System.getenv("APPDATA") : System.getProperty("user.home"),
		DEFAULT_MINECRAFT_PATH = DEFAULT_MINECRAFT_CONTAINER + "/.minecraft",
		DEFAULT_GAME_DATA_PATH = DEFAULT_MINECRAFT_CONTAINER + "/.minecraft-shitty";

	private static final Path
		MIRRORS_PATH = new File(DATA_PATH + "/mirrors.json").toPath();

	private static FileCustomConfig config;
	private static FileCustomConfig tokenConfig;

	private static List<GameInstallation> installations;
	private static List<DownloadsMirror> mirrors;

	static {
		config = ConfigLoader.loadFileConfig(new File(ShittyAuthLauncherSettings.DATA_PATH + "/settings.yml"));
		config.registerMapper(JSONObjectMapper.create(ServerConfiguration.class, EnumSet.of(SerializationOption.DONT_INCLUDE_CLASS), EnumSet.noneOf(DeserializationOption.class)));
		config.registerMapper(JSONObjectMapper.create(GameInstallation.class, EnumSet.of(SerializationOption.SHORT_ENUMS, SerializationOption.DONT_INCLUDE_CLASS), EnumSet.of(DeserializationOption.SHORT_ENUMS)));

		tokenConfig = ConfigLoader.loadFileConfig(new File(ShittyAuthLauncherSettings.DATA_PATH + "/token.yml"));
		tokenConfig.registerMapper(JSONObjectMapper.create(MinecraftAccount.class, EnumSet.of(SerializationOption.DONT_INCLUDE_CLASS), EnumSet.noneOf(DeserializationOption.class)));

		if(config.isEmpty()) {
			setUseAdoptium(true);
			setAlwaysPatchAuthlib(false);
			setAlwaysPatchMinecraft(false);
			setMinimizeLauncher(true);
			setInstallations(Collections.emptyList());
			setAccounts(Collections.emptyList());
			setActiveAccount(null);
			save();
		}

		installations = getInstallations();
		if(!installations.stream().anyMatch(i -> i.type == InstallationType.LATEST_RELEASE)) {
			installations.add(new GameInstallation(InstallationType.LATEST_RELEASE, "latest-release", "Latest Release", null, DEFAULT_GAME_DATA_PATH, null, null, null));
			setInstallations(installations);
		}

		if(!installations.stream().anyMatch(i -> i.type == InstallationType.LATEST_SNAPSHOT)) {
			installations.add(new GameInstallation(InstallationType.LATEST_SNAPSHOT, "latest-snapshot", "Latest Snapshot", null, DEFAULT_GAME_DATA_PATH, null, null, null));
			setInstallations(installations);
		}

		mirrors = getMirrors();
		mirrors.add(0, DownloadsMirror.MOJANG);
	}

	public static void save() {
		config.saveToFile();
	}

	public static void setUseAdoptium(boolean alwaysPatchAuthlib) {
		config.set("use-adoptium", alwaysPatchAuthlib);
	}

	public static boolean isUseAdoptium() {
		return config.getBoolean("use-adoptium");
	}

	public static void setAlwaysPatchAuthlib(boolean alwaysPatchAuthlib) {
		config.set("always-patch-authlib", alwaysPatchAuthlib);
	}

	public static boolean isAlwaysPatchAuthlib() {
		return config.getBoolean("always-patch-authlib");
	}

	public static void setAlwaysPatchMinecraft(boolean alwaysPatchMinecraft) {
		config.set("always-patch-minecraft", alwaysPatchMinecraft);
	}

	public static boolean isAlwaysPatchMinecraft() {
		return config.getBoolean("always-patch-minecraft");
	}

	public static void setMinimizeLauncher(boolean minimizeLauncher) {
		config.set("minimize-launcher", minimizeLauncher);
	}

	public static boolean isMinimizeLauncher() {
		return config.getBoolean("minimize-launcher");
	}

	public static void setInstallations(List<GameInstallation> installations) {
		ShittyAuthLauncherSettings.installations = new ArrayList<>(installations);
		config.set("installations", new ArrayList<>(installations));
	}

	public static List<GameInstallation> getInstallations() {
		if(installations != null) return installations;
		return installations = config.getGenericList("installations", GameInstallation.class, new ArrayList<>(), false);
	}

	public static void setActiveInstallation(GameInstallation installation) {
		config.set("active-installation", installation == null ? null : installation.id);
	}

	public static GameInstallation getActiveInstallation() {
		String inst = config.getString("active-installation");
		if(inst == null) return null;
		return getInstallations().stream()
				.filter(i -> i.id.equals(inst))
				.findFirst().orElse(null);
	}

	public static void setAccounts(List<MinecraftAccount> accounts) {
		tokenConfig.set("accounts", new ArrayList<>(accounts));
		tokenConfig.saveToFile();
	}

	public static List<MinecraftAccount> getAccounts() {
		return tokenConfig.getGenericList("accounts", MinecraftAccount.class, new ArrayList<>(), false);
	}

	public static void setActiveAccount(MinecraftAccount account) {
		tokenConfig.set("active-account", account == null ? null : account.getId());
		tokenConfig.saveToFile();
	}

	public static MinecraftAccount getActiveAccount() {
		String acc = tokenConfig.getString("active-account");
		if(acc == null) return null;
		return getAccounts().stream()
				.filter(a -> a.getId().equals(acc))
				.findFirst().orElse(null);
	}

	private static List<DownloadsMirror> loadMirrors() {
		try {
			JSONArray p = new JSONArray(Files.readString(MIRRORS_PATH, StandardCharsets.UTF_8));
			return p.stream()
				.map(m -> JSONConverter.decodeObject((JSONObject) m, DownloadsMirror.class))
				.collect(Collectors.toList());
		} catch (IOException e) {
			throw new FriendlyException("Failed to read file", e);
		}
	}

	public static List<DownloadsMirror> getMirrors() {
		if(mirrors != null) return mirrors;
		return mirrors = loadMirrors();
	}

	public static DownloadsMirror getMirror(String name) {
		return getMirrors().stream()
			.filter(m -> m.getName().equals(name))
			.findFirst().orElse(null);
	}

	public static void setMirrors(List<DownloadsMirror> mirrors) {
		try {
			ShittyAuthLauncherSettings.mirrors = new ArrayList<>(mirrors);
			JSONArray mirrorsArray = new JSONArray();
			for(DownloadsMirror m : getMirrors()) {
				if(m == DownloadsMirror.MOJANG) continue;
				mirrorsArray.add(m.toJSON(SerializationOption.DONT_INCLUDE_CLASS));
			}
			Files.writeString(MIRRORS_PATH, mirrorsArray.toFancyString());
		} catch (IOException e) {
			System.err.println("Failed to write mirrors file");
			e.printStackTrace();
		}
	}

}
