package me.mrletsplay.shittyauthlauncher.util.dialog;

import java.util.List;

public class DialogElement {

	private DialogInputType type;
	private String id;
	private String name;
	private String prompt;
	private Object initialValue;
	private List<Object> choices;

	public DialogElement(DialogInputType type, String id, String name, String prompt, Object initialValue, List<Object> choices) {
		this.type = type;
		this.id = id;
		this.name = name;
		this.prompt = prompt;
		this.initialValue = initialValue;
		this.choices = choices;
	}

	public DialogInputType getType() {
		return type;
	}

	public String getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPrompt() {
		return prompt;
	}

	public Object getInitialValue() {
		return initialValue;
	}

	public List<Object> getChoices() {
		return choices;
	}

}
