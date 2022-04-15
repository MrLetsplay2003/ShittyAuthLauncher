package me.mrletsplay.shittyauthlauncher;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.shittyauthlauncher.auth.AuthHelper;
import me.mrletsplay.shittyauthlauncher.auth.LoginData;
import me.mrletsplay.shittyauthlauncher.auth.MinecraftAccount;
import me.mrletsplay.shittyauthlauncher.util.GameInstallation;
import me.mrletsplay.shittyauthlauncher.util.InstallationType;
import me.mrletsplay.shittyauthlauncher.util.LaunchHelper;
import me.mrletsplay.shittyauthpatcher.util.ServerConfiguration;
import me.mrletsplay.shittyauthpatcher.version.MinecraftVersion;
import me.mrletsplay.shittyauthpatcher.version.MinecraftVersionType;

public class ShittyAuthController {

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
	private Button buttonNewInstallation;

	@FXML
	private VBox boxInstallations;

	@FXML
	private Button buttonNewAccount;

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
			boxInstallations.getChildren().removeIf(c -> !(c instanceof Button));
			for(GameInstallation inst : installationsList) {
				boxInstallations.getChildren().add(createInstallationItem(inst));
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
				vbox.getChildren().add(createInstallationItem(inst));
			}
			vbox.getChildren().add(newInst);
		};
		
		installationsList.addListener(l);
		
		dialog.getDialogPane().setContent(scroll);

		dialog.showAndWait();
		installationsList.removeListener(l);
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
	void buttonNewAccount(ActionEvent event) {
		ServerConfiguration conf = showEditServersDialog(null);
		if(conf != null) {
			MinecraftAccount acc = new MinecraftAccount(conf);
			accountsList.add(acc);
			ShittyAuthLauncherSettings.setAccounts(accountsList);
			ShittyAuthLauncherSettings.save();
		}
	}
	
	private Node createInstallationItem(GameInstallation installation) {
		try {
			URL url = ShittyAuthLauncher.class.getResource("/include/installation-item.fxml");
			if(url == null) url = new File("./include/installation-item.fxml").toURI().toURL();
	
			FXMLLoader l = new FXMLLoader(url);
			Parent pr = l.load(url.openStream());
			
			ImageView img = (ImageView) pr.lookup("#imageIcon");
			img.setImage(new Image(ShittyAuthController.class.getResourceAsStream("/include/icon.png")));
			
			Label lbl = (Label) pr.lookup("#textName");
			lbl.setText(installation.name);
			
			Button edit = (Button) pr.lookup("#buttonEdit");
			edit.setOnAction(event -> {
				showEditInstallationDialog(installation);
				installationsList.set(installationsList.indexOf(installation), installation); // Cause a list update
				dropdownInstallations.getSelectionModel().select(dropdownInstallations.getSelectionModel().getSelectedItem());
				ShittyAuthLauncherSettings.setInstallations(installationsList);
				ShittyAuthLauncherSettings.save();
			});
			
			Button delete = (Button) pr.lookup("#buttonDelete");
			if(installation.type != InstallationType.CUSTOM) delete.setDisable(true);
			delete.setOnAction(event -> {
				if(DialogHelper.showYesNo("Do you really want to delete the installation '" + installation.name + "'?\n\n"
						+ "Note: This will only remove it from the launcher, the game data folder will not be deleted")) {
					installationsList.remove(installation);
					ShittyAuthLauncherSettings.setInstallations(installationsList);
					ShittyAuthLauncherSettings.save();
				}
			});
			
			return pr;
		}catch(IOException e) {
			throw new FriendlyException(e);
		}
	}
	
	private GameInstallation showEditInstallationDialog(GameInstallation from) {
		Dialog<GameInstallation> dialog = new Dialog<>();
		dialog.initOwner(ShittyAuthLauncher.stage);
		dialog.initStyle(StageStyle.UTILITY);
		dialog.setTitle("Edit Installation");
		dialog.setHeaderText("Enter installation settings");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.FINISH, ButtonType.CANCEL);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 10, 10, 10));

		TextField name = new TextField();
		name.setPromptText("Name");
		name.setPrefWidth(300);
		if(from != null) name.setText(from.name);
		if(from != null && from.type != InstallationType.CUSTOM) name.setDisable(true);
		
		TextField directory = new TextField();
		directory.setPromptText("Directory");
		directory.setPrefWidth(300);
		if(from != null) directory.setText(from.gameDirectory);
		Button browseDir = new Button("Browse...");
		browseDir.setOnAction(event -> {
			DirectoryChooser ch = new DirectoryChooser();
			if(from != null) {
				File oldDir = new File(from.gameDirectory);	
				if(oldDir.exists()) ch.setInitialDirectory(oldDir);
			}
			File f = ch.showDialog(dialog.getDialogPane().getScene().getWindow());
			if(f != null) directory.setText(f.getAbsolutePath());
		});
		
		TextField javaPath = new TextField();
		javaPath.setPromptText("empty = default");
		javaPath.setPrefWidth(300);
		if(from != null) javaPath.setText(from.javaPath);
		Button browseJavaPath = new Button("Browse...");
		browseJavaPath.setOnAction(event -> {
			FileChooser ch = new FileChooser();
			if(from != null) {
				File oldDir = new File(from.javaPath).getParentFile();	
				if(oldDir.exists()) ch.setInitialDirectory(oldDir);
			}
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
		
		Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.FINISH);
		okButton.addEventFilter(ActionEvent.ACTION, ae -> {
			String nm = getString(name);
			String gameDir = getString(directory);
			
			if(nm == null || gameDir == null) {
				DialogHelper.showError("Both name and game directory need to be set");
				ae.consume();
			}
		});

		dialog.setResultConverter(type -> {
			if (type == ButtonType.FINISH) {
				String nm = getString(name);
				String gameDir = getString(directory);
				String java = getString(javaPath);
				
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

		return dialog.showAndWait().orElse(null);
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
		Dialog<ServerConfiguration> dialog = new Dialog<>();
		dialog.initOwner(ShittyAuthLauncher.stage);
		dialog.initStyle(StageStyle.UTILITY);
		dialog.setTitle("Edit Servers");
		dialog.setHeaderText("Enter server settings");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.FINISH, ButtonType.CANCEL);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 10, 10, 10));

		TextField authServer = new TextField();
		authServer.setPromptText("e.g. http://auth.example.com");
		authServer.setPrefWidth(300);
		if(from != null) authServer.setText(from.authServer);

		TextField accountsServer = new TextField();
		accountsServer.setPromptText("e.g. http://account.example.com");
		accountsServer.setPrefWidth(300);
		if(from != null) accountsServer.setText(from.accountsServer);

		TextField sessionServer = new TextField();
		sessionServer.setPromptText("e.g. http://session.example.com");
		sessionServer.setPrefWidth(300);
		if(from != null) sessionServer.setText(from.sessionServer);

		TextField servicesServer = new TextField();
		servicesServer.setPromptText("e.g. http://services.example.com");
		servicesServer.setPrefWidth(300);
		if(from != null) servicesServer.setText(from.servicesServer);

		TextField skinHost = new TextField();
		skinHost.setPromptText("e.g. skins.example.com");
		skinHost.setPrefWidth(300);
		if(from != null) skinHost.setText(from.skinHost);

		grid.add(new Label("Authentication server URL"), 0, 0);
		grid.add(authServer, 1, 0);
		grid.add(new Label("Accounts server URL"), 0, 1);
		grid.add(accountsServer, 1, 1);
		grid.add(new Label("Session server URL"), 0, 2);
		grid.add(sessionServer, 1, 2);
		grid.add(new Label("Services server URL"), 0, 3);
		grid.add(servicesServer, 1, 3);
		grid.add(new Label("Skin/Cape host"), 0, 4);
		grid.add(skinHost, 1, 4);

		dialog.getDialogPane().setContent(grid);
		Platform.runLater(() -> authServer.requestFocus());
		
		Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.FINISH);
		okButton.addEventFilter(ActionEvent.ACTION, ae -> {
			String authServerURL = getString(authServer);
			String accountsServerURL = getString(accountsServer);
			String sessionServerURL = getString(sessionServer);
			String servicesServerURL = getString(servicesServer);
			String skinHostname = getString(skinHost);
			
			if(authServerURL == null
					|| accountsServerURL == null
					|| sessionServerURL == null
					|| servicesServerURL == null
					|| skinHostname == null) {
				DialogHelper.showError("All server URLs need to be set");
				ae.consume();
			}
		});

		dialog.setResultConverter(type -> {
			if (type == ButtonType.FINISH) {
				String authServerURL = getString(authServer);
				String accountsServerURL = getString(accountsServer);
				String sessionServerURL = getString(sessionServer);
				String servicesServerURL = getString(servicesServer);
				String skinHostname = getString(skinHost);
				
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

			return null;
		});

		return dialog.showAndWait().orElse(null);
	}

	private void showLoginDialog(MinecraftAccount account) {
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
		
		Button loginButton = (Button) dialog.getDialogPane().lookupButton(login);
		loginButton.addEventFilter(ActionEvent.ACTION, ae -> {
			String name = getString(username);
			String pass = getString(password);
			
			if(name == null || pass == null) {
				DialogHelper.showError("Need both username and password to log in");
				ae.consume();
			}
		});

		dialog.setResultConverter(type -> {
			if (type == login) {
				String name = getString(username);
				String pass = getString(password);
				return new Pair<>(name, pass);
			}

			return null;
		});

		Optional<Pair<String, String>> creds = dialog.showAndWait();
		if (!creds.isPresent())
			return;
		Pair<String, String> p = creds.get();
		String user = p.getKey();
		String pass = p.getValue();
		
		try {
			LoginData data = AuthHelper.authenticate(user, pass, account.getServers());
			if(data != null) {
				account.setLoginData(data);
				accountsList.set(accountsList.indexOf(account), account);
				ShittyAuthLauncherSettings.setAccounts(accountsList);
				ShittyAuthLauncherSettings.save();
			}
		}catch(Exception e) {
			DialogHelper.showError("Failed to log in", e);
		}
	}
	
	private String getString(TextField textField) {
		String txt = textField.getText();
		if(txt == null || txt.isBlank()) txt = null;
		return txt;
	}
	
	public void clearLog() {
		areaLog.setText(null);
	}
	
	public void appendLog(String text) {
		areaLog.appendText(text);
	}

}
