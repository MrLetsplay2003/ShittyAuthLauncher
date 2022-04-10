package me.mrletsplay.shittyauthlauncher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import me.mrletsplay.shittyauthlauncher.auth.AuthHelper;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;
import me.mrletsplay.shittyauthlauncher.util.GameInstallation;
import me.mrletsplay.shittyauthlauncher.util.LaunchHelper;
import me.mrletsplay.shittyauthpatcher.version.MinecraftVersion;
import me.mrletsplay.shittyauthpatcher.version.MinecraftVersionType;

public class ShittyAuthController {

	private ObservableList<MinecraftVersion> versionsListRelease;
	private ObservableList<MinecraftVersion> versionsList;
	private ObservableList<GameInstallation> installationsList;

	@FXML
	private ComboBox<MinecraftVersion> dropdownVersions;

	@FXML
	private ComboBox<GameInstallation> dropdownInstallations;

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
		dropdownVersions.getSelectionModel().select(MinecraftVersion.LATEST_RELEASE);
		dropdownVersions.setOnAction(e -> {
			GameInstallation inst = dropdownInstallations.getValue();
			if(inst == GameInstallation.DEFAULT_INSTALLATION) return;
			inst.lastVersionId = dropdownVersions.getValue().getId();
			ShittyAuthLauncherSettings.setInstallations(installationsList);
			ShittyAuthLauncherSettings.save();
		});
		
		installationsList = FXCollections.observableArrayList();
		installationsList.add(GameInstallation.DEFAULT_INSTALLATION);
		installationsList.addAll(ShittyAuthLauncherSettings.getInstallations());
		dropdownInstallations.setItems(installationsList);
		dropdownInstallations.getSelectionModel().select(GameInstallation.DEFAULT_INSTALLATION);
		dropdownInstallations.setOnAction(e -> {
			GameInstallation inst = dropdownInstallations.getValue();
			if(inst == GameInstallation.DEFAULT_INSTALLATION) {
				dropdownVersions.getSelectionModel().select(MinecraftVersion.LATEST_RELEASE);
			}else {
				dropdownVersions.getSelectionModel().select(MinecraftVersion.getVersion(inst.lastVersionId));
			}
		});
		
		updateLogin();
	}

	public void updateLogin() {
		LoginData d = ShittyAuthLauncherSettings.getLoginData();
		if (d != null) {
			textLoggedIn.setText(d.getUsername());
		}
	}
	
	public void updateInstallations() {
		List<GameInstallation> installations = ShittyAuthLauncherSettings.getInstallations();
		installationsList.addAll(installations);
		
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
		}
	}

	@FXML
	void buttonPlay(ActionEvent event) {
		MinecraftVersion ver = dropdownVersions.getSelectionModel().getSelectedItem();
		if(ver == null) {
			DialogHelper.showError("No version selected");
			return;
		}
		
		GameInstallation installation = dropdownInstallations.getValue();
		if(installation == GameInstallation.DEFAULT_INSTALLATION) installation = null;
		LaunchHelper.launch(ver, installation);
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

	@FXML
	void buttonInstallations(ActionEvent event) throws IOException {
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(ShittyAuthLauncher.stage);
		dialog.initStyle(StageStyle.UTILITY);
		dialog.setTitle("Installations");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
		
		VBox vbox = new VBox(10);
		vbox.setMaxWidth(Double.MAX_VALUE);
		vbox.setMaxHeight(Double.MAX_VALUE);
		vbox.setFillWidth(true);
		
		ScrollPane scroll = new ScrollPane(vbox);
		scroll.setPrefWidth(500);
		scroll.setPrefHeight(300);
		scroll.setMaxWidth(Double.MAX_VALUE);
		scroll.setMaxHeight(Double.MAX_VALUE);
		scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		scroll.setFitToWidth(true);
		
		for(GameInstallation inst : installationsList) {
			if(inst == GameInstallation.DEFAULT_INSTALLATION) continue;
			vbox.getChildren().add(createInstallationItem(inst));
		}
		
		Button newInst = new Button("Create New...");
		newInst.setOnAction(e -> {
			GameInstallation inst = showEditInstallationDialog(null);
			if(inst != null) {
				installationsList.add(inst);
				ShittyAuthLauncherSettings.setInstallations(installationsList);
				ShittyAuthLauncherSettings.save();
			}
		});
		vbox.getChildren().add(newInst);
		
		InvalidationListener l = v -> {
			vbox.getChildren().clear();
			for(GameInstallation inst : installationsList) {
				if(inst == GameInstallation.DEFAULT_INSTALLATION) continue;
				vbox.getChildren().add(createInstallationItem(inst));
			}
			vbox.getChildren().add(newInst);
		};
		
		installationsList.addListener(l);
		
		dialog.getDialogPane().setContent(scroll);

		dialog.showAndWait();
		installationsList.removeListener(l);
	}
	
	private Node createInstallationItem(GameInstallation installation) {
		HBox hbox = new HBox();
		hbox.setSpacing(10);
		hbox.setPadding(new Insets(10, 10, 10, 10));
		hbox.setAlignment(Pos.CENTER);
		
		ImageView img = new ImageView();
		img.setFitWidth(64);
		img.setFitHeight(64);
		img.setImage(new Image(ShittyAuthController.class.getResourceAsStream("/include/icon.png")));
		hbox.getChildren().add(img);
		
		Label lbl = new Label(installation.name);
		HBox.setHgrow(lbl, Priority.ALWAYS);
		lbl.setMaxHeight(Double.MAX_VALUE);
		lbl.setMaxWidth(Double.MAX_VALUE);
		hbox.getChildren().add(lbl);
		
		VBox buttons = new VBox(10);
		
		Button btn = new Button();
		btn.setText("Edit");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(event -> {
			showEditInstallationDialog(installation);
			installationsList.set(installationsList.indexOf(installation), installation); // Cause a list update
			ShittyAuthLauncherSettings.setInstallations(installationsList);
			ShittyAuthLauncherSettings.save();
		});
		buttons.getChildren().add(btn);
		
		Button btn2 = new Button();
		btn2.setText("Delete");
		btn2.setMaxWidth(Double.MAX_VALUE);
		btn2.setOnAction(event -> {
			if(DialogHelper.showYesNo("Do you really want to delete the installation '" + installation.name + "'?\n\n"
					+ "Note: This will only remove it from the launcher, the game data folder will not be deleted")) {
				installationsList.remove(installation);
				ShittyAuthLauncherSettings.setInstallations(installationsList);
				ShittyAuthLauncherSettings.save();
			}
		});
		buttons.getChildren().add(btn2);
		
		hbox.getChildren().add(buttons);
		
		return hbox;
	}
	
	private GameInstallation showEditInstallationDialog(GameInstallation from) {
		Dialog<GameInstallation> dialog = new Dialog<>();
		dialog.initOwner(ShittyAuthLauncher.stage);
		dialog.initStyle(StageStyle.UTILITY);
		dialog.setTitle("Edit Installation");
		dialog.setHeaderText("Enter installation details");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.FINISH, ButtonType.CANCEL);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 10, 10, 10));

		TextField name = new TextField();
		name.setPromptText("Name");
		name.setPrefWidth(300);
		if(from != null) name.setText(from.name);
		
		TextField directory = new TextField();
		directory.setPromptText("Directory");
		directory.setPrefWidth(300);
		if(from != null) directory.setText(from.gameDirectory);
		Button browseDir = new Button("Browse...");
		browseDir.setOnAction(event -> {
			DirectoryChooser ch = new DirectoryChooser();
			File gameDir = new File(ShittyAuthLauncherSettings.getGameDataPath());	
			if(gameDir.exists()) ch.setInitialDirectory(gameDir);
			File f = ch.showDialog(dialog.getDialogPane().getScene().getWindow());
			if(f != null) directory.setText(f.getAbsolutePath());
		});
		
		TextField javaPath = new TextField();
		javaPath.setPromptText("Java Path");
		javaPath.setPrefWidth(300);
		if(from != null) javaPath.setText(from.javaPath);
		Button browseJavaPath = new Button("Browse...");
		browseJavaPath.setOnAction(event -> {
			FileChooser ch = new FileChooser();
			File gameDir = new File(ShittyAuthLauncherSettings.getGameDataPath());	
			if(gameDir.exists()) ch.setInitialDirectory(gameDir);
			File f = ch.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
			if(f != null) javaPath.setText(f.getAbsolutePath());
		});

		grid.add(new Label("Installation Name"), 0, 0);
		grid.add(name, 1, 0);
		grid.add(new Label("Directory"), 0, 1);
		grid.add(directory, 1, 1);
		grid.add(browseDir, 2, 1);
		grid.add(new Label("Java Path"), 0, 2);
		grid.add(javaPath, 1, 2);
		grid.add(browseJavaPath, 2, 2);

		dialog.getDialogPane().setContent(grid);
		Platform.runLater(() -> name.requestFocus());

		dialog.setResultConverter(type -> {
			if (type == ButtonType.FINISH) {
				String nm = getString(name);
				String gameDir = getString(directory);
				String java = getString(javaPath);
				
				if(nm == null || gameDir == null) {
					DialogHelper.showError("Both name and game directory need to be set");
					return null;
				}
				
				if(from != null) {
					from.name = nm;
					from.gameDirectory = gameDir;
					from.javaPath = java;
					return from;
				}else {
					GameInstallation inst = new GameInstallation();
					inst.id = UUID.randomUUID().toString();
					inst.name = nm;
					inst.gameDirectory = gameDir;
					inst.javaPath = java;
					inst.lastVersionId = MinecraftVersion.LATEST_RELEASE.getId();
					return inst;
				}
			}

			return null;
		});

		Optional<GameInstallation> res = dialog.showAndWait();
		return res.orElse(null);
	}
    
    private String getString(TextField textField) {
    	String txt = textField.getText();
    	if(txt.isBlank()) txt = null;
    	return txt;
    }

}
