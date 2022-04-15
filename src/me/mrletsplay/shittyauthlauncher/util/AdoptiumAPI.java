package me.mrletsplay.shittyauthlauncher.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import javafx.concurrent.Task;
import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;

public class AdoptiumAPI {
	
	private static final List<Integer> AVAILABLE_VERSIONS;
	
	static {
		AVAILABLE_VERSIONS = new ArrayList<>();
		JSONObject releases = HttpRequest.createGet("https://api.adoptium.net/v3/info/available_releases").execute().asJSONObject();
		JSONArray r = releases.getJSONArray("available_releases");
		for(int i = 0; i < r.size(); i++) {
			AVAILABLE_VERSIONS.add(r.getInt(i));
		}
		Collections.sort(AVAILABLE_VERSIONS);
	}
	
	public static Task<File> downloadJRE(int majorVersion, File folder) {
		int ver = AVAILABLE_VERSIONS.stream()
				.filter(v -> v >= majorVersion)
				.findFirst().orElseThrow(() -> new LaunchException("Release not available from Adoptium: " + majorVersion));
		
		return new CombinedTask<File>() {
			
			@Override
			protected File call() throws Exception {
				File executable = new File(folder, OS.getCurrentOS().getType().getJavaPath());
				if(executable.exists()) return executable;
				
				updateMessage("Downloading package from Adoptium");
				
				JSONObject r = HttpRequest.createGet("https://api.adoptium.net/v3/assets/latest/" + ver + "/hotspot")
						.setHeaderParameter("Accept", "application/json")
						.addQueryParameter("architecture", "x64")
						.addQueryParameter("image_type", "jdk")
						.addQueryParameter("os", OS.getCurrentOS().getType().getAdoptiumName())
						.execute().asJSONArray().getJSONObject(0);
				
				updateMessage("Extracting package from Adoptium");
				
				String dl = r.getJSONObject("binary").getJSONObject("package").getString("link");
				File file = new File(folder, "download");
				HttpRequest.createGet(dl).execute().transferTo(file);
				extractTarGZ(file, folder);
				file.delete();
				
				return executable;
			}
		};
	}
	
	private static void extractTarGZ(File archiveFile, File folder) {
		try(TarArchiveInputStream in = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(archiveFile)))) {
			TarArchiveEntry en;
			while((en = in.getNextTarEntry()) != null) {
				if(en.isDirectory()) continue;
				Path p = Path.of(en.getName());
				Path destPath = p.subpath(1, p.getNameCount());
				File out = new File(folder, destPath.toString());
				IOUtils.createFile(out);
				try(FileOutputStream fOut = new FileOutputStream(out)) {
					byte[] buf = new byte[1024];
					int len;
					while((len = in.read(buf)) > 0) {
						fOut.write(buf, 0, len);
					}
				}
				
				if(OS.getCurrentOS().getType() != OSType.WINDOWS) {
					if((en.getMode() & (1 << 6)) == (1 << 6)) { // owner executable flag
						Set<PosixFilePermission> perms = new HashSet<>();
						perms.add(PosixFilePermission.OWNER_EXECUTE);
						perms.add(PosixFilePermission.OWNER_READ);
						perms.add(PosixFilePermission.OWNER_WRITE);
						Files.setPosixFilePermissions(out.toPath(), perms);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
