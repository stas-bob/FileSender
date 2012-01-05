package de.stas.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootUpStarter extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Intent service = new Intent("de.stas.service.SendService");
			context.startService(service);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
