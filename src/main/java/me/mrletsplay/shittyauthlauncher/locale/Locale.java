package me.mrletsplay.shittyauthlauncher.locale;

import java.util.HashMap;
import java.util.Map;
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

	public default String get(String key, String... vars) {
		if(vars.length % 2 != 0) throw new IllegalArgumentException("Variables must be alternating keys and values");

		Map<String, String> variables = new HashMap<>();
		for(int i = 0; i < vars.length; i += 2) {
			variables.put(vars[i], vars[i+1]);
		}

		String val = getRaw(key);
		if(val == null) return key;
		Matcher m = VARIABLE_PATTERN.matcher(val);
		return m.replaceAll(r -> {
			if(VARIABLE_GAME_NAME.equals(r.group(1))) return ShittyAuthLauncherPlugins.getBrandingProvider().getGameName();
			String var = variables.get(r.group(1));
			if(var == null) var = getVar(r.group(1));
			return var == null ? r.group() : var;
		});
	}

}
