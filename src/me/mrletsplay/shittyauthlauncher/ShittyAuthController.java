package me.mrletsplay.shittyauthlauncher;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.shittyauthlauncher.auth.AuthHelper;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;
import me.mrletsplay.shittyauthlauncher.auth.MinecraftAccount;
import me.mrletsplay.shittyauthlauncher.util.GameInstallation;
import me.mrletsplay.shittyauthlauncher.util.InstallationType;
import me.mrletsplay.shittyauthlauncher.util.LaunchHelper;
import me.mrletsplay.shittyauthlauncher.util.dialog.DialogData;
import me.mrletsplay.shittyauthlauncher.util.dialog.DialogHelper;
import me.mrletsplay.shittyauthlauncher.util.dialog.SimpleInputDialog;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;
import me.mrletsplay.shittyauthpatcher.version.MinecraftVersion;
import me.mrletsplay.shittyauthpatcher.version.MinecraftVersionType;

public class ShittyAuthController {
	
	private static final Pattern BASE64_DATA_URL = Pattern.compile("data:image/png;base64,(?<base64>.+)");

	private ObservableList<MinecraftVersion> versionsListRelease;
	private ObservableList<MinecraftVersion> versionsList;
	private ObservableList<GameInstallation> installationsList;
	private ObservableList<MinecraftAccount> accountsList;

	@FXML
	private ComboBox<MinecraftVersion> dropdownVersions;

	@FXML
	private ComboBox<GameInstallation> dropdownInstallations;

	@FXML
	private ComboBox<MinecraftAccount> dropdownAccounts;

	@FXML
	private CheckBox checkboxShowAllVersions;

	@FXML
	private TextArea areaLog;

	@FXML
	private VBox boxInstallations;

	@FXML
	private VBox boxAccounts;

	public void init() {
		versionsList = FXCollections.observableArrayList(new ArrayList<>(MinecraftVersion.VERSIONS));
		List<MinecraftVersion> releases = MinecraftVersion.VERSIONS.stream()
				.filter(v -> v.getType() == MinecraftVersionType.RELEASE)
				.collect(Collectors.toList());
		versionsListRelease = FXCollections.observableArrayList(releases);
		dropdownVersions.setItems(versionsListRelease);
		dropdownVersions.setOnAction(e -> {
			GameInstallation inst = dropdownInstallations.getSelectionModel().getSelectedItem();
			if(inst.type != InstallationType.CUSTOM) return;
			MinecraftVersion ver = dropdownVersions.getSelectionModel().getSelectedItem();
			if(ver == null) return;
			inst.lastVersionId = ver.getId();
			ShittyAuthLauncherSettings.setInstallations(installationsList);
			ShittyAuthLauncherSettings.save();
		});
		
		installationsList = FXCollections.observableArrayList();
		installationsList.addListener((ListChangeListener<GameInstallation>) v -> {
			boxInstallations.getChildren().clear();
			for(GameInstallation inst : installationsList) {
				boxInstallations.getChildren().add(createInstallationItem(inst, false));
			}
		});
		
		installationsList.addAll(ShittyAuthLauncherSettings.getInstallations());
		dropdownInstallations.setItems(installationsList);
		GameInstallation i = ShittyAuthLauncherSettings.getActiveInstallation();
		if(i == null) i = ShittyAuthLauncherSettings.getInstallations().get(0);
		dropdownInstallations.getSelectionModel().select(i);
		selectInstallation(i);
		dropdownInstallations.setOnAction(e -> {
			GameInstallation inst = dropdownInstallations.getSelectionModel().getSelectedItem();
			selectInstallation(inst);
			ShittyAuthLauncherSettings.setActiveInstallation(inst);
			ShittyAuthLauncherSettings.save();
		});
		
		accountsList = FXCollections.observableArrayList();
		accountsList.addListener((ListChangeListener<MinecraftAccount>) v -> {
			boxAccounts.getChildren().removeIf(c -> !(c instanceof Button));
			for(MinecraftAccount acc : accountsList) {
				boxAccounts.getChildren().add(createAccountItem(acc));
			}
		});
		
		accountsList.addAll(ShittyAuthLauncherSettings.getAccounts());
		dropdownAccounts.setItems(accountsList);
		dropdownAccounts.getSelectionModel().select(ShittyAuthLauncherSettings.getActiveAccount());
		dropdownAccounts.setOnAction(event -> {
			MinecraftAccount acc = dropdownAccounts.getSelectionModel().getSelectedItem();
			ShittyAuthLauncherSettings.setActiveAccount(acc);
		});
	}
	
	private void selectInstallation(GameInstallation inst) {
		MinecraftVersion ver;
		switch(inst.type) {
			default:
			case LATEST_RELEASE:
				ver = MinecraftVersion.LATEST_RELEASE;
				dropdownVersions.setDisable(true);
				break;
			case LATEST_SNAPSHOT:
				ver = MinecraftVersion.LATEST_SNAPSHOT;
				dropdownVersions.setDisable(true);
				break;
			case CUSTOM:
				ver = MinecraftVersion.getVersion(inst.lastVersionId);
				dropdownVersions.setDisable(false);
				break;
		};
		
		if(!dropdownVersions.getItems().contains(ver)) {
			checkboxShowAllVersions.setSelected(true);
			dropdownVersions.setItems(versionsList);
		}
		dropdownVersions.getSelectionModel().select(ver);
	}

	@FXML
	void buttonPlay(ActionEvent event) {
		MinecraftVersion ver = dropdownVersions.getSelectionModel().getSelectedItem();
		if(ver == null) {
			DialogHelper.showError("No version selected");
			return;
		}
		
		MinecraftAccount acc = dropdownAccounts.getSelectionModel().getSelectedItem();
		if(acc == null || !acc.isLoggedIn()) {
			DialogHelper.showWarning("You need to log in first");
			return;
		}
		
		GameInstallation inst = dropdownInstallations.getSelectionModel().getSelectedItem();
		if(inst == null) {
			DialogHelper.showError("No installation selected");
			return;
		}
		
		LaunchHelper.launch(ver, acc, dropdownInstallations.getSelectionModel().getSelectedItem());
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
	void buttonNewInstallation(ActionEvent event) {
		GameInstallation inst = showEditInstallationDialog(null);
		if(inst != null) {
			installationsList.add(inst);
			ShittyAuthLauncherSettings.setInstallations(installationsList);
			ShittyAuthLauncherSettings.save();
		}
	}

	@FXML
	void buttonImportInstallation(ActionEvent event) {
		DialogData data = new SimpleInputDialog()
				.addFile("profiles", "Path to launcher_profiles.json", "Path to launcher_profiles.json", new File(ShittyAuthLauncherSettings.DEFAULT_MINECRAFT_PATH + "/launcher_profiles.json"))
				.setVerifier(d -> d.get("profiles") == null ? "Need path to launcher_profiles.json" : null)
				.show("Profiles path", "Where is launcher_profiles.json located?");
		
		if(data == null) return;
		
		File profiles = data.get("profiles");
		if(!profiles.exists()) {
			DialogHelper.showError("The selected file does not exist");
			return;
		}
		
		JSONObject p;
		try {
			p = new JSONObject(Files.readString(profiles.toPath(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			DialogHelper.showError("Failed to read file", e);
			return;
		}
		
		List<GameInstallation> installationsToImport = new ArrayList<>();
		
		JSONObject pr = p.getJSONObject("profiles");
		for(String k : pr.keySet()) {
			JSONObject profile = pr.getJSONObject(k);
			if(!profile.getString("type").equals("custom")) continue;
			
			String ver = profile.getString("lastVersionId");
			MinecraftVersion version = MinecraftVersion.getVersion(ver);
			if(version == null) continue; // Unknown version (e.g. modded, not yet supported)
			
			String icon = profile.optString("icon").orElse(null);
			System.out.println(icon);
			if(icon != null) {
				Matcher m = BASE64_DATA_URL.matcher(icon);
				if(!m.matches()) {
					icon = null;
				}else {
					icon = m.group("base64");
				}
			}
			
			GameInstallation inst = new GameInstallation(
					InstallationType.CUSTOM,
					UUID.randomUUID().toString(),
					profile.getString("name"),
					icon,
					profile.optString("gameDir").orElse(profiles.getParentFile().getAbsolutePath()),
					profile.optString("javaDir").orElse(null),
					ver);
			installationsToImport.add(inst);
		}
		
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.initOwner(ShittyAuthLauncher.stage);
		dialog.initStyle(StageStyle.UTILITY);
		dialog.setTitle("Import Installations");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
		
		VBox vbox = new VBox(10);
		vbox.setMaxWidth(Double.MAX_VALUE);
		vbox.setMaxHeight(Double.MAX_VALUE);
		vbox.setFillWidth(true);
		
		ScrollPane scroll = new ScrollPane(vbox);
		scroll.setPrefWidth(600);
		scroll.setPrefHeight(300);
		scroll.setMaxWidth(Double.MAX_VALUE);
		scroll.setMaxHeight(Double.MAX_VALUE);
		scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
		scroll.setFitToWidth(true);
		
		for(GameInstallation inst : installationsToImport) {
			vbox.getChildren().add(createInstallationItem(inst, true));
		}
		
		dialog.getDialogPane().setContent(scroll);
		dialog.showAndWait();
	}

	@FXML
	void buttonNewAccount(ActionEvent event) {
		ServerConfiguration conf = showEditServersDialog(null);
		if(conf != null) {
			MinecraftAccount acc = new MinecraftAccount(conf);
			accountsList.add(acc);
			ShittyAuthLauncherSettings.setAccounts(accountsList);
			ShittyAuthLauncherSettings.save();
		}
	}
	
	private Node createInstallationItem(GameInstallation installation, boolean isImport) {
		try {
			URL url = ShittyAuthLauncher.class.getResource("/include/installation-item.fxml");
			if(url == null) url = new File("./include/installation-item.fxml").toURI().toURL();
	
			FXMLLoader l = new FXMLLoader(url);
			Parent pr = l.load(url.openStream());
			
			ImageView img = (ImageView) pr.lookup("#imageIcon");
			String data = installation.imageData;
			if(data == null) data = GameInstallation.DEFAULT_IMAGE_DATA;
			byte[] bytes = Base64.getDecoder().decode(data);
			img.setImage(new Image(new ByteArrayInputStream(bytes)));
			
			Label name = (Label) pr.lookup("#textName");
			name.setText(installation.name);
			
			Label gameDir = (Label) pr.lookup("#textGameDir");
			gameDir.setText("Game Directory: " + installation.gameDirectory);
			
			Label version = (Label) pr.lookup("#textVersion");
			String ver = installation.lastVersionId;
			switch(installation.type) {
				case LATEST_RELEASE:
					ver = MinecraftVersion.LATEST_RELEASE.getId();
					break;
				case LATEST_SNAPSHOT:
					ver = MinecraftVersion.LATEST_SNAPSHOT.getId();
					break;
				default:
					break;
			}
			version.setText("Version: " + ver);
			
			VBox buttons = (VBox) pr.lookup("#boxButtons");
			
			if(!isImport) {
				Button edit = new Button("Edit");
				edit.setMaxWidth(Double.MAX_VALUE);
				edit.setOnAction(event -> {
					showEditInstallationDialog(installation);
					installationsList.set(installationsList.indexOf(installation), installation); // Cause a list update
					dropdownInstallations.getSelectionModel().select(dropdownInstallations.getSelectionModel().getSelectedItem());
					ShittyAuthLauncherSettings.setInstallations(installationsList);
					ShittyAuthLauncherSettings.save();
				});
				buttons.getChildren().add(edit);
	
				Button delete = new Button("Delete");
				delete.setMaxWidth(Double.MAX_VALUE);
				if(installation.type != InstallationType.CUSTOM) delete.setDisable(true);
				delete.setOnAction(event -> {
					if(DialogHelper.showYesNo("Do you really want to delete the installation '" + installation.name + "'?\n\n"
							+ "Note: This will only remove it from the launcher, the game data folder will not be deleted")) {
						installationsList.remove(installation);
						ShittyAuthLauncherSettings.setInstallations(installationsList);
						ShittyAuthLauncherSettings.save();
					}
				});
				buttons.getChildren().add(delete);
			}else {
				Button importBtn = new Button("Import");
				importBtn.setMaxWidth(Double.MAX_VALUE);
				importBtn.setOnAction(event -> {
					installationsList.add(installation);
					importBtn.setText("Imported");
					importBtn.setDisable(true);
				});
				buttons.getChildren().add(importBtn);
			}
			
			return pr;
		}catch(IOException e) {
			throw new FriendlyException(e);
		}
	}
	
	private GameInstallation showEditInstallationDialog(GameInstallation from) {
		SimpleInputDialog dialog = new SimpleInputDialog()
			.addString("name", "Installation Name", "Name", from != null ? from.name : null)
			.addDirectory("directory", "Directory", "Directory", from != null ? new File(from.gameDirectory) : null)
			.addFile("java", "Java Path", "empty = default", from != null && from.javaPath != null ? new File(from.javaPath) : null)
			.setVerifier(d -> {
				if(d.get("name") == null) return "Name may not be empty";
				if(d.get("directory") == null) return "Directory may not be empty";
				return null;
			});
		
		if(from != null && from.type != InstallationType.CUSTOM) dialog.disable("name");
		
		DialogData data = dialog.show("Edit Installation", "Enter installation settings");
		if(data == null) return null;
		
		if(from != null) {
			from.name = data.get("name");
			from.gameDirectory = data.<File>get("directory").getAbsolutePath();
			File java = data.get("java");
			from.javaPath = java == null ? null : java.getAbsolutePath();
			return from;
		}else {
			GameInstallation inst = new GameInstallation();
			inst.name = data.get("name");
			inst.gameDirectory = data.<File>get("directory").getAbsolutePath();
			File java = data.get("java");
			inst.javaPath = java == null ? null : java.getAbsolutePath();
			inst.lastVersionId = MinecraftVersion.LATEST_RELEASE.getId();
			return inst;
		}
	}
	
	private Node createAccountItem(MinecraftAccount account) {
		try {
			URL url = ShittyAuthLauncher.class.getResource("/include/account-item.fxml");
			if(url == null) url = new File("./include/account-item.fxml").toURI().toURL();
	
			FXMLLoader l = new FXMLLoader(url);
			Parent pr = l.load(url.openStream());
			
			ImageView img = (ImageView) pr.lookup("#imageIcon");
			img.setImage(new Image(ShittyAuthController.class.getResourceAsStream("/include/icon.png")));
			
			Label lbl = (Label) pr.lookup("#textName");
			lbl.setText(account.getLoginData() != null ? account.getLoginData().getUsername() : "Not Logged In");
			
			Button switchAccount = (Button) pr.lookup("#buttonLogin");
			switchAccount.setOnAction(event -> showLoginDialog(account));
			
			Button edit = (Button) pr.lookup("#buttonEdit");
			edit.setOnAction(event -> {
				showEditServersDialog(account.getServers());
				accountsList.set(accountsList.indexOf(account), account); // Cause a list update
				ShittyAuthLauncherSettings.setAccounts(accountsList);
				ShittyAuthLauncherSettings.save();
			});
			
			Button delete = (Button) pr.lookup("#buttonDelete");
			delete.setOnAction(event -> {
				if(DialogHelper.showYesNo("Do you really want to delete the account '" + account.toString() + "'?")) {
					accountsList.remove(account);
					ShittyAuthLauncherSettings.setAccounts(accountsList);
					ShittyAuthLauncherSettings.save();
				}
			});
			
			return pr;
		}catch(IOException e) {
			throw new FriendlyException(e);
		}
	}
	
	private ServerConfiguration showEditServersDialog(ServerConfiguration from) {
		DialogData data = new SimpleInputDialog()
			.addString("auth", "Authentication server URL", "e.g. http://auth.example.com", from != null ? from.authServer : null)
			.addString("accounts", "Accounts server URL", "e.g. http://account.example.com", from != null ? from.accountsServer : null)
			.addString("session", "Session server URL", "e.g. http://session.example.com", from != null ? from.sessionServer : null)
			.addString("services", "Services server URL", "e.g. http://services.example.com", from != null ? from.servicesServer : null)
			.addString("skin", "Skin/Cape host", "e.g. skins.example.com", from != null ? from.skinHost : null)
			.setVerifier(d -> d.get("auth") == null
						|| d.get("accounts") == null
						|| d.get("session") == null
						|| d.get("services") == null
						|| d.get("skin") == null ?
								"All servers must be set" : null)
			.show("Edit Servers", "Enter server settings");
		
		if(data == null) return null;

		String authServerURL = data.get("auth");
		if(authServerURL.endsWith("/")) authServerURL = authServerURL.substring(0, authServerURL.length() - 1);
		String accountsServerURL = data.get("accounts");
		if(accountsServerURL.endsWith("/")) accountsServerURL = accountsServerURL.substring(0, accountsServerURL.length() - 1);
		String sessionServerURL = data.get("session");
		if(sessionServerURL.endsWith("/")) sessionServerURL = sessionServerURL.substring(0, sessionServerURL.length() - 1);
		String servicesServerURL = data.get("services");
		if(servicesServerURL.endsWith("/")) servicesServerURL = servicesServerURL.substring(0, servicesServerURL.length() - 1);
		String skinHostname = data.get("skin");
		if(from != null) {
			from.authServer = authServerURL;
			from.accountsServer = accountsServerURL;
			from.sessionServer = sessionServerURL;
			from.servicesServer = servicesServerURL;
			from.skinHost = skinHostname;
			return from;
		}else {
			ServerConfiguration servers = new ServerConfiguration();
			servers.authServer = authServerURL;
			servers.accountsServer = accountsServerURL;
			servers.sessionServer = sessionServerURL;
			servers.servicesServer = servicesServerURL;
			servers.skinHost = skinHostname;
			return servers;
		}
	}

	private void showLoginDialog(MinecraftAccount account) {
		ButtonType loginButton = new ButtonType("Login", ButtonData.OK_DONE);
		DialogData data = new SimpleInputDialog()
				.addString("username", "Username", "Username")
				.addString("password", "Password", "Password")
				.setVerifier(d -> {
					return d.get("username") == null || d.get("password") == null ?
							"Need both username and password to log in" : null;
				})
				.setConfirmButton(loginButton)
				.show("Login", "Enter your credentials");
		
		if(data == null) return;
		
		try {
			String user = data.get("username");
			String pass = data.get("password");
			LoginData login = AuthHelper.authenticate(user, pass, account.getServers());
			if(login != null) {
				account.setLoginData(login);
				accountsList.set(accountsList.indexOf(account), account);
				ShittyAuthLauncherSettings.setAccounts(accountsList);
				ShittyAuthLauncherSettings.save();
			}
		}catch(Exception e) {
			DialogHelper.showError("Failed to log in", e);
		}
	}
	
	public void clearLog() {
		areaLog.setText(null);
	}
	
	public void appendLog(String text) {
		areaLog.appendText(text);
	}

}
