package me.mrletsplay.shittyauthlauncher.util;

public class VersionNotFoundException extends LaunchException {

	private static final long serialVersionUID = -993248750640157476L;

	public VersionNotFoundException() {
		super();
	}

	public VersionNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public VersionNotFoundException(String message) {
		super(message);
	}

	public VersionNotFoundException(Throwable cause) {
		super(cause);
	}
	
}
