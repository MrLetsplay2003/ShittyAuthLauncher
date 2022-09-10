package me.mrletsplay.shittyauthlauncher.util;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;

public class LauncherMeta {

	private static String
		jfxVersion,
		launcherSystemVersion;

	static {
		URL url = ShittyAuthLauncher.class.getResource("/include/meta.json");
		try {
			JSONObject branding = new JSONObject(new String(IOUtils.readAllBytes(url.openStream()), StandardCharsets.UTF_8));
			jfxVersion = branding.getString("jfxVersion");
			launcherSystemVersion = branding.getString("launcherSystemVersion");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private LauncherMeta() {}

	public static String getJFXVersion() {
		return jfxVersion;
	}

	public static String getLauncherSystemVersion() {
		return launcherSystemVersion;
	}

}
