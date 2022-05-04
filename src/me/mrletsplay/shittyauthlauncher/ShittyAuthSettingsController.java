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
import me.mrletsplay.shittyauthlauncher.util.GameInstallation;
import me.mrletsplay.shittyauthlauncher.util.InstallationType;
import me.mrletsplay.shittyauthlauncher.util.ProfilesHelper;
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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

public class ShittyAuthSettingsController {
	public static ObservableList<DownloadsMirror> MirrorsList;
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
		MirrorsList = FXCollections.observableArrayList(new ArrayList<>());

		MirrorsList.addListener((ListChangeListener<DownloadsMirror>) v -> {
			boxMirrors.getChildren().removeIf(c -> !(c instanceof Button));
			for(DownloadsMirror acc : MirrorsList) {
				boxMirrors.getChildren().add(createMirrorItem(acc));
			}
		});

		//<Default Mirror>
		DownloadsMirror mirror = new MojangMirror();
		MirrorsList.add(mirror);
		MinecraftVersion.initVersions(mirror);
		controller.loadVersions();
		textUsing.setText("Current Mirror: "+mirror.name);
		ShittyAuthLauncher.mirror = mirror;
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
		MirrorsList.addAll(checkedMirrors);
	}


	public static List<DownloadsMirror> loadMirror(File mirrorsFile) {
		JSONObject p;
		try {
			p = new JSONObject(Files.readString(mirrorsFile.toPath(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new FriendlyException("Failed to read file", e);
		}

		List<DownloadsMirror> mirrors = new ArrayList<>();

		JSONArray pr = p.getJSONArray("mirrors");
		for(Object mirror2 : pr.toArray()) {
			JSONObject mirror3 = (JSONObject) mirror2;

			boolean custom = mirror3.getBoolean("custom");
			String name = mirror3.getString("name");
			String version_manifest = mirror3.getString("version_manifest");
			String assets_url = mirror3.getString("assets_url");


			DownloadsMirror toAdd = new DownloadsMirror();
			toAdd.custom = custom;
			toAdd.name = name;
			toAdd.version_manifest = version_manifest;
			toAdd.assets_url = assets_url;
			mirrors.add(toAdd);
		}
		return mirrors;
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
		DownloadsMirror inst = showEditMirrorDialog(null);
		if(inst != null) {
			inst.custom = true;
			MirrorsList.add(inst);
			JSONArray checkedMirrors = new JSONArray();
			for(DownloadsMirror mirror: MirrorsList){
				if(mirror.custom){
					System.out.println(mirror.name);
					JSONObject mirror2 = new JSONObject();
					mirror2.set("name", mirror.name);
					mirror2.set("version_manifest", mirror.version_manifest);
					mirror2.set("assets_url", mirror.assets_url);
					mirror2.set("custom", mirror.custom);
					checkedMirrors.add(mirror2);
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
		DownloadsMirror example = new MojangMirror();
		SimpleInputDialog dialog = new SimpleInputDialog()
				.addString("name", "Mirror Name", "Name", from != null ? from.name : example.name)
				.addString("manifest", "Manifest URL", "Manifest", from != null ? from.version_manifest : example.version_manifest)
				.addString("resources", "Resources URL", "Resources", from != null ? from.assets_url : example.assets_url)
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
			from.version_manifest = data.get("manifest");
			from.assets_url = data.get("resources");
			return from;
		} else {
			DownloadsMirror inst = new DownloadsMirror();
			inst.name = data.get("name");
			inst.version_manifest = data.get("manifest");
			inst.assets_url = data.get("resources");
			return inst;
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
			lbl1.setText(mirror.version_manifest);

			Label lbl2 = (Label) pr.lookup("#textResources");
			lbl2.setText(mirror.assets_url);


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
				MirrorsList.set(MirrorsList.indexOf(mirror), mirror); // Cause a list update
			});

			Button delete = (Button) pr.lookup("#buttonDelete");
			delete.setOnAction(event -> {
				if(DialogHelper.showYesNo("Do you really want to delete the mirror '" + mirror.name + "'?")) {
					MirrorsList.remove(mirror);
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
