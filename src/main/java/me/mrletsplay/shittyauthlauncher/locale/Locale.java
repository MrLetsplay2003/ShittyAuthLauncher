package me.mrletsplay.shittyauthlauncher.locale;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncherPlugins;

public interface Locale {

	public static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([a-zA-Z\\.]+)\\}");
	public static final String
		VARIABLE_GAME_NAME = "gameName";

	public String getID();

	public String getName();

	public String getVar(String var);

	public String getRaw(String key);

	public default String get(String key) {
		String val = getRaw(key);
		if(val == null) return null;
		Matcher m = VARIABLE_PATTERN.matcher(val);
		return m.replaceAll(r -> {
			if(VARIABLE_GAME_NAME.equals(r.group(1))) return ShittyAuthLauncherPlugins.getBrandingProvider().getGameName();
			String var = getVar(r.group(1));
			return var == null ? r.group() : var;
		});
	}

}
