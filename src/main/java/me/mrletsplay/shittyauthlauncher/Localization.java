package me.mrletsplay.shittyauthlauncher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import me.mrletsplay.shittyauthlauncher.locale.Locale;
import me.mrletsplay.shittyauthlauncher.locale.LocalizedProps;

public class Localization {

	public static final String LOCALE_PREFIX = "@locale/";
	private static final String LOCALIZED_PROPS_KEY = "localized_props";

	public static Locale getDefaultLocale() {
		return ShittyAuthLauncherPlugins.getDefaultsProvider().getDefaultLocale();
	}

	public static void updateLocale(Locale locale) {
		replaceLocaleStrings(locale, ShittyAuthLauncher.stage.getScene().getRoot());
		replaceLocaleStrings(locale, ShittyAuthLauncher.settingsStage.getScene().getRoot());
	}

	public static Locale getLocale() {
		String l = ShittyAuthLauncherSettings.getLocale();
		Locale loc = l == null ? getDefaultLocale() : ShittyAuthLauncherPlugins.getLocale(l);
		if(loc == null) return getDefaultLocale();
		return loc;
	}

	public static String getString(Locale l, String string) {
		if(string == null || !string.startsWith(LOCALE_PREFIX)) return null;

		String key = string.substring(LOCALE_PREFIX.length());
		String val = l.get(key);
		return val == null ? string : val;
	}

	private static void replaceLocaleString(Locale l, StringProperty prop, LocalizedProps props) {
		String v = prop.getValue();

		String key = props.get(prop);
		if(v != null && v.startsWith(LOCALE_PREFIX)) {
			key = v.substring(LOCALE_PREFIX.length());
			props.set(prop, key);
		}

		if(key != null) {
			String val = l.get(key);
			if(val != null) prop.set(val);
		}
	}

	public static void replaceLocaleStrings(Locale l, Node n) {
		LocalizedProps props = (LocalizedProps) n.getProperties().getOrDefault(LOCALIZED_PROPS_KEY, new LocalizedProps());

		Class<?> clz = n.getClass();
		for(Method m : clz.getMethods()) {
			if(m.getReturnType().equals(StringProperty.class) && m.getParameters().length == 0) {
				// Possibly localizable string
				try {
					StringProperty prop = (StringProperty) m.invoke(n);
					replaceLocaleString(l, prop, props);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					continue;
				}
			}
		}

		if(n instanceof Parent) {
			for(Node ch : ((Parent) n).getChildrenUnmodifiable()) {
				replaceLocaleStrings(l, ch);
			}
		}

		if(n instanceof TabPane) {
			for(Tab t : ((TabPane) n).getTabs()) {
				replaceLocaleString(l, t.textProperty(), props);
				replaceLocaleStrings(l, t.getContent());
			}
		}

		if(!props.isEmpty()) n.getProperties().put(LOCALIZED_PROPS_KEY, props);
	}

}
