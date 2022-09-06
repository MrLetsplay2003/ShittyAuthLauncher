package me.mrletsplay.shittyauthlauncher.util;

import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;

public class OS {

	private static final OS CURRENT_OS;

	static {
		String osName = System.getProperty("os.name").toLowerCase();
		String osVersion = System.getProperty("os.version");
		String osArch = System.getProperty("os.arch");

		ShittyAuthLauncher.LOGGER.info("OS Name: " + osName + ", OS version: " + osVersion + ", OS arch: " + osArch);

		OSType type;
		if(osName.contains("windows")) {
			type = OSType.WINDOWS;
		}else if(osName.contains("mac") || osName.contains("darwin")) {
			type = OSType.MACOS;
		}else {
			type = OSType.LINUX;
		}

		CURRENT_OS = new OS(type, osName, osVersion, osArch);
	}

	private final OSType type;
	private final String name;
	private final String version;
	private final String arch;

	public OS(OSType type, String name, String version, String arch) {
		this.type = type;
		this.name = name;
		this.version = version;
		this.arch = arch;
	}

	public OSType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getArch() {
		return arch;
	}

	public String getVersion() {
		return version;
	}

	public static OS getCurrentOS() {
		return CURRENT_OS;
	}

}
