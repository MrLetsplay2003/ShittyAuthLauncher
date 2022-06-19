package me.mrletsplay.shittyauthlauncher.util;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.http.HttpResult;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;

public class SkinHelper {

	public static BufferedImage getSkinHead(ServerConfiguration servers, String uuid) {
		try {
			// TODO: improve
			HttpResult r = HttpRequest.createGet(servers.sessionServer + "/session/minecraft/profile/" + uuid).execute();
			if(!r.isSuccess()) return null;
			JSONObject obj = new JSONObject(r.asString());
			String tex = obj.getJSONArray("properties").getJSONObject(0).getString("value");
			JSONObject texts = new JSONObject(new String(Base64.getDecoder().decode(tex), StandardCharsets.UTF_8));
			BufferedImage img = ImageIO.read(HttpRequest.createGet(texts.getJSONObject("textures").getJSONObject("SKIN").getString("url")).executeAsInputStream());
			BufferedImage img2 = new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR);
			img2.createGraphics().drawImage(img, 0, 0, 256, 256, 8, 8, 16, 16, null);
			return img2;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

}
