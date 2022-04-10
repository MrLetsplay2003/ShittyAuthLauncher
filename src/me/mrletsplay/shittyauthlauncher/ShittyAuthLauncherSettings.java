package me.mrletsplay.shittyauthlauncher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.config.ConfigLoader;
import me.mrletsplay.mrcore.config.FileCustomConfig;
import me.mrletsplay.mrcore.config.mapper.JSONObjectMapper;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;
import me.mrletsplay.shittyauthlauncher.util.GameInstallation;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public class ShittyAuthLauncherSettings {
	
	private static final String
		DEFAULT_MINECRAFT_CONTAINER = System.getProperty("os.name").toLowerCase().contains("windows") ? System.getenv("APPDATA") : System.getProperty("user.home"),
		DEFAULT_MINECRAFT_PATH = DEFAULT_MINECRAFT_CONTAINER + "/.minecraft",
		DEFAULT_GAME_DATA_PATH = DEFAULT_MINECRAFT_CONTAINER + "/.minecraft-shitty",
		DEFAULT_SERVER_URL = "https://mc.graphite-official.com",
		DEFAULT_SKIN_HOST = "mc.graphite-official.com";
	
	private static final ServerConfiguration DEFAULT_SERVERS = new ServerConfiguration(
			DEFAULT_SERVER_URL,
			DEFAULT_SERVER_URL,
			DEFAULT_SERVER_URL,
			DEFAULT_SERVER_URL
		);
	
	private static FileCustomConfig config;
	private static FileCustomConfig tokenConfig;
	
	static {
		config = ConfigLoader.loadFileConfig(new File("shittyauthlauncher/settings.yml"));
		config.registerMapper(JSONObjectMapper.create(ServerConfiguration.class));
		config.registerMapper(JSONObjectMapper.create(GameInstallation.class));
		
		tokenConfig = ConfigLoader.loadFileConfig(new File("shittyauthlauncher/token.yml"));
		tokenConfig.registerMapper(JSONObjectMapper.create(LoginData.class));
		
		if(config.isEmpty()) {
			setMinecraftPath(DEFAULT_MINECRAFT_PATH);
			setGameDataPath(DEFAULT_GAME_DATA_PATH);
			setNewJavaPath("java");
			setOldJavaPath("java");
			setServers(DEFAULT_SERVERS);
			setSkinHost(DEFAULT_SKIN_HOST);
			setAlwaysPatchAuthlib(false);
			setAlwaysPatchMinecraft(false);
			setMinimizeLauncher(true);
			setInstallations(Collections.emptyList());
	    	save();
		}
	}
	
	public static void save() {
		config.saveToFile();
	}
	
	public static void setMinecraftPath(String path) {
		config.set("minecraft-path", path);
	}
	
	public static String getMinecraftPath() {
		return config.getString("minecraft-path", DEFAULT_MINECRAFT_PATH, false);
	}
	
	public static void setGameDataPath(String path) {
		config.set("game-data-path", path);
		config.saveToFile();
	}
	
	public static String getGameDataPath() {
		return config.getString("game-data-path", DEFAULT_GAME_DATA_PATH, false);
	}
	
	public static void setNewJavaPath(String path) {
		config.set("new-java-path", path);
	}
	
	public static String getNewJavaPath() {
		return config.getString("new-java-path", "java", false);
	}
	
	public static void setOldJavaPath(String path) {
		config.set("old-java-path", path);
	}
	
	public static String getOldJavaPath() {
		return config.getString("old-java-path", "java", false);
	}
	
	public static void setServers(ServerConfiguration configuration) {
		config.set("servers", configuration);
	}
	
	public static ServerConfiguration getServers() {
		return config.getGeneric("servers", ServerConfiguration.class, DEFAULT_SERVERS, false);
	}
	
	public static void setSkinHost(String url) {
		config.set("skin-host", url);
	}
	
	public static String getSkinHost() {
		return config.getString("skin-host", DEFAULT_SKIN_HOST, false);
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
		config.set("installations", installations.stream()
				.filter(i -> i != GameInstallation.DEFAULT_INSTALLATION)
				.collect(Collectors.toList()));
	}
	
	public static List<GameInstallation> getInstallations() {
		return config.getGenericList("installations", GameInstallation.class, new ArrayList<>(), false);
	}
	
	public static void setLoginData(LoginData data) {
		tokenConfig.set("loginData", data);
		tokenConfig.saveToFile();
	}
	
	public static LoginData getLoginData() {
		return tokenConfig.getGeneric("loginData", LoginData.class);
	}

}
