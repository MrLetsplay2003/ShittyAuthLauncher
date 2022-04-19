package me.mrletsplay.shittyauthlauncher.util.dialog;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import me.mrletsplay.shittyauthlauncher.ShittyAuthLauncher;

public class DialogHelper {

	public static void showWarning(String warning) {
		Alert a = new Alert(AlertType.WARNING);
		a.initOwner(ShittyAuthLauncher.stage);
		a.setContentText(warning);
		a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		a.showAndWait();
	}
	
	public static void showError(String error) {
		Alert a = new Alert(AlertType.ERROR);
		a.initOwner(ShittyAuthLauncher.stage);
		a.setContentText(error);
		a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		a.showAndWait();
	}
	
	public static void showError(String error, Throwable r) {
		Alert a = new Alert(AlertType.ERROR);
		a.initOwner(ShittyAuthLauncher.stage);
		a.setHeaderText(error);
		a.setResizable(true);
		
		StringWriter w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);
		r.printStackTrace(pw);
		String exceptionText = w.toString();
		
		Label label = new Label("Exception stacktrace:");
		TextArea area = new TextArea(exceptionText);
		area.setEditable(false);
		area.setWrapText(true);
		area.setMaxWidth(Double.MAX_VALUE);
		area.setMaxHeight(Double.MAX_VALUE);
		area.autosize();
		
		GridPane.setVgrow(area, Priority.ALWAYS);
		GridPane.setHgrow(area, Priority.ALWAYS);
		
		GridPane content = new GridPane();
		content.setMaxWidth(Double.MAX_VALUE);
		content.add(label, 0, 0);
		content.add(area, 0, 1);
		
		a.getDialogPane().setContent(content);
		
		a.showAndWait();
	}
	
	public static boolean showYesNo(String message) {
		Alert a = new Alert(AlertType.CONFIRMATION);
		a.initOwner(ShittyAuthLauncher.stage);
		a.getButtonTypes().clear();
		a.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
		a.setContentText(message);
		a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		return a.showAndWait().orElse(null) == ButtonType.YES;
	}
	
}
