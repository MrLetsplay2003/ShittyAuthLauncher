package me.mrletsplay.shittyauthlauncher;

import java.io.File;

import me.mrletsplay.mrcore.config.ConfigLoader;
import me.mrletsplay.mrcore.config.FileCustomConfig;
import me.mrletsplay.mrcore.config.mapper.JSONObjectMapper;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;

public class ShittyAuthLauncherSettings {
	
	private static final String
		DEFAULT_MINECRAFT_CONTAINER = System.getProperty("os.name").toLowerCase().contains("windows") ? System.getenv("APPDATA") : System.getProperty("user.home"),
		DEFAULT_MINECRAFT_PATH = DEFAULT_MINECRAFT_CONTAINER + "/.minecraft",
		DEFAULT_GAME_DATA_PATH = DEFAULT_MINECRAFT_CONTAINER + "/.minecraft-shitty",
		DEFAULT_AUTH_SERVER_URL = "https://mc.graphite-official.com";
	
	private static FileCustomConfig config;
	private static FileCustomConfig tokenConfig;
	
	static {
		config = ConfigLoader.loadFileConfig(new File("shittyauthlauncher/settings.yml"));
		tokenConfig = ConfigLoader.loadFileConfig(new File("shittyauthlauncher/token.yml"));
		tokenConfig.registerMapper(JSONObjectMapper.create(LoginData.class));
		
		if(config.isEmpty()) {
			setMinecraftPath(DEFAULT_MINECRAFT_PATH);
			setGameDataPath(DEFAULT_GAME_DATA_PATH);
			setNewJavaPath("java");
			setOldJavaPath("java");
			setAuthServerURL(DEFAULT_AUTH_SERVER_URL);
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
	
	public static void setAuthServerURL(String url) {
		config.set("auth-server-url", url);
	}
	
	public static String getAuthServerURL() {
		return config.getString("auth-server-url", DEFAULT_AUTH_SERVER_URL, false);
	}
	
	public static void setLoginData(LoginData data) {
		tokenConfig.set("loginData", data);
		tokenConfig.saveToFile();
	}
	
	public static LoginData getLoginData() {
		return tokenConfig.getGeneric("loginData", LoginData.class);
	}

}
