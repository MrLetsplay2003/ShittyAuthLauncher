package me.mrletsplay.shittyauthlauncher.locale;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.StringProperty;

public class LocalizedProps {

	private Map<StringProperty, String> props;

	public LocalizedProps() {
		this.props = new HashMap<>();
	}

	public String get(StringProperty prop) {
		return props.get(prop);
	}

	public void set(StringProperty prop, String key) {
		props.put(prop, key);
	}

	public boolean isEmpty() {
		return props.isEmpty();
	}

}
