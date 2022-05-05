package me.mrletsplay.shittyauthlauncher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import me.mrletsplay.mrcore.config.ConfigLoader;
import me.mrletsplay.mrcore.config.FileCustomConfig;
import me.mrletsplay.mrcore.config.mapper.JSONObjectMapper;
import me.mrletsplay.mrcore.json.converter.DeserializationOption;
import me.mrletsplay.mrcore.json.converter.SerializationOption;
import me.mrletsplay.shittyauthlauncher.auth.MinecraftAccount;
import me.mrletsplay.shittyauthlauncher.util.GameInstallation;
import me.mrletsplay.shittyauthlauncher.util.InstallationType;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public class ShittyAuthLauncherSettings {
	
	public static final String
		DEFAULT_MINECRAFT_CONTAINER = System.getProperty("os.name").toLowerCase().contains("windows") ? System.getenv("APPDATA") : System.getProperty("user.home"),
		DEFAULT_MINECRAFT_PATH = DEFAULT_MINECRAFT_CONTAINER + "/.minecraft",		
		DEFAULT_GAME_DATA_PATH = DEFAULT_MINECRAFT_CONTAINER + "/.minecraft-shitty";
	
	private static FileCustomConfig config;
	private static FileCustomConfig tokenConfig;

	public static String dataPath = "shittyauthlauncher";
	public static String launcherVersion = "ShittyAuthLauncher";
	public static String launcherBrand = "69.420";
	
	static {
		config = ConfigLoader.loadFileConfig(new File(ShittyAuthLauncherSettings.dataPath+"/settings.yml"));
		config.registerMapper(JSONObjectMapper.create(ServerConfiguration.class, EnumSet.of(SerializationOption.DONT_INCLUDE_CLASS), EnumSet.noneOf(DeserializationOption.class)));
		config.registerMapper(JSONObjectMapper.create(GameInstallation.class, EnumSet.of(SerializationOption.SHORT_ENUMS, SerializationOption.DONT_INCLUDE_CLASS), EnumSet.of(DeserializationOption.SHORT_ENUMS)));
		
		tokenConfig = ConfigLoader.loadFileConfig(new File(ShittyAuthLauncherSettings.dataPath+"/token.yml"));
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
		
		List<GameInstallation> installations = getInstallations();
		if(!installations.stream().anyMatch(i -> i.type == InstallationType.LATEST_RELEASE)) {
			installations.add(new GameInstallation(InstallationType.LATEST_RELEASE, "latest-release", "Latest Release", null, DEFAULT_GAME_DATA_PATH, null, null, null));
			setInstallations(installations);
		}
		
		if(!installations.stream().anyMatch(i -> i.type == InstallationType.LATEST_SNAPSHOT)) {
			installations.add(new GameInstallation(InstallationType.LATEST_SNAPSHOT, "latest-snapshot", "Latest Snapshot", null, DEFAULT_GAME_DATA_PATH, null, null, null));
			setInstallations(installations);
		}
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
		config.set("installations", new ArrayList<>(installations));
	}
	
	public static List<GameInstallation> getInstallations() {
		return config.getGenericList("installations", GameInstallation.class, new ArrayList<>(), false);
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

}
