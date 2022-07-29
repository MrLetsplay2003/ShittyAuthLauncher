package me.mrletsplay.shittyauthlauncher.auth;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import me.mrletsplay.mrcore.http.HttpGeneric;
import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.http.HttpResult;
import me.mrletsplay.mrcore.http.data.JSONObjectData;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public class AuthHelper {

	private static LoginData getLoginData(JSONObject response) {
		String name = response.getJSONObject("selectedProfile").getString("name");
		String userId = response.getJSONObject("selectedProfile").getString("id");
		String clientToken = response.getString("clientToken");
		String accessToken = response.getString("accessToken");
		return new LoginData(name, userId, clientToken, accessToken);
	}

	public static LoginData authenticate(String username, String password, ServerConfiguration servers) {
		HttpGeneric post = HttpRequest.createGeneric("POST", servers.authServer + "/authenticate");

		JSONObject req = new JSONObject();
		JSONObject agent = new JSONObject();
		agent.put("name", "Minecraft");
		agent.put("version", 1);
		req.put("agent", agent);
		req.put("username", username);
		req.put("password", password);
		post.setData(JSONObjectData.of(req));

		HttpResult r = post.execute();
		if(!r.isSuccess()) {
			JSONObject errorResponse = r.asJSONObject();
			System.out.println("Error response: " + errorResponse.toString());
			Alert a = new Alert(AlertType.ERROR);
			a.setTitle("Error");
			a.setContentText(errorResponse.getString("errorMessage"));
			a.showAndWait();
			return null;
		}

		return getLoginData(r.asJSONObject());
	}

	public static boolean validate(LoginData data, ServerConfiguration servers) {
		HttpGeneric post = HttpRequest.createGeneric("POST", servers.authServer + "/validate");
		post.setHeader("Content-Type", "application/json");

		JSONObject req = new JSONObject();
		req.put("accessToken", data.getAccessToken());
		req.put("clientToken", data.getClientToken());
		post.setData(JSONObjectData.of(req));

		HttpResult r = post.execute();
		return r.isSuccess();
	}

	public static LoginData refresh(LoginData data, ServerConfiguration servers) {
		HttpGeneric post = HttpRequest.createGeneric("POST", servers.authServer + "/refresh");
		post.setHeader("Content-Type", "application/json");

		JSONObject req = new JSONObject();
		req.put("accessToken", data.getAccessToken());
		req.put("clientToken", data.getClientToken());
		post.setData(JSONObjectData.of(req));

		HttpResult r = post.execute();
		if(!r.isSuccess()) return null;
		return getLoginData(r.asJSONObject());
	}

}
