/**
 */
package es.deusto.terrarium;

import java.util.Date;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

/**
 * @author moritz
 *
 */
public class TerrariumService extends IntentService {
	public static int TEMPERATURE_NOTIFICATION_ID = 1;
	public static int LID_NOTIFICATION_ID = 2;
	private SharedPreferences sharedPrefs = null;
	private ResultSet rs = null;

	public TerrariumService() {
		super("TerrariumService");
	}

	/* (non-Javadoc)
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		// set member variables (cannot access the context in constructor)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
		// temperature
		boolean showTemperatureNotification = sharedPrefs.getBoolean(getString(R.string.pref_key_temperature_limits_notifications), true);
		if (showTemperatureNotification) {
			rs = DataLoader.getData("temp", this);
			showTemperatureNotifications();
		}

		// lid
		boolean showLidNotification = sharedPrefs.getBoolean(getString(R.string.pref_key_lid_open_notifications), true);
		if (showLidNotification) {
			ResultSet rsLid = DataLoader.getData("lidOpen", this);
			if(rsLid.getFirstValue() == 1) {
				// lid open
				Date at = new Date(rsLid.getTimestamps().get(0)); // guaranteed to have a value
				String dateString = "("
						+ (at.getHours() < 10 ? "0" + at.getHours() : at.getHours())
						+ ":"
						+ (at.getMinutes() < 10 ? "0" + at.getMinutes() : at.getMinutes())
						+ ":"
						+ (at.getSeconds() < 10 ? "0" + at.getSeconds() : at.getSeconds()) + ") ";
				displayNotification(getString(R.string.tickerLidNotification), dateString + getString(R.string.contentLidNotification), LID_NOTIFICATION_ID);
			} else {
				// lid closed or data not retrieved! (rsLid.getFirstValue() returns 0 on failure)
				// nothing to do
			}
		}
		

		//TODO debug output
		Toast.makeText(this, "Terrarium updated", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDestroy() {
//		Toast.makeText(this, "TerrariumService stopped", Toast.LENGTH_SHORT).show();
	}

	private void showTemperatureNotifications() {
		int maxTemp = Integer.valueOf(sharedPrefs.getString(getString(R.string.pref_key_maxTemp), getString(R.string.pref_default_maxTemp)));
		int minTemp = Integer.valueOf(sharedPrefs.getString(getString(R.string.pref_key_minTemp), getString(R.string.pref_default_minTemp)));
		double currentTemp = rs.getFirstValue();
		if(currentTemp > maxTemp) {
			displayNotification(getString(R.string.tickerTemperatureHighNotification), getString(R.string.contentTemperatureHighNotification), TEMPERATURE_NOTIFICATION_ID);
		}
		else if(currentTemp < minTemp) {
			displayNotification(getString(R.string.tickerTemperatureLowNotification), getString(R.string.contentTemperatureLowNotification), TEMPERATURE_NOTIFICATION_ID);
		}
	}

	/**
	 * Displays a notification. Reads the SharedPreferences to turn vibrate on
	 * or off
	 * 
	 * @param ticker
	 * @param contentText
	 */
	protected void displayNotification(CharSequence ticker, CharSequence contentText, int notificationID)
	{
		Intent i = new Intent(this, MainActivity.class);
		i.putExtra("notificationID", notificationID);

		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i, 0);
		NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		CharSequence contentTitle = getString(R.string.app_name);

		// read vibrate setting
		boolean vibrate = sharedPrefs.getBoolean(getString(R.string.pref_key_notifications_temperature_limits_vibrate), true);
		long[] pattern = null;
		if (vibrate) {
			pattern = new long[] {100, 250, 100, 500};
		}

		// read ringtone setting
		Uri soundUri = Uri.parse(sharedPrefs.getString(getString(R.string.pref_key_notifications_temperature_limits_ringtone), ""));

		Notification noti = new NotificationCompat.Builder(this)
								.setContentIntent(pendingIntent)
								.setTicker(ticker)
								.setContentTitle(contentTitle)
								.setContentText(contentText)
								.setSmallIcon(R.drawable.ic_launcher)
								.addAction(R.drawable.ic_launcher, ticker, pendingIntent)
								.setVibrate(pattern)
								.setAutoCancel(true)
								.setSound(soundUri)
								.build();
		nm.notify(notificationID, noti);
	}
}
