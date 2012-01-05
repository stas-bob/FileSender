package de.stas.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import de.stas.R;

public class ConfirmDialog extends Dialogs implements android.view.View.OnClickListener {
	private Button okButton, cancelButton;
	private Runnable okRunnable, cancelRunnable;
	
	public ConfirmDialog(Context c, Runnable okRunnable, Runnable cancelRunnable) {
		super(c, R.layout.confirm_dialog, R.drawable.question);
		okButton = (Button)findViewById(R.id.confirm_ok_dialog_button);
		cancelButton = (Button)findViewById(R.id.confirm_cancel_dialog_button);
		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
		this.cancelRunnable = cancelRunnable;
		this.okRunnable = okRunnable;
	}

	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.confirm_ok_dialog_button:
			if (okRunnable != null) {
				okRunnable.run();
			}
			break;
		case R.id.confirm_cancel_dialog_button:
			if (cancelRunnable != null) {
				cancelRunnable.run();
			} 
			break;
		}
		dismiss();
	}
	
	public void setOkRunnable(Runnable okRunnable) {
		this.okRunnable = okRunnable;
	}
	
	public void setCancelRunnable(Runnable cancelRunnable) {
		this.cancelRunnable = cancelRunnable;
	}

}
