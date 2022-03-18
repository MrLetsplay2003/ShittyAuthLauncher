package me.mrletsplay.shittyauthlauncher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import me.mrletsplay.shittyauthlauncher.auth.AuthHelper;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;
import me.mrletsplay.shittyauthlauncher.version.MinecraftVersion;
import me.mrletsplay.shittyauthlauncher.version.MinecraftVersionType;

public class ShittyAuthController {

	private ObservableList<MinecraftVersion> versionsListRelease;
	private ObservableList<MinecraftVersion> versionsList;

	@FXML
	private ComboBox<MinecraftVersion> dropdownVersions;

	@FXML
	private TextArea textLog;

	@FXML
	private Label textLoggedIn;

	@FXML
	private CheckBox checkboxShowAllVersions;

	public void init() {
		versionsList = FXCollections.observableArrayList(new ArrayList<>(MinecraftVersion.VERSIONS));
		List<MinecraftVersion> releases = MinecraftVersion.VERSIONS.stream()
				.filter(v -> v.getType() == MinecraftVersionType.RELEASE)
				.collect(Collectors.toList());
		versionsListRelease = FXCollections.observableArrayList(releases);
		dropdownVersions.setItems(versionsListRelease);
		dropdownVersions.getSelectionModel().select(releases.indexOf(MinecraftVersion.LATEST_RELEASE));
		updateLogin();
	}

	public void updateLogin() {
		LoginData d = ShittyAuthLauncherSettings.getLoginData();
		if (d != null) {
			textLoggedIn.setText(d.getUsername());
		}
	}

	@FXML
	void buttonChangeUser(ActionEvent event) {
		ButtonType login = new ButtonType("Login", ButtonData.OK_DONE);
		Dialog<Pair<String, String>> dialog = new Dialog<>();
		dialog.initOwner(ShittyAuthLauncher.stage);
		dialog.initStyle(StageStyle.UTILITY);
		dialog.setTitle("Login");
		dialog.setHeaderText("Enter your credentials");
		dialog.getDialogPane().getButtonTypes().addAll(login, ButtonType.CANCEL);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));

		TextField username = new TextField();
		username.setPromptText("Username");
		PasswordField password = new PasswordField();
		password.setPromptText("Password");

		grid.add(new Label("Username"), 0, 0);
		grid.add(username, 1, 0);
		grid.add(new Label("Password"), 0, 1);
		grid.add(password, 1, 1);

		dialog.getDialogPane().setContent(grid);
		Platform.runLater(() -> username.requestFocus());

		dialog.setResultConverter(type -> {
			if (type == login) {
				return new Pair<>(username.getText(), password.getText());
			}

			return null;
		});

		Optional<Pair<String, String>> creds = dialog.showAndWait();
		if (!creds.isPresent())
			return;
		Pair<String, String> p = creds.get();
		String user = p.getKey();
		String pass = p.getValue();
		LoginData data = AuthHelper.authenticate(user, pass);
		if(data != null) {
			ShittyAuthLauncherSettings.setLoginData(data);
	    	updateLogin();
		}else {
			
		}
	}

	@FXML
	void buttonPlay(ActionEvent event) {
		MinecraftVersion ver = dropdownVersions.getSelectionModel().getSelectedItem();
		if(ver == null) {
			DialogHelper.showError("No version selected");
			return;
		}
		ver.launch();
	}

	@FXML
	void buttonSettings(ActionEvent event) {
    	ShittyAuthLauncher.settingsStage.show();
	}

	@FXML
	void checkboxShowAllVersions(ActionEvent event) {
		if (checkboxShowAllVersions.isSelected()) {
			dropdownVersions.setItems(versionsList);
		} else {
			dropdownVersions.setItems(versionsListRelease);
		}
	}

}
