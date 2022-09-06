package me.mrletsplay.shittyauthlauncher.api;

import java.io.InputStream;

import org.pf4j.ExtensionPoint;

public interface BrandingProvider extends ExtensionPoint {

	public String getLauncherBrand();

	public String getLauncherVersion();

	public InputStream loadIcon();

}
