package me.mrletsplay.shittyauthlauncher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
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
import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.shittyauthlauncher.api.BrandingProvider;
import me.mrletsplay.shittyauthlauncher.api.DefaultsProvider;
import me.mrletsplay.shittyauthlauncher.api.IconProvider;
import me.mrletsplay.shittyauthlauncher.api.LocaleProvider;
import me.mrletsplay.shittyauthlauncher.api.MirrorProvider;
import me.mrletsplay.shittyauthlauncher.api.Theme;
import me.mrletsplay.shittyauthlauncher.api.ThemeProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultBrandingProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultDefaultsProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultIconProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultLocaleProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultMirrorProvider;
import me.mrletsplay.shittyauthlauncher.api.impl.DefaultThemeProvider;
import me.mrletsplay.shittyauthlauncher.locale.Locale;
import me.mrletsplay.shittyauthlauncher.util.LauncherMeta;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;

public class ShittyAuthLauncherPlugins {

	private static BrandingProvider brandingProvider;
	private static List<ThemeProvider> themeProviders;
	private static DefaultsProvider defaultsProvider;
	private static List<MirrorProvider> mirrorProviders;
	private static IconProvider iconProvider;
	private static List<LocaleProvider> localeProviders;

	private static PluginManager pluginManager;

	public static void load() {
		brandingProvider = null;
		themeProviders = new ArrayList<>();
		themeProviders.add(DefaultThemeProvider.INSTANCE);
		defaultsProvider = null;
		mirrorProviders = new ArrayList<>();
		mirrorProviders.add(DefaultMirrorProvider.INSTANCE);
		localeProviders = new ArrayList<>();
		localeProviders.add(DefaultLocaleProvider.INSTANCE);

		ShittyAuthLauncher.LOGGER.info("Loading plugins");
		Path pluginsFolder = Path.of(ShittyAuthLauncherSettings.DATA_PATH, "plugins");
		try {
			Files.createDirectories(pluginsFolder);
		} catch (IOException e2) {
			throw new FriendlyException("Failed to create plugins directory", e2);
		}
		pluginManager = new DefaultPluginManager(pluginsFolder);
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
		ShittyAuthLauncher.LOGGER.info("Loaded " + themeProviders.size() + " theme provider(s)");

		brandingProvider = loadOneProvider("branding", BrandingProvider.class, DefaultBrandingProvider.INSTANCE);
		ShittyAuthLauncher.LOGGER.info("Using " + brandingProvider.getClass().getName() + " as branding provider");

		defaultsProvider = loadOneProvider("defaults", DefaultsProvider.class, DefaultDefaultsProvider.INSTANCE);
		ShittyAuthLauncher.LOGGER.info("Using " + defaultsProvider.getClass().getName() + " as defaults provider");

		mirrorProviders.addAll(pluginManager.getExtensions(MirrorProvider.class));
		ShittyAuthLauncher.LOGGER.info("Loaded " + mirrorProviders.size() + " mirror provider(s)");

		iconProvider = loadOneProvider("icon", IconProvider.class, DefaultIconProvider.INSTANCE);
		ShittyAuthLauncher.LOGGER.info("Using " + iconProvider.getClass().getName() + " as icon provider");

		localeProviders.addAll(pluginManager.getExtensions(LocaleProvider.class));
		ShittyAuthLauncher.LOGGER.info("Loaded " + localeProviders.size() + " locale provider(s)");
	}

	private static <T> T loadOneProvider(String what, Class<T> providerClass, T defaultValue) {
		List<T> providers = pluginManager.getExtensions(providerClass);
		if(providers.isEmpty()) return defaultValue;
		if(providers.size() > 1) ShittyAuthLauncher.LOGGER.warn(String.format("There are multiple (%s) %s providers, make sure to only have one", providers.size(), what));
		return providers.get(0);
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

	public static IconProvider getIconProvider() {
		return iconProvider;
	}

	public static List<LocaleProvider> getLocaleProviders() {
		return localeProviders;
	}

	public static List<Locale> getLocales() {
		return localeProviders.stream()
			.flatMap(p -> p.getLocales().stream())
			.collect(Collectors.toList());
	}

	public static Locale getLocale(String id) {
		return localeProviders.stream()
			.flatMap(p -> p.getLocales().stream())
			.filter(t -> Objects.equals(id, t.getID()))
			.findFirst().orElse(null);
	}

}
