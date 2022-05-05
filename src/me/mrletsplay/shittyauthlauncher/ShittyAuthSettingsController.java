package me.mrletsplay.shittyauthlauncher;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.shittyauthlauncher.util.dialog.DialogData;
import me.mrletsplay.shittyauthlauncher.util.dialog.DialogHelper;
import me.mrletsplay.shittyauthlauncher.util.dialog.SimpleInputDialog;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;
import me.mrletsplay.shittyauthpatcher.mirrors.MojangMirror;
import me.mrletsplay.shittyauthpatcher.version.MinecraftVersion;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ShittyAuthSettingsController {
	public static ObservableList<DownloadsMirror> mirrors;
	private ShittyAuthController controller;
	
	@FXML
	private CheckBox checkboxAlwaysPatchAuthlib;
	
	@FXML
	private CheckBox checkboxUseAdoptium;
	
	@FXML
	private CheckBox checkboxAlwaysPatchMinecraft;
	
	@FXML
	private CheckBox checkboxMinimizeLauncher;

	@FXML
	private Label textUsing;

	@FXML
	private VBox boxMirrors;

	public void init(ShittyAuthController controller) {
		this.controller = controller;
		mirrors = FXCollections.observableArrayList(new ArrayList<>());

		mirrors.addListener((ListChangeListener<DownloadsMirror>) v -> {
			boxMirrors.getChildren().removeIf(c -> !(c instanceof Button));
			for(DownloadsMirror acc : mirrors) {
				boxMirrors.getChildren().add(createMirrorItem(acc));
			}
		});

		//<Default Mirror>
		DownloadsMirror mirror = new MojangMirror();
		mirrors.add(mirror);
		ShittyAuthLauncher.mirror = mirror;
		MinecraftVersion.initVersions(mirror);
		controller.loadVersions();
		textUsing.setText("Current Mirror: "+mirror.name);
		//</Default Mirror>

		File mirrorsJSON = new File("shittyauthlauncher/mirrors.json");


		List<DownloadsMirror> checkedMirrors = new ArrayList<>();
		if(mirrorsJSON.exists()) {
			checkedMirrors = loadMirror(mirrorsJSON);
		}else {
			try {
				JSONObject obj = new JSONObject();
				obj.put("mirrors", new JSONObject());
				Files.writeString(mirrorsJSON.toPath(), obj.toFancyString());
			} catch (IOException e) {
				System.err.println("Failed to write file");
				e.printStackTrace();
			}
		};
		mirrors.addAll(checkedMirrors);
	}


	public static List<DownloadsMirror> loadMirror(File mirrorsFile) {
		JSONObject p;
		try {
			p = new JSONObject(Files.readString(mirrorsFile.toPath(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new FriendlyException("Failed to read file", e);
		}

		List<DownloadsMirror> mirrorsToLoad = new ArrayList<>();

		JSONArray pr = p.getJSONArray("mirrors");
		for(Object loopMirror : pr.toArray()) {
			JSONObject mirrorJSON = (JSONObject) loopMirror;

			boolean custom = mirrorJSON.getBoolean("custom");
			String name = mirrorJSON.getString("name");
			String versionManifest = mirrorJSON.getString("versionManifest");
			String assetsURL = mirrorJSON.getString("assetsURL");


			DownloadsMirror mirrorAdding = new DownloadsMirror();
			mirrorAdding.custom = custom;
			mirrorAdding.name = name;
			mirrorAdding.versionManifest = versionManifest;
			mirrorAdding.assetsURL = assetsURL;
			mirrorsToLoad.add(mirrorAdding);
		}
		return mirrorsToLoad;
	}

	@FXML
	void buttonSave(ActionEvent event) {
		ShittyAuthLauncherSettings.setUseAdoptium(checkboxUseAdoptium.isSelected());
		ShittyAuthLauncherSettings.setAlwaysPatchAuthlib(checkboxAlwaysPatchAuthlib.isSelected());
		ShittyAuthLauncherSettings.setAlwaysPatchMinecraft(checkboxAlwaysPatchMinecraft.isSelected());
		ShittyAuthLauncherSettings.setMinimizeLauncher(checkboxMinimizeLauncher.isSelected());
		ShittyAuthLauncherSettings.save();
		ShittyAuthLauncher.settingsStage.hide();
	}

	@FXML
	void buttonCancel(ActionEvent event) {
		ShittyAuthLauncher.settingsStage.hide();
	}

	@FXML
	void buttonNewMirror(ActionEvent event) {
		DownloadsMirror mirror = showEditMirrorDialog(null);
		if(mirror != null) {
			mirror.custom = true;
			mirrors.add(mirror);
			JSONArray checkedMirrors = new JSONArray();
			for(DownloadsMirror loopMirror: mirrors){
				if(loopMirror.custom){
					System.out.println(loopMirror.name);
					JSONObject mirrorJSON = new JSONObject();
					mirrorJSON.set("name", loopMirror.name);
					mirrorJSON.set("versionManifest", loopMirror.versionManifest);
					mirrorJSON.set("assetsURL", loopMirror.assetsURL);
					mirrorJSON.set("custom", loopMirror.custom);
					checkedMirrors.add(mirrorJSON);
				}
			}
			try {
				JSONObject obj = new JSONObject();
				obj.put("mirrors", checkedMirrors);
				Files.writeString(Path.of("shittyauthlauncher/mirrors.json"), obj.toFancyString());
			} catch (IOException e) {
				System.err.println("Failed to write file");
				e.printStackTrace();
			}
		}
	}

	private DownloadsMirror showEditMirrorDialog(DownloadsMirror from) {
		DownloadsMirror defaultMirror = new MojangMirror();
		SimpleInputDialog dialog = new SimpleInputDialog()
				.addString("name",
						"Mirror Name",
						"Name",
						from != null ? from.name : defaultMirror.name)

				.addString("manifest",
						"Manifest URL",
						"Manifest",
						from != null ? from.versionManifest : defaultMirror.versionManifest)

				.addString("resources",
						"Resources URL",
						"Resources",
						from != null ? from.assetsURL : defaultMirror.assetsURL)

				.setVerifier(d -> {
					if (d.get("name") == null) return "Name may not be empty";
					if (d.get("manifest") == null) return "Manifest URL may not be empty";
					if (d.get("resources") == null) return "Resources URL may not be empty";
					return null;
				});

		DialogData data = dialog.show("Edit Mirror", "Enter Mirror settings");
		if (data == null) return null;

		if (from != null) {
			from.name = data.get("name");
			from.versionManifest = data.get("manifest");
			from.assetsURL = data.get("resources");
			if(!from.assetsURL.endsWith("/")){
				from.assetsURL = from.assetsURL.concat("/");
			}
			return from;
		} else {
			DownloadsMirror mirror = new DownloadsMirror();
			mirror.name = data.get("name");
			mirror.versionManifest = data.get("manifest");
			mirror.assetsURL = data.get("resources");
			if(!mirror.assetsURL.endsWith("/")){
				mirror.assetsURL = mirror.assetsURL.concat("/");
			}
			return mirror;
		}
	}

	private Node createMirrorItem(DownloadsMirror mirror) {
		try {
			URL url = ShittyAuthLauncher.class.getResource("/include/mirror-item.fxml");
			if(url == null) url = new File("./include/mirror-item.fxml").toURI().toURL();

			FXMLLoader l = new FXMLLoader(url);
			Parent pr = l.load(url.openStream());


			Label lbl = (Label) pr.lookup("#textName");
			lbl.setText(mirror.name);

			Label lbl1 = (Label) pr.lookup("#textManifest");
			lbl1.setText(mirror.versionManifest);

			Label lbl2 = (Label) pr.lookup("#textResources");
			lbl2.setText(mirror.assetsURL);


			Button switchMirror = (Button) pr.lookup("#buttonApply");
			switchMirror.setOnAction(event -> {
				MinecraftVersion.initVersions(mirror);
				controller.loadVersions();
				textUsing.setText("Current Mirror: "+mirror.name);
				ShittyAuthLauncher.mirror = mirror;
			});

			Button edit = (Button) pr.lookup("#buttonEdit");
			edit.setOnAction(event -> {
				showEditMirrorDialog(mirror);
				mirrors.set(mirrors.indexOf(mirror), mirror); // Cause a list update
			});

			Button delete = (Button) pr.lookup("#buttonDelete");
			delete.setOnAction(event -> {
				if(DialogHelper.showYesNo("Do you really want to delete the mirror '" + mirror.name + "'?")) {
					mirrors.remove(mirror);
				}
			});

			if(!mirror.custom){
				edit.setDisable(true);
				delete.setDisable(true);
			} else {
				edit.setDisable(false);
				delete.setDisable(false);
			}

			return pr;
		}catch(IOException e) {
			throw new FriendlyException(e);
		}
	}

	public void update() {
		checkboxUseAdoptium.setSelected(ShittyAuthLauncherSettings.isUseAdoptium());
		checkboxAlwaysPatchAuthlib.setSelected(ShittyAuthLauncherSettings.isAlwaysPatchAuthlib());
		checkboxAlwaysPatchMinecraft.setSelected(ShittyAuthLauncherSettings.isAlwaysPatchMinecraft());
		checkboxMinimizeLauncher.setSelected(ShittyAuthLauncherSettings.isMinimizeLauncher());
	}

}
