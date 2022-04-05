package me.mrletsplay.shittyauthlauncher.auth;

import java.nio.charset.StandardCharsets;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import me.mrletsplay.mrcore.http.HttpGeneric;
import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.http.HttpResult;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncherSettings;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public class AuthHelper {
	
	public static LoginData authenticate(String username, String password) {
		try {
			ServerConfiguration servers = ShittyAuthLauncherSettings.getServers();
			HttpGeneric post = HttpRequest.createGeneric("POST", servers.authServer + "/authenticate");
			post.setHeaderParameter("Content-Type", "application/json");
			
			JSONObject req = new JSONObject();
			JSONObject agent = new JSONObject();
			agent.put("name", "Minecraft");
			agent.put("version", 1);
			req.put("agent", agent);
			req.put("username", username);
			req.put("password", password);
			post.setContent(req.toString().getBytes(StandardCharsets.UTF_8));
			
			HttpResult r = post.execute();
			if(!r.isSuccess()) {
				System.out.println("Error response: " + r.getErrorResponse());
			}
			JSONObject response = r.asJSONObject();
			return new LoginData(response.getJSONObject("selectedProfile").getString("name"), response.getJSONObject("selectedProfile").getString("id"), response.getString("accessToken"));
		}catch(IllegalStateException e) {
			Alert a = new Alert(AlertType.ERROR);
			a.setTitle("Error");
			a.setContentText("Invalid credentials or auth server not reachable");
			a.showAndWait();
			return null;
		}
	}

}
