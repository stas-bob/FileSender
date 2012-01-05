package de.stas.dialogs;

import de.stas.R;
import android.content.Context;
import android.widget.Button;

public class WarningDialog extends InfoDialogs {

	public WarningDialog(Context c, Runnable okRunnable) {
		super(c, R.drawable.warning_icon, okRunnable);
		setTitle("Warning");
		((Button)findViewById(R.id.info_dialog_ok_button)).setOnClickListener(this);
	}

}
