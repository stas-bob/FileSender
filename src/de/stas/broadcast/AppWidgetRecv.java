package de.stas.broadcast;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import de.stas.service.WidgetUpdateService;

public class AppWidgetRecv extends AppWidgetProvider {
	
	@Override
	public void onEnabled(Context c) {
		Intent i = new Intent("de.stas.broadcast.AppWidgetRecv_ALARM");
		PendingIntent pi = PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarms = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
		alarms.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 1000, pi);
	}
	
	@Override
	public void onReceive(Context c, Intent i) {
		super.onReceive(c, i);
		c.startService(new Intent(c, WidgetUpdateService.class));
	}
}
