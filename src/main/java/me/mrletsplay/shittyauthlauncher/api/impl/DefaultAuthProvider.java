package me.mrletsplay.shittyauthlauncher.api.impl;

import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthlauncher.api.AuthProvider;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;

public class DefaultAuthProvider implements AuthProvider {

	public static final DefaultAuthProvider INSTANCE = new DefaultAuthProvider();

	protected DefaultAuthProvider() {}

	@Override
	public LoginData parseLoginResponse(JSONObject response) {
		String name = response.getJSONObject("selectedProfile").getString("name");
		String userId = response.getJSONObject("selectedProfile").getString("id");
		String clientToken = response.getString("clientToken");
		String accessToken = response.getString("accessToken");
		return new LoginData(name, userId, clientToken, accessToken);
	}

	@Override
	public JSONObject getAuthenticatePayload(String username, String password) {
		JSONObject req = new JSONObject();
		JSONObject agent = new JSONObject();
		agent.put("name", "Minecraft");
		agent.put("version", 1);
		req.put("agent", agent);
		req.put("username", username);
		req.put("password", password);
		return req;
	}

	@Override
	public JSONObject getValidatePayload(LoginData loginData) {
		JSONObject req = new JSONObject();
		req.put("accessToken", loginData.getAccessToken());
		req.put("clientToken", loginData.getClientToken());
		return req;
	}

	@Override
	public JSONObject getRefreshPayload(LoginData loginData) {
		JSONObject req = new JSONObject();
		req.put("accessToken", loginData.getAccessToken());
		req.put("clientToken", loginData.getClientToken());
		return req;
	}

}
