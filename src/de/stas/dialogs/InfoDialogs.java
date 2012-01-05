package de.stas.dialogs;

import de.stas.R;
import android.content.Context;
import android.view.View;

public abstract class InfoDialogs extends Dialogs implements android.view.View.OnClickListener {
	private Runnable okRunnable;
	
	public InfoDialogs(Context c, int drawableId, Runnable okRunnable) {
		super(c, R.layout.info_dialog, drawableId);
		this.okRunnable = okRunnable;
	}

	@Override
	public void onClick(View v) {
		if (okRunnable != null) {
			okRunnable.run();
		}
		dismiss();
	}
	
	public void setOkRunnable(Runnable okRunnable) {
		this.okRunnable = okRunnable;
	}
}
