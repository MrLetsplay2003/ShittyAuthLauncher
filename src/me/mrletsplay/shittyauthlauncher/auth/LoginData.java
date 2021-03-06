package me.mrletsplay.shittyauthlauncher.auth;

import me.mrletsplay.mrcore.json.converter.JSONConstructor;
import me.mrletsplay.mrcore.json.converter.JSONConvertible;
import me.mrletsplay.mrcore.json.converter.JSONValue;

public class LoginData implements JSONConvertible {

	@JSONValue
	private String username;

	@JSONValue
	private String uuid;

	@JSONValue
	private String clientToken;

	@JSONValue
	private String accessToken;

	@JSONConstructor
	private LoginData() {}

	public LoginData(String username, String uuid, String clientToken, String accessToken) {
		this.username = username;
		this.uuid = uuid;
		this.clientToken = clientToken;
		this.accessToken = accessToken;
	}

	public String getUsername() {
		return username;
	}

	public String getUUID() {
		return uuid;
	}

	public String getClientToken() {
		return clientToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

}
