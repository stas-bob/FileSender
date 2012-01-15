package de.stas;


import android.os.Parcel;
import android.os.Parcelable;

public class Message implements Parcelable {
	private String line;
	private boolean error;
	public Message(String line, boolean error) {
		super();
		this.line = line;
		this.error = error;
	}
	
	public Message(Parcel in) {
		readFromParcel(in);
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
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(line);
		byte b = 0;
		if (error) {
			b = 1;
		}
		dest.writeByte(b);
	}
	
	private void readFromParcel(Parcel in) {
		line = in.readString();
		error = in.readByte() == 1;
	}
}
