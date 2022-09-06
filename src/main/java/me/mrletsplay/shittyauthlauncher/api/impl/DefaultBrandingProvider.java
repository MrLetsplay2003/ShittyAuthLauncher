package me.mrletsplay.shittyauthlauncher.api.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;
import me.mrletsplay.shittyauthlauncher.api.BrandingProvider;

public class DefaultBrandingProvider implements BrandingProvider {

	private String
		launcherBrand,
		launcherVersion,
		iconPath;

	public DefaultBrandingProvider() {
		URL url = ShittyAuthLauncher.class.getResource("/include/branding.json");
		try {
			JSONObject branding = new JSONObject(new String(IOUtils.readAllBytes(url.openStream()), StandardCharsets.UTF_8));
			launcherBrand = branding.getString("launcherBrand");
			launcherVersion = branding.getString("launcherVersion");
			iconPath = branding.getString("icon");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getLauncherBrand() {
		return launcherBrand;
	}

	@Override
	public String getLauncherVersion() {
		return launcherVersion;
	}

	@Override
	public InputStream loadIcon() {
		return ShittyAuthLauncher.class.getResourceAsStream(iconPath);
	}

}
