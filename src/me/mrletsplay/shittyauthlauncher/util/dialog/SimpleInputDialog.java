package me.mrletsplay.shittyauthlauncher.util.dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;

public class SimpleInputDialog {
	
	private List<DialogElement> elements;
	private List<String> disabled;
	private Function<DialogData, String> verifier; // Data -> Error message (null = success)
	private ButtonType confirmButton;
	
	public SimpleInputDialog() {
		this.elements = new ArrayList<>();
		this.disabled = new ArrayList<>();
		this.verifier = d -> null;
		this.confirmButton = ButtonType.FINISH;
	}
	
	private void addInput(DialogInputType type, String id, String name, String prompt, Object initialValue) {
		elements.add(new DialogElement(type, id, name, prompt, initialValue));
	}
	
	public SimpleInputDialog addString(String id, String name, String prompt) {
		addInput(DialogInputType.STRING, id, name, prompt, null);
		return this;
	}
	
	public SimpleInputDialog addString(String id, String name, String prompt, String initialValue) {
		addInput(DialogInputType.STRING, id, name, prompt, initialValue);
		return this;
	}
	
	public SimpleInputDialog addBoolean(String id, String name) {
		addInput(DialogInputType.BOOLEAN, id, name, null, false);
		return this;
	}
	
	public SimpleInputDialog addBoolean(String id, String name, boolean initialValue) {
		addInput(DialogInputType.BOOLEAN, id, name, null, initialValue);
		return this;
	}
	
	public SimpleInputDialog addFile(String id, String name, String prompt) {
		addInput(DialogInputType.FILE, id, name, prompt, null);
		return this;
	}
	
	public SimpleInputDialog addFile(String id, String name, String prompt, File initialValue) {
		addInput(DialogInputType.FILE, id, name, prompt, initialValue);
		return this;
	}
	
	public SimpleInputDialog addDirectory(String id, String name, String prompt) {
		addInput(DialogInputType.DIRECTORY, id, name, prompt, null);
		return this;
	}
	
	public SimpleInputDialog addDirectory(String id, String name, String prompt, File initialValue) {
		addInput(DialogInputType.DIRECTORY, id, name, prompt, initialValue);
		return this;
	}
	
	public SimpleInputDialog setVerifier(Function<DialogData, String> verifier) {
		this.verifier = verifier;
		return this;
	}
	
	public SimpleInputDialog setConfirmButton(ButtonType confirmButton) {
		this.confirmButton = confirmButton;
		return this;
	}
	
	public SimpleInputDialog disable(String elementID) {
		disabled.add(elementID);
		return this;
	}
	
	public DialogData show(String title, String header) {
		Dialog<DialogData> dialog = new Dialog<>();
		dialog.initOwner(ShittyAuthLauncher.stage);
		dialog.initStyle(StageStyle.UTILITY);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);
		
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setMinWidth(GridPane.USE_PREF_SIZE);
		grid.setMinHeight(GridPane.USE_PREF_SIZE);
		grid.setAlignment(Pos.CENTER);
		grid.setPadding(new Insets(20, 10, 10, 10));

		Map<String, Supplier<Object>> nodeValueFunctions = new HashMap<>();
		int row = 0;
		for(DialogElement e : elements) {
			grid.add(new Label(e.getName()), 0, row);
			switch(e.getType()) {
				case BOOLEAN:
				{
					CheckBox cb = new CheckBox();
					if(e.getInitialValue() != null) cb.setSelected((boolean) e.getInitialValue());
					if(disabled.contains(e.getID())) cb.setDisable(true);
					nodeValueFunctions.put(e.getID(), () -> cb.isSelected());
					grid.add(cb, 1, row);
					break;
				}
				case DIRECTORY:
				{
					TextField directory = new TextField();
					directory.setPromptText(e.getPrompt());
					directory.setPrefWidth(300);
					GridPane.setHgrow(directory, Priority.ALWAYS);
					directory.setMaxWidth(Double.MAX_VALUE);
					if(e.getInitialValue() != null) directory.setText(((File) e.getInitialValue()).getAbsolutePath());
					if(disabled.contains(e.getID())) directory.setDisable(true);
					nodeValueFunctions.put(e.getID(), () -> {
						String path = getString(directory);
						if(path == null) return null;
						return new File(path);
					});
					Button browseDir = new Button("Browse...");
					browseDir.setOnAction(event -> {
						DirectoryChooser ch = new DirectoryChooser();
						if(e.getInitialValue() != null) {
							File initialDir = (File) e.getInitialValue();
							if(initialDir.exists()) ch.setInitialDirectory(initialDir);
						}
						File f = ch.showDialog(dialog.getDialogPane().getScene().getWindow());
						if(f != null) directory.setText(f.getAbsolutePath());
					});
					if(disabled.contains(e.getID())) browseDir.setDisable(true);
					grid.add(directory, 1, row);
					grid.add(browseDir, 2, row);
					break;
				}
				case FILE:
				{
					TextField file = new TextField();
					file.setPromptText(e.getPrompt());
					file.setPrefWidth(300);
					GridPane.setHgrow(file, Priority.ALWAYS);
					file.setMaxWidth(Double.MAX_VALUE);
					if(e.getInitialValue() != null) file.setText(((File) e.getInitialValue()).getAbsolutePath());
					if(disabled.contains(e.getID())) file.setDisable(true);
					nodeValueFunctions.put(e.getID(), () -> {
						String path = getString(file);
						if(path == null) return null;
						return new File(path);
					});
					Button browseFile = new Button("Browse...");
					browseFile.setOnAction(event -> {
						FileChooser ch = new FileChooser();
						if(e.getInitialValue() != null) {
							File initialFile = (File) e.getInitialValue();
							if(initialFile.exists()) ch.setInitialDirectory(initialFile.getParentFile());
						}
						File f = ch.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
						if(f != null) file.setText(f.getAbsolutePath());
					});
					if(disabled.contains(e.getID())) browseFile.setDisable(true);
					grid.add(file, 1, row);
					grid.add(browseFile, 2, row);
					break;
				}
				case STRING:
				{
					TextField name = new TextField();
					name.setPromptText(e.getPrompt());
					name.setPrefWidth(300);
					GridPane.setHgrow(name, Priority.ALWAYS);
					name.setMaxWidth(Double.MAX_VALUE);
					if(e.getInitialValue() != null) name.setText((String) e.getInitialValue());
					if(disabled.contains(e.getID())) name.setDisable(true);
					nodeValueFunctions.put(e.getID(), () -> getString(name));
					grid.add(name, 1, row);
					break;
				}
				default:
					throw new UnsupportedOperationException("Invalid element type");
			}
			row++;
		}

		dialog.getDialogPane().setContent(grid);
		
		Button okButton = (Button) dialog.getDialogPane().lookupButton(confirmButton);
		okButton.addEventFilter(ActionEvent.ACTION, ae -> {
			Map<String, Object> data = new HashMap<>();
			nodeValueFunctions.forEach((k, v) -> data.put(k, v.get()));
			String err = verifier.apply(new DialogData(data));
			if(err != null) {
				DialogHelper.showError(err);
				ae.consume();
			}
		});

		dialog.setResultConverter(type -> {
			if (type == confirmButton) {
				Map<String, Object> data = new HashMap<>();
				nodeValueFunctions.forEach((k, v) -> data.put(k, v.get()));
				return new DialogData(data);
			}

			return null;
		});

		return dialog.showAndWait().orElse(null);
	}
	
	private static String getString(TextField textField) {
		String txt = textField.getText();
		if(txt == null || txt.isBlank()) txt = null;
		return txt;
	}

}
