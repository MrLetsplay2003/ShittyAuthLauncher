package me.mrletsplay.shittyauthlauncher.util;

public enum OSType {

	WINDOWS("windows", "bin/java.exe", "windows"),
	LINUX("linux", "bin/java", "linux"),
	MACOS("osx", "jre.bundle/Contents/Home/bin/java", "mac");
	
	private final String ruleName;
	private final String javaPath;
	private final String adoptiumName;
	
	private OSType(String ruleName, String javaPath, String adoptiumName) {
		this.ruleName = ruleName;
		this.javaPath = javaPath;
		this.adoptiumName = adoptiumName;
	}
	
	public String getRuleName() {
		return ruleName;
	}
	
	public String getJavaPath() {
		return javaPath;
	}
	
	public String getAdoptiumName() {
		return adoptiumName;
	}
	
}