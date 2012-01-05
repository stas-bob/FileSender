package de.stas;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import de.stas.db.DBWrapper;
import de.stas.dialogs.ConfirmDialog;
import de.stas.dialogs.InfoDialog;
import de.stas.dialogs.InfoDialogs;
import de.stas.dialogs.WarningDialog;

public class BaseActivity extends Activity {
	protected DBWrapper dbwrapper;
	
	protected InfoDialogs infoDialog;
	protected InfoDialogs warningDialog;
	protected ConfirmDialog confirmDialog;
	
	public static final int CONFIRM_DIALOG = 1;
	public static final int INFO_DIALOG = 2;
	public static final int WARNING_DIALOG = 3;
	
	@Override
	public Dialog onCreateDialog(int i) {
		switch (i) {
		case WARNING_DIALOG: return warningDialog;
		case INFO_DIALOG: return infoDialog;
		case CONFIRM_DIALOG: return confirmDialog;
		}
		return null;
	}
	
	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		dbwrapper = new DBWrapper(this);
		warningDialog = new WarningDialog(this, null);
		infoDialog = new InfoDialog(this, null);
		confirmDialog = new ConfirmDialog(this, null, null);
	}	
	
	@Override
	protected void onStart() {
		super.onStart();
		dbwrapper.resume();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		dbwrapper.close();
	}
}
