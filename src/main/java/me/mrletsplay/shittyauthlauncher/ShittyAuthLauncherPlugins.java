package me.mrletsplay.shittyauthlauncher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

import me.mrletsplay.shittyauthlauncher.api.BrandingProvider;
import me.mrletsplay.shittyauthlauncher.api.DefaultsProvider;
import me.mrletsplay.shittyauthlauncher.api.MirrorProvider;
import me.mrletsplay.shittyauthlauncher.api.Theme;
import me.mrletsplay.shittyauthlauncher.api.ThemeProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultBrandingProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultDefaultsProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultMirrorProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultThemeProvider;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;

public class ShittyAuthLauncherPlugins {

	private static BrandingProvider brandingProvider;
	private static List<ThemeProvider> themeProviders;
	private static DefaultsProvider defaultsProvider;
	private static List<MirrorProvider> mirrorProviders;

	private static PluginManager pluginManager;

	public static void load() {
		brandingProvider = null;
		themeProviders = new ArrayList<>();
		themeProviders.add(DefaultThemeProvider.INSTANCE);
		defaultsProvider = null;
		mirrorProviders = new ArrayList<>();
		mirrorProviders.add(DefaultMirrorProvider.INSTANCE);

		ShittyAuthLauncher.LOGGER.info("Loading plugins");
		pluginManager = new DefaultPluginManager(Path.of(ShittyAuthLauncherSettings.DATA_PATH, "plugins"));
		pluginManager.loadPlugins();
		pluginManager.startPlugins();

		themeProviders.addAll(pluginManager.getExtensions(ThemeProvider.class));
		ShittyAuthLauncher.LOGGER.info("Loaded " + themeProviders.size() + " theme providers");

		List<BrandingProvider> brandingProviders = pluginManager.getExtensions(BrandingProvider.class);
		if(!brandingProviders.isEmpty()) {
			if(brandingProviders.size() > 1) ShittyAuthLauncher.LOGGER.warn("There are multiple (" + brandingProviders.size() + ") branding providers, make sure to only have one");
			brandingProvider = brandingProviders.get(0);
		}else {
			brandingProvider = DefaultBrandingProvider.INSTANCE;
		}

		List<DefaultsProvider> defaultsProviders = pluginManager.getExtensions(DefaultsProvider.class);
		if(!defaultsProviders.isEmpty()) {
			if(defaultsProviders.size() > 1) ShittyAuthLauncher.LOGGER.warn("There are multiple (" + defaultsProviders.size() + ") defaults providers, make sure to only have one");
			defaultsProvider = defaultsProviders.get(0);
		}else {
			defaultsProvider = DefaultDefaultsProvider.INSTANCE;
		}

		mirrorProviders.addAll(pluginManager.getExtensions(MirrorProvider.class));
		ShittyAuthLauncher.LOGGER.info("Loaded " + mirrorProviders.size() + " mirror providers");

		ShittyAuthLauncher.LOGGER.info("Using " + brandingProvider.getClass().getName() + " as branding provider");
	}

	public static void unload() {
		ShittyAuthLauncher.LOGGER.info("Unloading plugins");
		pluginManager.stopPlugins();
		pluginManager.unloadPlugins();
	}

	public static List<PluginWrapper> getPlugins() {
		return pluginManager.getPlugins();
	}

	public static BrandingProvider getBrandingProvider() {
		return brandingProvider;
	}

	public static List<ThemeProvider> getThemeProviders() {
		return themeProviders;
	}

	public static List<Theme> getThemes() {
		return themeProviders.stream()
			.flatMap(p -> p.getThemes().stream())
			.collect(Collectors.toList());
	}

	public static Theme getTheme(String id) {
		return themeProviders.stream()
			.flatMap(p -> p.getThemes().stream())
			.filter(t -> Objects.equals(id, t.getID()))
			.findFirst().orElse(null);
	}

	public static DefaultsProvider getDefaultsProvider() {
		return defaultsProvider;
	}

	public static List<MirrorProvider> getMirrorProviders() {
		return mirrorProviders;
	}

	public static List<DownloadsMirror> getMirrors() {
		return mirrorProviders.stream()
			.flatMap(p -> p.getMirrors().stream())
			.collect(Collectors.toList());
	}

}
