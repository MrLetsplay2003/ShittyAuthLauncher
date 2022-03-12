package me.mrletsplay.shittyauthlauncher.version;

import me.mrletsplay.mrcore.json.converter.JSONPrimitiveStringConvertible;

public enum MinecraftVersionType implements JSONPrimitiveStringConvertible {
	
	RELEASE,
	SNAPSHOT,
	OLD_BETA,
	OLD_ALPHA;
	
	public static MinecraftVersionType decodePrimitive(Object o) {
		return valueOf(((String) o).toUpperCase());
	}
	
	@Override
	public String toJSONPrimitive() {
		return name().toLowerCase();
	}

}
