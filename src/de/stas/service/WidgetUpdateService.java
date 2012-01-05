package de.stas.service;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.RemoteViews;
import de.stas.Main;
import de.stas.R;
import de.stas.broadcast.AppWidgetRecv;

public class WidgetUpdateService extends Service implements ServiceConnection {
	private ServiceINTF service;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent ix, int flags, int startCommand) {
        final ComponentName thisWidget = new ComponentName(this, AppWidgetRecv.class);
        final AppWidgetManager manager = AppWidgetManager.getInstance(this);
        
		final RemoteViews updateViews = buildUpdate(this);
		Intent i = new Intent();
		i.setAction("de.stas.service.SendService");
		bindService(i, this, BIND_AUTO_CREATE);
		new Thread() {
			
			@Override
			public void run() {
				while (service == null);
				try {
					while(true) {
						int sec = service.getTimeTillNextScan();
						int min = (sec / 60)%60;
						updateViews.setTextViewText(R.id.widget_textView, "Next scan in " + sec / 3600 + "h " + min + "m " + sec % 60 + "s");
						manager.updateAppWidget(thisWidget, updateViews);
						synchronized(this) {
							try {
								wait(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				} catch (android.os.DeadObjectException ee) {
					
				} catch (final RemoteException e) {
					e.printStackTrace();
				}
			}
		}.start();
		return START_STICKY;
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
 }