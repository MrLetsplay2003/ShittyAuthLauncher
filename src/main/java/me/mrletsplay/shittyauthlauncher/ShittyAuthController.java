package me.mrletsplay.shittyauthlauncher;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import me.mrletsplay.mrcore.http.HttpException;
import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.shittyauthlauncher.auth.AuthHelper;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;
import me.mrletsplay.shittyauthlauncher.auth.MinecraftAccount;
import me.mrletsplay.shittyauthlauncher.util.GameInstallation;
import me.mrletsplay.shittyauthlauncher.util.InstallationType;
import me.mrletsplay.shittyauthlauncher.util.LaunchHelper;
import me.mrletsplay.shittyauthlauncher.util.ProfilesHelper;
import me.mrletsplay.shittyauthlauncher.util.SkinHelper;
import me.mrletsplay.shittyauthlauncher.util.dialog.DialogData;
import me.mrletsplay.shittyauthlauncher.util.dialog.DialogHelper;
import me.mrletsplay.shittyauthlauncher.util.dialog.SimpleInputDialog;
import me.mrletsplay.shittyauthpatcher.mirrors.DownloadsMirror;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;
import me.mrletsplay.shittyauthpatcher.version.AbstractMinecraftVersion;
import me.mrletsplay.shittyauthpatcher.version.MinecraftVersionType;

public class ShittyAuthController {

	public static ObservableList<AbstractMinecraftVersion> versionsList;
	public static FilteredList<AbstractMinecraftVersion> versionsListRelease;
	public static ObservableList<GameInstallation> installationsList;
	public static ObservableList<MinecraftAccount> accountsList;
	public static ObservableList<DownloadsMirror> mirrors;

	@FXML
	private ComboBox<AbstractMinecraftVersion> dropdownVersions;

	@FXML
	private ComboBox<GameInstallation> dropdownInstallations;

	@FXML
	private ComboBox<MinecraftAccount> dropdownAccounts;

	@FXML
	private CheckBox checkboxShowAllVersions;

	@FXML
	private Button buttonPlay;

	@FXML
	private TextArea areaLog;

	@FXML
	private VBox boxInstallations;

	@FXML
	private VBox boxAccounts;

	@FXML
	private VBox boxMirrors;

	@FXML
	private TabPane tabPaneAll;

	public void init() {
		importInstallationsFromJSON();

		installationsList = FXCollections.observableArrayList();
		installationsList.addListener((ListChangeListener<GameInstallation>) v -> {
			boxInstallations.getChildren().clear();
			for(GameInstallation inst : installationsList) {
				boxInstallations.getChildren().add(createInstallationItem(inst, false));
			}
		});
		installationsList.addAll(ShittyAuthLauncherSettings.getInstallations());

		versionsList = FXCollections.observableArrayList();
		versionsListRelease = new FilteredList<>(versionsList, v -> v.getType() == MinecraftVersionType.RELEASE);

		dropdownInstallations.setItems(installationsList);
		dropdownInstallations.setOnAction(e -> {
			GameInstallation inst = dropdownInstallations.getSelectionModel().getSelectedItem();
			selectInstallation(inst);
			ShittyAuthLauncherSettings.setActiveInstallation(inst);
			ShittyAuthLauncherSettings.save();
		});

		dropdownVersions.setItems(versionsListRelease);
		dropdownVersions.setDisable(false);
		loadAllVersions();

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

		dropdownVersions.setOnAction(e -> {
			GameInstallation inst = dropdownInstallations.getSelectionModel().getSelectedItem();
			if(inst.type != InstallationType.CUSTOM) return;
			AbstractMinecraftVersion ver = dropdownVersions.getSelectionModel().getSelectedItem();
			if(ver == null) return;
			inst.lastVersionId = ver.getId();
			ShittyAuthLauncherSettings.setInstallations(installationsList);
			ShittyAuthLauncherSettings.save();
		});

		mirrors = FXCollections.observableArrayList();
		mirrors.addListener((ListChangeListener<DownloadsMirror>) v -> {
			boxMirrors.getChildren().clear();
			for(DownloadsMirror mirror : mirrors) {
				boxMirrors.getChildren().add(createMirrorItem(mirror));
			}
		});
		mirrors.addAll(ShittyAuthLauncherSettings.getMirrors());

		GameInstallation i = ShittyAuthLauncherSettings.getActiveInstallation();
		if(i == null) i = ShittyAuthLauncherSettings.getInstallations().get(0);
		dropdownInstallations.getSelectionModel().select(i);
		selectInstallation(i);
	}

	public void addTab(String name, Node content) {
		Tab t = new Tab(name);
		tabPaneAll.getTabs().add(t);
		t.setContent(content);
	}

	private void selectInstallation(GameInstallation inst) {
		versionsList.clear();
		versionsList.addAll(inst.getVersions().getVersions());

		AbstractMinecraftVersion ver;
		switch(inst.type) {
			default:
			case LATEST_RELEASE:
				ver = inst.getVersions().getLatestRelease();
				dropdownVersions.setDisable(true);
				break;
			case LATEST_SNAPSHOT:
				ver = inst.getVersions().getLatestSnapshot();
				dropdownVersions.setDisable(true);
				break;
			case CUSTOM:
				ver = inst.getVersions().getVersion(inst.lastVersionId);
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
		AbstractMinecraftVersion ver = dropdownVersions.getSelectionModel().getSelectedItem();
		if(ver == null) {
			DialogHelper.showError("No version selected");
			return;
		}

		MinecraftAccount acc = dropdownAccounts.getSelectionModel().getSelectedItem();
		if(acc == null) {
			DialogHelper.showWarning("You need to create an account first");
			return;
		}

		try {
			if(acc.getLoginData() != null && !AuthHelper.validate(acc.getLoginData(), acc.getServers())) {
				acc.setLoginData(AuthHelper.refresh(acc.getLoginData(), acc.getServers()));
			}
		}catch(HttpException e) {
			DialogHelper.showError("Failed to validate/refresh account data", e);
			return;
		}

		if(!acc.isLoggedIn()) {
			showLoginDialog(acc);
			if(!acc.isLoggedIn()) return;
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

		List<GameInstallation> installationsToImport = ProfilesHelper.loadInstallations(profiles);

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
		scroll.getStyleClass().add("content-scroll");
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
		ServerConfiguration conf;
		if(ShittyAuthLauncherPlugins.getDefaultsProvider().allowCustomServerConfigurations()) {
			conf = showEditServersDialog(null);
		}else {
			ServerConfiguration def = ShittyAuthLauncherPlugins.getDefaultsProvider().getDefaultServerConfiguration();
			if(def == null) throw new FriendlyException("Illegal defaults provider: Default server configuration may not be null when not allowing custom server configurations");
			conf = new ServerConfiguration(def.authServer, def.accountsServer, def.sessionServer, def.servicesServer, def.skinHost);
		}

		if(conf != null) {
			MinecraftAccount acc = new MinecraftAccount(conf);
			accountsList.add(acc);
			ShittyAuthLauncherSettings.setAccounts(accountsList);
			ShittyAuthLauncherSettings.save();
		}
	}

	@FXML
	void buttonNewMirror(ActionEvent event) {
		DownloadsMirror mirror = showEditMirrorDialog(null);
		if(mirror != null) {
			mirrors.add(mirror);
			ShittyAuthLauncherSettings.setMirrors(mirrors);
		}
	}

	private Node createInstallationItem(GameInstallation installation, boolean isImport) {
		try {
			URL url = ShittyAuthLauncher.class.getResource("/include/ui/installation-item.fxml");

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
					ver = installation.getVersions().getLatestRelease().getId();
					break;
				case LATEST_SNAPSHOT:
					ver = installation.getVersions().getLatestSnapshot().getId();
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
					if(dropdownInstallations.getValue() == installation) selectInstallation(installation);
				});
				buttons.getChildren().add(edit);

				Button clone = new Button("Clone");
				clone.setMaxWidth(Double.MAX_VALUE);
				clone.setOnAction(event -> {
					GameInstallation inst = showEditInstallationDialog(null, installation);
					if(inst != null) {
						installationsList.add(inst);
						ShittyAuthLauncherSettings.setInstallations(installationsList);
						ShittyAuthLauncherSettings.save();
					}
				});
				buttons.getChildren().add(clone);

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
					if(!Path.of(installation.gameDirectory).equals(Path.of(ShittyAuthLauncherSettings.DEFAULT_MINECRAFT_PATH))) {
						DialogHelper.showWarning("This installation does not use the default .minecraft directory. If it contains modded versions, they need to be manually re-installed into the directory to appear");
					}

					installationsList.add(installation);
					ShittyAuthLauncherSettings.setInstallations(installationsList);
					ShittyAuthLauncherSettings.save();
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

	private GameInstallation showEditInstallationDialog(GameInstallation from, GameInstallation initWith) {
		SimpleInputDialog dialog = new SimpleInputDialog()
			.addString("name", "Installation Name", "Name", initWith != null ? initWith.name : null)
			.addDirectory("directory", "Directory", "Directory", initWith != null ? new File(initWith.gameDirectory) : null)
			.addFile("java", "Java Path", "empty = default", initWith != null && initWith.javaPath != null ? new File(initWith.javaPath) : null)
			.addString("jvmArgs", "JVM arguments", "empty = none", initWith != null ? Optional.ofNullable(initWith.jvmArgs).map(a -> a.stream().collect(Collectors.joining(" "))).orElse("") : "")
			.addChoice("mirror", "Mirror", initWith == null ? ShittyAuthLauncherPlugins.getDefaultsProvider().getDefaultMirror() : initWith.getMirror(), ShittyAuthLauncherSettings.getMirrors())
			.setVerifier(d -> {
				if(d.get("name") == null) return "Name may not be empty";
				if(d.get("directory") == null) return "Directory may not be empty";
				return null;
			});

		if(from != null && from.type != InstallationType.CUSTOM) dialog.disable("name");

		DialogData data = dialog.show("Edit Installation", "Enter installation settings");
		if(data == null) return null;

		if(from == null) from = new GameInstallation();
		from.name = data.get("name");
		from.gameDirectory = data.<File>get("directory").getAbsolutePath();
		File java = data.get("java");
		from.javaPath = java == null ? null : java.getAbsolutePath();
		String jvmArgs = data.get("jvmArgs");
		from.jvmArgs = jvmArgs == null ? Collections.emptyList() : Arrays.asList(jvmArgs.split(" "));
		if(from.lastVersionId == null) from.lastVersionId = from.getVersions().getLatestRelease().getId();
		from.mirror = data.<DownloadsMirror>get("mirror").getName();
		from.updateVersions();
		if(from == dropdownInstallations.getValue()) versionsList.setAll(from.getVersions().getVersions());
		return from;
	}

	private GameInstallation showEditInstallationDialog(GameInstallation from) {
		return showEditInstallationDialog(from, from);
	}

	private Node createAccountItem(MinecraftAccount account) {
		try {
			URL url = ShittyAuthLauncher.class.getResource("/include/ui/account-item.fxml");

			FXMLLoader l = new FXMLLoader(url);
			Parent pr = l.load(url.openStream());

			ImageView img = (ImageView) pr.lookup("#imageIcon");
			img.setImage(new Image(ShittyAuthLauncherPlugins.getIconProvider().loadDefaultAccountIcon()));

			BufferedImage accHead = SkinHelper.getSkinHead(account);
			if(accHead != null) {
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				ImageIO.write(accHead, "PNG", bOut);
				img.setImage(new Image(new ByteArrayInputStream(bOut.toByteArray())));
			}

			Label lbl = (Label) pr.lookup("#textName");
			lbl.setText(account.getLoginData() != null ? account.getLoginData().getUsername() : "Not Logged In");

			Button switchAccount = (Button) pr.lookup("#buttonLogin");
			switchAccount.setOnAction(event -> showLoginDialog(account));

			Button edit = (Button) pr.lookup("#buttonEdit");
			edit.setDisable(!ShittyAuthLauncherPlugins.getDefaultsProvider().allowCustomServerConfigurations());
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
		boolean custom = true;
		if(from == null) {
			ServerConfiguration def = ShittyAuthLauncherPlugins.getDefaultsProvider().getDefaultServerConfiguration();
			List<String> choices = new ArrayList<>();
			choices.add("ShittyAuth");
			choices.add("Custom");
			if(def != null) choices.add("Default");
			int ch = DialogHelper.showChoice("Server Setup", "Choose your setup type", choices.toArray(String[]::new));
			if(ch == 2) return new ServerConfiguration(def.authServer, def.accountsServer, def.sessionServer, def.servicesServer, def.skinHost);
			if(ch == 0) custom = false;
		}

		DialogData data;

		if(custom) {
			data = new SimpleInputDialog()
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
		}else {
			data = new SimpleInputDialog()
				.addString("auth", "ShittyAuthServer URL", "e.g. http://shittyauth.example.com")
				.show("Edit Servers", "Enter server settings");
		}

		if(data == null) return null;

		String authServerURL = data.get("auth");
		if(authServerURL.endsWith("/")) authServerURL = authServerURL.substring(0, authServerURL.length() - 1);
		String accountsServerURL = data.get("accounts", authServerURL);
		if(accountsServerURL.endsWith("/")) accountsServerURL = accountsServerURL.substring(0, accountsServerURL.length() - 1);
		String sessionServerURL = data.get("session", authServerURL);
		if(sessionServerURL.endsWith("/")) sessionServerURL = sessionServerURL.substring(0, sessionServerURL.length() - 1);
		String servicesServerURL = data.get("services", authServerURL);
		if(servicesServerURL.endsWith("/")) servicesServerURL = servicesServerURL.substring(0, servicesServerURL.length() - 1);
		String skinHostname = data.get("skin", URI.create(authServerURL).getHost());

		if(from == null) from = new ServerConfiguration();
		from.authServer = authServerURL;
		from.accountsServer = accountsServerURL;
		from.sessionServer = sessionServerURL;
		from.servicesServer = servicesServerURL;
		from.skinHost = skinHostname;
		return from;
	}

	private void showLoginDialog(MinecraftAccount account) {
		ButtonType loginButton = new ButtonType("Login", ButtonData.OK_DONE);
		DialogData data = new SimpleInputDialog()
				.addString("username", "Username", "Username")
				.addPassword("password", "Password", "Password")
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
			}else {
				DialogHelper.showError("Invalid credentials");
			}
		}catch(Exception e) {
			DialogHelper.showError("Failed to log in", e);
		}
	}

	private DownloadsMirror showEditMirrorDialog(DownloadsMirror from) {
		DownloadsMirror defaultMirror = ShittyAuthLauncherPlugins.getDefaultsProvider().getDefaultMirror();
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
			URL url = ShittyAuthLauncher.class.getResource("/include/ui/mirror-item.fxml");

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

			if(ShittyAuthLauncherPlugins.getMirrors().contains(mirror)){
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

	public void clearLog() {
		areaLog.setText(null);
	}

	public void appendLog(String text) {
		areaLog.appendText(text);
	}

	private void loadAllVersions() {
		ShittyAuthLauncher.LOGGER.info("Loading versions...");
		for(GameInstallation inst : ShittyAuthLauncherSettings.getInstallations()) {
			inst.updateVersions();
		}
	}

	// Provides compatibility for installers such as Forge
	private void importInstallationsFromJSON() {
		ShittyAuthLauncher.LOGGER.info("Loading installations from launcher_profiles.json...");
		List<GameInstallation> oldInsts = ShittyAuthLauncherSettings.getInstallations();
		List<GameInstallation> insts = new ArrayList<>();
		for(GameInstallation inst : oldInsts) {
			ShittyAuthLauncher.LOGGER.info("Loading installations for '" + inst.name + "'...");
			File profilesJSON = new File(inst.gameDirectory, "launcher_profiles.json");
			if(profilesJSON.exists()) {
				JSONObject obj;
				try {
					obj = new JSONObject(Files.readString(profilesJSON.toPath()));
					if(!obj.has("shittyauth_autoImport")) {
						ShittyAuthLauncher.LOGGER.info("Ignoring " + profilesJSON.getAbsolutePath() + ", because it doesn't have the 'shittyauth_autoImport' key set");
						continue;
					}
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}

				JSONObject profiles = obj.getJSONObject("profiles");
				List<GameInstallation> installations = ProfilesHelper.loadInstallations(profilesJSON);
				for(GameInstallation newInst : installations) {
					if(oldInsts.stream().anyMatch(i -> i.id.equals(newInst.id))) {
						profiles.remove(newInst.id);
						continue;
					}
					newInst.updateVersions();

//					DefaultMinecraftVersion ver = DefaultMinecraftVersion.getVersion(newInst.lastVersionId);
//					if(ver == null || (ver.isImported() && getImportedVersion(inst.id, newInst.lastVersionId) == null)) {
//						System.out.println("Installation '" + newInst.name + "' in launcher_profiles.json references invalid version '" + newInst.lastVersionId + "', not importing");
//						continue;
//					} TODO

					insts.add(newInst);
					profiles.remove(newInst.id);
				}

				try {
					Files.writeString(profilesJSON.toPath(), obj.toFancyString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else {
				try {
					JSONObject obj = new JSONObject();
					obj.put("shittyauth_autoImport", true);
					obj.put("profiles", new JSONObject());
					IOUtils.createFile(profilesJSON);
					Files.writeString(profilesJSON.toPath(), obj.toFancyString());
				} catch (IOException e) {
					System.err.println("Failed to write file");
					e.printStackTrace();
					continue;
				}
			}
		}

		oldInsts.addAll(insts);
		ShittyAuthLauncherSettings.setInstallations(oldInsts);
		ShittyAuthLauncherSettings.save();
	}

}
