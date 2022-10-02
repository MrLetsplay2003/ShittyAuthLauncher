package me.mrletsplay.shittyauthlauncher.locale;

import me.mrletsplay.mrcore.json.JSONObject;

public class JSONLocale extends AbstractLocale {

	private JSONObject json;

	public JSONLocale(String id, String name, JSONObject json) {
		super(id, name);
		this.json = json;
	}

	public JSONLocale(String id, String name, String json) {
		this(id, name, new JSONObject(json));
	}

	@Override
	public String getVar(String var) {
		return json.optString("vars." + var).orElse(null);
	}

	@Override
	public String getRaw(String key) {
		return json.optString(key).orElse(null);
	}

}
