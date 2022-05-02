package me.mrletsplay.shittyauthlauncher.util.dialog;

import java.util.Map;

public class DialogData {
	
	private Map<String, Object> data;
	
	public DialogData(Map<String, Object> data) {
		this.data = data;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) data.get(key);
	}
	
	@Override
	public String toString() {
		return data.toString();
	}

}
