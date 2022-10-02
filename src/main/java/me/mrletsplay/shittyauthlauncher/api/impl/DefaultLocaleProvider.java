package me.mrletsplay.shittyauthlauncher.api.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;
import me.mrletsplay.shittyauthlauncher.api.LocaleProvider;
import me.mrletsplay.shittyauthlauncher.locale.JSONLocale;
import me.mrletsplay.shittyauthlauncher.locale.Locale;

public class DefaultLocaleProvider implements LocaleProvider {

	public static final DefaultLocaleProvider INSTANCE = new DefaultLocaleProvider();

	private List<Locale> locales;

	private DefaultLocaleProvider() {
		this.locales = new ArrayList<>();
		locales.add(loadLocale("en_US", "English (US)"));
		locales.add(loadLocale("de_DE", "Deutsch"));
	}

	private Locale loadLocale(String id, String name) {
		try {
			return new JSONLocale(id, name, new String(IOUtils.readAllBytes(ShittyAuthLauncher.class.getResourceAsStream("/include/locale/" + id + ".json")), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new FriendlyException(e);
		}
	}

	@Override
	public List<Locale> getLocales() {
		return locales;
	}

}
