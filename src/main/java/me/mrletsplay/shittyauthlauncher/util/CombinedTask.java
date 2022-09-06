package me.mrletsplay.shittyauthlauncher.util;

import javafx.concurrent.Task;

public abstract class CombinedTask<V> extends Task<V> {

	private Task<?> runningTask;
	private Exception exception;
	
	protected <X> X runOther(Task<X> task) throws Exception {
		task.progressProperty().addListener(p -> updateProgress(task.getProgress(), 1));
		task.messageProperty().addListener(p -> updateMessage(task.getMessage()));
		task.setOnCancelled(event -> {
			runningTask = null; // Don't cancel the task again
			cancel();
		});
		task.setOnFailed(event -> {
			exception = (Exception) task.getException();
		});
		
		runningTask = task;
		task.run();
		if(exception != null) throw exception;
		runningTask = null;
		
		if(isCancelled()) return null;
		
		return task.get();
	}
	
	@Override
	protected void cancelled() {
		if(runningTask != null) runningTask.cancel();
	}
	
}
