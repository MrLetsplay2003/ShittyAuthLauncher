package me.mrletsplay.shittyauthlauncher.util;

public class LaunchException extends RuntimeException {

	private static final long serialVersionUID = 457335506008042303L;

	public LaunchException() {
		super();
	}

	public LaunchException(String message, Throwable cause) {
		super(message, cause);
	}

	public LaunchException(String message) {
		super(message);
	}

	public LaunchException(Throwable cause) {
		super(cause);
	}

}
