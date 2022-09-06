package me.mrletsplay.shittyauthlauncher.api.impl;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.converter.JSONConverter;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;
import me.mrletsplay.shittyauthlauncher.api.Theme;
import me.mrletsplay.shittyauthlauncher.api.ThemeProvider;

public class DefaultThemeProvider implements ThemeProvider {

	public static final Theme NO_THEME = new Theme("shittyauthlauncher_default", "ShittyAuthLauncher Default", Collections.emptyList());

	private static final List<String> THEMES = Arrays.asList("dark");

	private List<Theme> themes;

	public DefaultThemeProvider() {
		this.themes = new ArrayList<>();
		themes.add(NO_THEME);

		for(String theme : THEMES) {
			URL url = ShittyAuthLauncher.class.getResource("/include/theme/" + theme + ".json");
			try {
				JSONObject obj = new JSONObject(new String(IOUtils.readAllBytes(url.openStream()), StandardCharsets.UTF_8));
				themes.add(JSONConverter.decodeObject(obj, Theme.class));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public Collection<Theme> getThemes() {
		return themes;
	}

}
