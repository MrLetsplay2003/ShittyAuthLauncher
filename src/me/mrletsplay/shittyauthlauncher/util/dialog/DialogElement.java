package me.mrletsplay.shittyauthlauncher.util.dialog;

public class DialogElement {
	
	private DialogInputType type;
	private String id;
	private String name;
	private String prompt;
	private Object initialValue;
	
	public DialogElement(DialogInputType type, String id, String name, String prompt, Object initialValue) {
		this.type = type;
		this.id = id;
		this.name = name;
		this.prompt = prompt;
		this.initialValue = initialValue;
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

}
