package me.mrletsplay.shittyauthlauncher.api;

import org.pf4j.ExtensionPoint;

import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthlauncher.auth.AuthHelper;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public interface AuthProvider extends ExtensionPoint {

	/**
	 * @return The {@link LoginData} contained in the response from an authenticate or refresh request, or <code>null</code> if the response is invalid
	 */
	public LoginData parseLoginResponse(JSONObject response);

	/**
	 * @param username The username of the user
	 * @param password The password of the user
	 * @return The payload for the authenticate request, used by {@link AuthHelper#authenticate(String, String, ServerConfiguration)}
	 */
	public JSONObject getAuthenticatePayload(String username, String password);

	/**
	 * @param loginData The {@link LoginData} to be validated
	 * @return The payload for the validate request, used by {@link AuthHelper#validate(LoginData, ServerConfiguration)}
	 */
	public JSONObject getValidatePayload(LoginData loginData);

	/**
	 * @param loginData The {@link LoginData} to be refreshed
	 * @return The payload for the refresh request, used by {@link AuthHelper#refresh(LoginData, ServerConfiguration)}
	 */
	public JSONObject getRefreshPayload(LoginData loginData);

}
