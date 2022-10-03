package me.mrletsplay.shittyauthlauncher.auth;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import me.mrletsplay.mrcore.http.HttpGeneric;
import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.http.HttpResult;
import me.mrletsplay.mrcore.http.data.JSONObjectData;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncherPlugins;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public class AuthHelper {

	public static LoginData authenticate(String username, String password, ServerConfiguration servers) {
		HttpGeneric post = HttpRequest.createGeneric("POST", servers.authServer + "/authenticate");
		post.setHeader("Content-Type", "application/json");

		post.setData(JSONObjectData.of(ShittyAuthLauncherPlugins.getAuthProvider().getAuthenticatePayload(username, password)));

		HttpResult r = post.execute();
		if(!r.isSuccess()) {
			JSONObject errorResponse = r.asJSONObject();
			ShittyAuthLauncher.LOGGER.info("Error response: " + errorResponse.toString());
			Alert a = new Alert(AlertType.ERROR);
			a.setTitle("Error");
			a.setContentText(errorResponse.getString("errorMessage"));
			a.showAndWait();
			return null;
		}

		return ShittyAuthLauncherPlugins.getAuthProvider().parseLoginResponse(r.asJSONObject());
	}

	public static boolean validate(LoginData data, ServerConfiguration servers) {
		HttpGeneric post = HttpRequest.createGeneric("POST", servers.authServer + "/validate");
		post.setHeader("Content-Type", "application/json");

		post.setData(JSONObjectData.of(ShittyAuthLauncherPlugins.getAuthProvider().getValidatePayload(data)));

		HttpResult r = post.execute();
		return r.isSuccess();
	}

	public static LoginData refresh(LoginData data, ServerConfiguration servers) {
		HttpGeneric post = HttpRequest.createGeneric("POST", servers.authServer + "/refresh");
		post.setHeader("Content-Type", "application/json");

		post.setData(JSONObjectData.of(ShittyAuthLauncherPlugins.getAuthProvider().getRefreshPayload(data)));

		HttpResult r = post.execute();
		if(!r.isSuccess()) return null;
		return ShittyAuthLauncherPlugins.getAuthProvider().parseLoginResponse(r.asJSONObject());
	}

}
