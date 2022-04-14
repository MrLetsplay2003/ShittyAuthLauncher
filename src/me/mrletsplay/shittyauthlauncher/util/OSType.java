package me.mrletsplay.shittyauthlauncher.util;

public enum OSType {

	WINDOWS("windows"),
	LINUX("linux"),
	MACOS("osx");
	
	private final String ruleName;
	
	private OSType(String ruleName) {
		this.ruleName = ruleName;
	}
	
	public String getRuleName() {
		return ruleName;
	}
	
}