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
import me.mrletsplay.mrcore.json.converter.JSONConverter;
import me.mrletsplay.mrcore.json.converter.SerializationOption;
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
import java.util.Objects;

public class ShittyAuthSettingsController {
	public static ObservableList<DownloadsMirror> mirrors;

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

	private void saveMirrors(){
		JSONArray checkedMirrors = new JSONArray();
		for(DownloadsMirror loopMirror: mirrors){
			if(loopMirror.custom){
				checkedMirrors.add(loopMirror.toJSON(SerializationOption.DONT_INCLUDE_CLASS));
			}
		}

		try {
			JSONObject obj = new JSONObject();
			obj.put("mirrors", checkedMirrors);
			obj.put("currentMirror", ShittyAuthLauncher.mirror.name);
			Files.writeString(Path.of(ShittyAuthLauncherSettings.dataPath+"/mirrors.json"), obj.toFancyString());
		} catch (IOException e) {
			System.err.println("Failed to write file");
			e.printStackTrace();
		}
	}

	public void init() {
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
		ShittyAuthLauncher.controller.loadVersions();
		textUsing.setText("Current Mirror: "+mirror.name);
		//</Default Mirror>

		File mirrorsJSON = new File(ShittyAuthLauncherSettings.dataPath+"/mirrors.json");
		List<DownloadsMirror> checkedMirrors = new ArrayList<>();
		if(mirrorsJSON.exists()) {
			checkedMirrors = loadMirrors(mirrorsJSON);
		}else {
			try {
				JSONObject obj = new JSONObject();
				obj.put("mirrors", new JSONArray());
				obj.put("currentMirror", mirror.name);
				Files.writeString(mirrorsJSON.toPath(), obj.toFancyString());
			} catch (IOException e) {
				System.err.println("Failed to write file");
				e.printStackTrace();
			}
		}
		mirrors.addAll(checkedMirrors);

		JSONObject p;
		try {
			p = new JSONObject(Files.readString(mirrorsJSON.toPath(), StandardCharsets.UTF_8));
			String mirrorName = p.getString("currentMirror");
			if(!Objects.equals(ShittyAuthLauncher.mirror.name, mirrorName)){
				for(DownloadsMirror checking : mirrors){
					System.out.println(checking.name);
					System.out.println(mirrorName);
					if(checking.name.equals(mirrorName)){
						MinecraftVersion.initVersions(checking);
						ShittyAuthLauncher.controller.loadVersions();
						ShittyAuthLauncher.controller.init();
						textUsing.setText("Current Mirror: "+checking.name);
						ShittyAuthLauncher.mirror = checking;
					}
				}
			}
		} catch (IOException e) {
			throw new FriendlyException("Failed to read file", e);
		}
	}


	public static List<DownloadsMirror> loadMirrors(File mirrorsFile) {
		List<DownloadsMirror> mirrorsToLoad = new ArrayList<>();
		try {
			JSONObject p = new JSONObject(Files.readString(mirrorsFile.toPath(), StandardCharsets.UTF_8));
			JSONArray pr = p.getJSONArray("mirrors");
			for(Object loopMirror : pr.toArray()) {
				JSONObject mirrorJSON = (JSONObject) loopMirror;
				DownloadsMirror mirror = JSONConverter.decodeObject(mirrorJSON, DownloadsMirror.class);
				mirrorsToLoad.add(mirror);
			}
		} catch (IOException e) {
			throw new FriendlyException("Failed to read file", e);
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
			saveMirrors();
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
					for (DownloadsMirror checking : mirrors){
						if(from != checking) {
							//This is true, if the 'from' mirror is in 'mirrors',
							//if the class is already there, we know we are just editing it, so false.
							if (checking.name.equals(d.get("name"))) return "Mirror with that name already exits";
						}
					}
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
				ShittyAuthLauncher.controller.loadVersions();
				ShittyAuthLauncher.controller.init();
				textUsing.setText("Current Mirror: "+mirror.name);
				ShittyAuthLauncher.mirror = mirror;
				saveMirrors();
			});

			Button edit = (Button) pr.lookup("#buttonEdit");
			edit.setOnAction(event -> {
				showEditMirrorDialog(mirror);
				mirrors.set(mirrors.indexOf(mirror), mirror); // Cause a list update
				saveMirrors();
			});

			Button delete = (Button) pr.lookup("#buttonDelete");
			delete.setOnAction(event -> {
				if(DialogHelper.showYesNo("Do you really want to delete the mirror '" + mirror.name + "'?")) {
					mirrors.remove(mirror);
					saveMirrors();
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
