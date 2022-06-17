package me.mrletsplay.shittyauthlauncher;

import java.io.File;
import java.io.IOException;
import java.net.URL;

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
import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.shittyauthlauncher.util.dialog.DialogData;
import me.mrletsplay.shittyauthlauncher.util.dialog.DialogHelper;
import me.mrletsplay.shittyauthlauncher.util.dialog.SimpleInputDialog;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;

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
	private VBox boxMirrors;

	public void init() {
		mirrors = FXCollections.observableArrayList();
		mirrors.addListener((ListChangeListener<DownloadsMirror>) v -> {
			boxMirrors.getChildren().clear();
			for(DownloadsMirror mirror : mirrors) {
				boxMirrors.getChildren().add(createMirrorItem(mirror));
			}
		});
		mirrors.addAll(ShittyAuthLauncherSettings.getMirrors());
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
			mirrors.add(mirror);
			ShittyAuthLauncherSettings.setMirrors(mirrors);
		}
	}

	private DownloadsMirror showEditMirrorDialog(DownloadsMirror from) {
		DownloadsMirror defaultMirror = DownloadsMirror.MOJANG;
		SimpleInputDialog dialog = new SimpleInputDialog()
				.addString("name",
						"Mirror Name",
						"Name",
						from != null ? from.getName() : defaultMirror.getName())

				.addString("manifest",
						"Manifest URL",
						"Manifest",
						from != null ? from.getVersionManifestURL() : defaultMirror.getVersionManifestURL())

				.addString("resources",
						"Resources URL",
						"Resources",
						from != null ? from.getAssetsURL() : defaultMirror.getAssetsURL())

				.setVerifier(d -> {
					if(from == null) {
						// New mirror, check if one with that name already exists
						for (DownloadsMirror checking : mirrors){
							if (checking.getName().equals(d.get("name"))) return "Mirror with that name already exists";
						}
					}

					if (d.get("name") == null) return "Name may not be empty";
					if (d.get("manifest") == null) return "Manifest URL may not be empty";
					if (d.get("resources") == null) return "Resources URL may not be empty";
					return null;
				});

		DialogData data = dialog.show("Edit Mirror", "Enter Mirror settings");
		if (data == null) return null;

		DownloadsMirror mirror = from == null ? new DownloadsMirror() : from;
		mirror.setName(data.get("name"));
		mirror.setVersionManifestURL(data.get("manifest"));
		mirror.setAssetsURL(data.get("resources"));
		if(!mirror.getAssetsURL().endsWith("/")){
			mirror.setAssetsURL(from.getAssetsURL().concat("/"));
		}
		return mirror;
	}

	private Node createMirrorItem(DownloadsMirror mirror) {
		try {
			URL url = ShittyAuthLauncher.class.getResource("/include/mirror-item.fxml");
			if(url == null) url = new File("./include/mirror-item.fxml").toURI().toURL();

			FXMLLoader l = new FXMLLoader(url);
			Parent pr = l.load(url.openStream());


			Label lbl = (Label) pr.lookup("#textName");
			lbl.setText(mirror.getName());

			Label lbl1 = (Label) pr.lookup("#textManifest");
			lbl1.setText(mirror.getVersionManifestURL());

			Label lbl2 = (Label) pr.lookup("#textResources");
			lbl2.setText(mirror.getAssetsURL());

			Button edit = (Button) pr.lookup("#buttonEdit");
			edit.setOnAction(event -> {
				showEditMirrorDialog(mirror);
				mirrors.set(mirrors.indexOf(mirror), mirror); // Cause a list update
				ShittyAuthLauncherSettings.setMirrors(mirrors);
			});

			Button delete = (Button) pr.lookup("#buttonDelete");
			delete.setOnAction(event -> {
				if(DialogHelper.showYesNo("Do you really want to delete the mirror '" + mirror.getName() + "'?")) {
					mirrors.remove(mirror);
					ShittyAuthLauncherSettings.setMirrors(mirrors);
				}
			});

			if(mirror == DownloadsMirror.MOJANG){
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
