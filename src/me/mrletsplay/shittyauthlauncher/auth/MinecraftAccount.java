package me.mrletsplay.shittyauthlauncher.auth;

import java.util.Objects;
import java.util.UUID;

import me.mrletsplay.mrcore.json.converter.JSONConstructor;
import me.mrletsplay.mrcore.json.converter.JSONConvertible;
import me.mrletsplay.mrcore.json.converter.JSONValue;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public class MinecraftAccount implements JSONConvertible {
	
	@JSONValue
	private String id;
	
	@JSONValue
	private ServerConfiguration servers;

	@JSONValue
	private LoginData loginData;

	@JSONConstructor
	private MinecraftAccount() {}
	
	public MinecraftAccount(ServerConfiguration servers) {
		this.id = UUID.randomUUID().toString();
		this.servers = servers;
	}
	
	public String getId() {
		return id;
	}
	
	public ServerConfiguration getServers() {
		return servers;
	}
	
	public void setLoginData(LoginData loginData) {
		this.loginData = loginData;
	}
	
	public LoginData getLoginData() {
		return loginData;
	}
	
	public boolean isLoggedIn() {
		return loginData != null;
	}
	
	@Override
	public String toString() {
		return (isLoggedIn() ? loginData.getUsername() : "Not logged in") + " (" + servers.authServer + ")";
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MinecraftAccount other = (MinecraftAccount) obj;
		return Objects.equals(id, other.id);
	}
	
}
