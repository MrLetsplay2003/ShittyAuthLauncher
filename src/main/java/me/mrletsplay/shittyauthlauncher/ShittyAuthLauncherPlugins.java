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
import me.mrletsplay.shittyauthlauncher.api.Theme;
import me.mrletsplay.shittyauthlauncher.api.ThemeProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultBrandingProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultThemeProvider;

public class ShittyAuthLauncherPlugins {

	private static BrandingProvider brandingProvider;
	private static List<ThemeProvider> themeProviders;

	private static PluginManager pluginManager;

	public static void load() {
		brandingProvider = null;
		themeProviders = new ArrayList<>();
		themeProviders.add(new DefaultThemeProvider());

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
			brandingProvider = new DefaultBrandingProvider();
		}

		ShittyAuthLauncher.LOGGER.info("Using " + brandingProvider.getClass().getName() + " as branding provider");
	}

	public static void unload() {
		ShittyAuthLauncher.LOGGER.info("Unloading plugins");
		pluginManager.stopPlugins();
		pluginManager.unloadPlugins();
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

	public static List<PluginWrapper> getPlugins() {
		return pluginManager.getPlugins();
	}

}
