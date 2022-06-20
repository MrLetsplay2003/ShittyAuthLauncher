package me.mrletsplay.shittyauthlauncher.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

import me.mrletsplay.mrcore.http.HttpException;
import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.http.HttpResult;
import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncherSettings;
import me.mrletsplay.shittyauthlauncher.auth.MinecraftAccount;

public class SkinHelper {

	private static BufferedImage loadSkinHead(MinecraftAccount account) {
		try {
			if(account.getLoginData() == null) return null;
			HttpResult r = HttpRequest.createGet(account.getServers().sessionServer + "/session/minecraft/profile/" + account.getLoginData().getUUID()).execute();
			if(!r.isSuccess()) return null;
			JSONObject obj = new JSONObject(r.asString());
			String tex = obj.getJSONArray("properties").getJSONObject(0).getString("value");
			JSONObject texts = new JSONObject(new String(Base64.getDecoder().decode(tex), StandardCharsets.UTF_8));
			HttpResult r2 = HttpRequest.createGet(texts.getJSONObject("textures").getJSONObject("SKIN").getString("url")).execute();
			if(!r2.isSuccess()) return null;
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(r2.asRaw()));
			BufferedImage img2 = new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR);
			img2.createGraphics().drawImage(img, 0, 0, 256, 256, 8, 8, 16, 16, null);
			return img2;
		}catch (HttpException e) { // Session or skin server most likely unreachable
			System.err.println("Session/skin server for account '" + account.getLoginData().getUsername() + "@" + account.getServers().sessionServer + "' is unreachable");
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static BufferedImage getSkinHead(MinecraftAccount account) {
		if(account.getLoginData() == null) return null;
		File cacheFile = new File(ShittyAuthLauncherSettings.DATA_PATH + "/cache/heads/" + account.getServers().hashString() + "/" + account.getLoginData().getUUID() + ".png");
		try {
			if(cacheFile.exists()) return ImageIO.read(cacheFile);
			BufferedImage img = loadSkinHead(account);
			if(img == null) return null;
			IOUtils.createFile(cacheFile);
			ImageIO.write(img, "PNG", cacheFile);
			return img;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
