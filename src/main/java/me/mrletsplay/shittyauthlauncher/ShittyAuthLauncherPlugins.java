package me.mrletsplay.shittyauthlauncher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

import me.mrletsplay.fxloader.FXLoader;
import me.mrletsplay.shittyauthlauncher.api.BrandingProvider;
import me.mrletsplay.shittyauthlauncher.api.DefaultsProvider;
import me.mrletsplay.shittyauthlauncher.api.MirrorProvider;
import me.mrletsplay.shittyauthlauncher.api.Theme;
import me.mrletsplay.shittyauthlauncher.api.ThemeProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultBrandingProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultDefaultsProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultMirrorProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultThemeProvider;
import me.mrletsplay.shittyauthlauncher.util.LauncherMeta;
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

		Set<URL> additionalURLs = new HashSet<>();
		for(PluginWrapper pl : pluginManager.getPlugins()) {
			try {
				Manifest mf = new Manifest(pl.getPluginClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
				String jfxModules = mf.getMainAttributes().getValue("Plugin-RequiredFXModules");

				if(jfxModules != null) {
					String[] modules = jfxModules.split(",");
					for(String module : modules) {
						FXLoader.downloadDependency(module.trim(), LauncherMeta.getJFXVersion()).forEach(p -> {
							try {
								additionalURLs.add(p.toUri().toURL());
							} catch (MalformedURLException e) {
								ShittyAuthLauncher.LOGGER.error("Failed to add JFX module '" + module + "' for plugin '" + pl.getPluginId() + "'", e);
							}
						});
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		// Add the required JFX modules to the classpath
		for(URL u : additionalURLs) {
			try {
				Method m = ShittyAuthLauncher.class.getClassLoader().getClass().getDeclaredMethod("addURL", URL.class);
				m.setAccessible(true);
				m.invoke(ShittyAuthLauncher.class.getClassLoader(), u);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}

		pluginManager.startPlugins();
		pluginManager.setSystemVersion(LauncherMeta.getLauncherSystemVersion());

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

		ShittyAuthLauncher.LOGGER.info("Loaded " + mirrorProviders.size() + " tab providers");

		ShittyAuthLauncher.LOGGER.info("Using " + brandingProvider.getClass().getName() + " as branding provider");
	}

	public static void startUI() {

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
