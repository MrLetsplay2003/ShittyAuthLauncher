package me.mrletsplay.shittyauthlauncher.locale;

import java.util.Objects;

public abstract class AbstractLocale implements Locale {

	private String id;
	private String name;

	public AbstractLocale(String id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public String getName() {
		return name;
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
		AbstractLocale other = (AbstractLocale) obj;
		return Objects.equals(id, other.id);
	}

}