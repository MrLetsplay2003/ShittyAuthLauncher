package me.mrletsplay.shittyauthlauncher.api;

import java.util.List;
import java.util.Objects;

import me.mrletsplay.mrcore.json.JSONType;
import me.mrletsplay.mrcore.json.converter.JSONConstructor;
import me.mrletsplay.mrcore.json.converter.JSONConvertible;
import me.mrletsplay.mrcore.json.converter.JSONListType;
import me.mrletsplay.mrcore.json.converter.JSONValue;

public class Theme implements JSONConvertible {

	@JSONValue
	private String id;

	@JSONValue
	private String name;

	@JSONValue
	@JSONListType(JSONType.STRING)
	private List<String> stylesheets;

	@JSONConstructor
	private Theme() {}

	public Theme(String id, String name, List<String> stylesheets) {
		this.id = id;
		this.name = name;
		this.stylesheets = stylesheets;
	}

	public String getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<String> getStylesheets() {
		return stylesheets;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Theme other = (Theme) obj;
		return Objects.equals(id, other.id);
	}

}
