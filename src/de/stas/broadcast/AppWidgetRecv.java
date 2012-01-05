package de.stas.broadcast;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import de.stas.service.WidgetUpdateService;

public class AppWidgetRecv extends AppWidgetProvider {
	@Override
	public void onUpdate(Context c, AppWidgetManager mgr, int[] appWidgetIds) {
		c.startService(new Intent(c, WidgetUpdateService.class));
	}
}
