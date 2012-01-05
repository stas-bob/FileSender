package de.stas.dialogs;

import android.content.Context;
import android.widget.Button;
import de.stas.R;

public class InfoDialog extends InfoDialogs {
	
	public InfoDialog(Context c, Runnable okRunnable) {
		super(c, R.drawable.info_icon, okRunnable);
		setTitle("Info");
		((Button)findViewById(R.id.info_dialog_ok_button)).setOnClickListener(this);
	}
}
