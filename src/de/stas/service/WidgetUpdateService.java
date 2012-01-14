package de.stas.service;


import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.RemoteViews;
import de.stas.Main;
import de.stas.R;
import de.stas.broadcast.AppWidgetRecv;

public class WidgetUpdateService extends IntentService implements ServiceConnection {
	private ServiceINTF service;
	
	public WidgetUpdateService() {
		super("WidgetUpdateIntentService");
	}
	
	private RemoteViews buildUpdate(WidgetUpdateService updateService) {
		RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget);
		Intent i = new Intent(getApplicationContext(), Main.class);
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_layout, pi);
		return views;
	}

	@Override
	public void onServiceConnected(ComponentName arg0, IBinder arg1) {
		service = ServiceINTF.Stub.asInterface(arg1);
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		service = null;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ComponentName thisWidget = new ComponentName(this, AppWidgetRecv.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
		RemoteViews updateViews = buildUpdate(this);
		Intent i = new Intent();
		i.setAction("de.stas.service.SendService");
		bindService(i, this, BIND_AUTO_CREATE);
			
		while (service == null);
		try {
			int sec = service.getTimeTillNextScan();
			int min = (sec / 60)%60;
			if (sec == 0) {
				updateViews.setTextViewText(R.id.widget_textView, "currently scanning");
			} else {
				updateViews.setTextViewText(R.id.widget_textView, "Next scan in " + sec / 3600 + "h " + min + "m " + sec % 60 + "s");
			}
			manager.updateAppWidget(thisWidget, updateViews);
		} catch (Exception e) {
			e.printStackTrace();
		}
		unbindService(this);
	}
}
