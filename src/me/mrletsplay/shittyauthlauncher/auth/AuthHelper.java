package me.mrletsplay.shittyauthlauncher.auth;

import java.nio.charset.StandardCharsets;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import me.mrletsplay.mrcore.http.HttpGeneric;
import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncherSettings;

public class AuthHelper {
	
	public static LoginData authenticate(String username, String password) {
		try {
			HttpGeneric post = HttpRequest.createGeneric("POST", ShittyAuthLauncherSettings.getAuthServerURL() + "/authenticate");
			
			JSONObject req = new JSONObject();
			req.put("username", username);
			req.put("password", password);
			post.setContent(req.toString().getBytes(StandardCharsets.UTF_8));
			
			JSONObject response = post.execute().asJSONObject();
			return new LoginData(username, response.getJSONObject("user").getString("id"), response.getString("accessToken"));
		}catch(IllegalStateException e) {
			Alert a = new Alert(AlertType.ERROR);
			a.setTitle("Error");
			a.setContentText("Invalid credentials or auth server not reachable");
			a.showAndWait();
			return null;
		}
	}

}
