package de.stas.dialogs;

import de.stas.R;
import android.app.Dialog;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class Dialogs extends Dialog {
	private TextView tv;
	
	public Dialogs(Context c, int layoutId, int drawableId) {
		super(c);
		setContentView(layoutId);
		tv = (TextView) findViewById(R.id.dialog_textView);
		((ImageView)findViewById(R.id.dialog_imgView)).setImageDrawable(c.getResources().getDrawable(drawableId));
	}
	
	public void setText(String text) {
		tv.setText(text);
	}
}
