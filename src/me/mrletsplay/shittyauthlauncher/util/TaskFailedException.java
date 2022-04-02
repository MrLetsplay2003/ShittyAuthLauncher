package me.mrletsplay.shittyauthlauncher.util;

public class TaskFailedException extends RuntimeException {
	
	private static final long serialVersionUID = -5766160257176507870L;

	public TaskFailedException(Throwable t) {
		super(t);
	}

}
