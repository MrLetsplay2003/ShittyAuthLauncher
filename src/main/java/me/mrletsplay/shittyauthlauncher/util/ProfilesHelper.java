package me.mrletsplay.shittyauthlauncher.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.misc.FriendlyException;

public class ProfilesHelper {

	private static final Pattern BASE64_DATA_URL = Pattern.compile("data:image/png;base64,(?<base64>.+)");

	public static List<GameInstallation> loadInstallations(File profilesFile) {
		JSONObject p;
		try {
			p = new JSONObject(Files.readString(profilesFile.toPath(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new FriendlyException("Failed to read file", e);
		}

		List<GameInstallation> installations = new ArrayList<>();

		JSONObject pr = p.getJSONObject("profiles");
		for(String k : pr.keySet()) {
			JSONObject profile = pr.getJSONObject(k);
			if(!profile.getString("type").equals("custom")) continue;

			String ver = profile.getString("lastVersionId");

			String icon = profile.optString("icon").orElse(null);
			if(icon != null) {
				Matcher m = BASE64_DATA_URL.matcher(icon);
				if(!m.matches()) {
					icon = null;
				}else {
					icon = m.group("base64");
				}
			}

			GameInstallation inst = new GameInstallation(
					InstallationType.CUSTOM,
					k,
					profile.getString("name"),
					icon,
					profile.optString("gameDir").orElse(profilesFile.getParentFile().getAbsolutePath()),
					profile.optString("javaDir").orElse(null),
					profile.optString("javaArgs").map(a -> Arrays.asList(a.split(" "))).orElse(null),
					ver);
			installations.add(inst);
		}
		return installations;
	}

}
