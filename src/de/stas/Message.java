package de.stas;

public class Message {
	private String line;
	private boolean error;
	public Message(String line, boolean error) {
		super();
		this.line = line;
		this.error = error;
	}
	public String getLine() {
		return line;
	}
	public void setLine(String line) {
		this.line = line;
	}
	public boolean isError() {
		return error;
	}
	public void setError(boolean error) {
		this.error = error;
	}
	
}
