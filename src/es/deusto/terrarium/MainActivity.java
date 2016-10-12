package es.deusto.terrarium;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity 
{
	int notificationID = 1;
	public int datox, temperature;
	private SharedPreferences sharedPrefs = null;
	private ResultSet rs = null;
	protected PendingIntent servicePendingIntent = null;
	protected AlarmManager alarm = null;
	//TODO on settings change update interval for service!

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // set member variables
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // add onClick listener to current temperature label
        TextView tvCurrentTemperature = (TextView) findViewById(R.id.tv_current_temp);
		tvCurrentTemperature.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				forceNewDownload();
			}
		});

		// add onClick listener to service switch
		Switch switchService = (Switch) findViewById(R.id.service_running);
		switchService.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					Intent intent = new Intent(MainActivity.this, TerrariumService.class);
//					intent.setAction("es.deusto.terrarium.CHECK_FOR_SERVICE_ACTION");
					MainActivity.this.servicePendingIntent = PendingIntent.getService(MainActivity.this, 0, intent, 0);

					// set up AlarmManager for the service
					Calendar cal = Calendar.getInstance();
					MainActivity.this.alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
					// Start every x minutes, user defined in settings
					int intervalMinutes = Integer.valueOf(MainActivity.this.sharedPrefs.getString(MainActivity.this.getString(R.string.pref_key_sync_frequency), MainActivity.this.getString(R.string.pref_default_sync_frequency)));
					MainActivity.this.alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), intervalMinutes*60000, MainActivity.this.servicePendingIntent);
					Toast.makeText(MainActivity.this, "TerrariumService " + MainActivity.this.getString(R.string.started), Toast.LENGTH_SHORT).show();
				} else {
					MainActivity.this.alarm.cancel(MainActivity.this.servicePendingIntent);
					Toast.makeText(MainActivity.this, "TerrariumService " + MainActivity.this.getString(R.string.stopped), Toast.LENGTH_SHORT).show();
				}
			}
		});
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		
		// download data only if interval passed or settings changed, otherwise reload data
		rs = DataLoader.getDataIfIntervalPassedOrSettingsChanged("temp", MainActivity.this);

		// update labels (download lid data)
		updateLabels();

		// update service switch
		Switch switchService = (Switch) findViewById(R.id.service_running);
		Intent checkIntent = new Intent(MainActivity.this, TerrariumService.class);
//		checkIntent.setAction("es.deusto.terrarium.CHECK_FOR_SERVICE_ACTION");
		boolean alarmUp = (PendingIntent.getBroadcast(MainActivity.this, 0,
				checkIntent, PendingIntent.FLAG_NO_CREATE) != null);
		if(alarmUp) {
			switchService.setChecked(true);
		}

		// rotation specific changes
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// display buttons next to each other
			// service switch button
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) switchService.getLayoutParams();
			params.addRule(RelativeLayout.CENTER_HORIZONTAL, 0);
			params.addRule(RelativeLayout.LEFT_OF, R.id.label_lid);
			//params.addRule(RelativeLayout.ALIGN_BASELINE, R.id.label_lid);
			switchService.setLayoutParams(params);
			switchService.setBackgroundColor(Color.WHITE);
			switchService.getBackground().setAlpha(166);
			switchService.setText(getString(R.string.service_running_short));

			// plot button
			Button plotButton = (Button) findViewById(R.id.btn_view_plot);
			params = (RelativeLayout.LayoutParams) plotButton.getLayoutParams();
			params.addRule(RelativeLayout.RIGHT_OF, R.id.label_lid);
			params.addRule(RelativeLayout.ALIGN_BASELINE, R.id.service_running);
			params.setMargins(0, 0, 0, 0);
			plotButton.setLayoutParams(params);
			plotButton.setBackgroundColor(Color.WHITE);
			plotButton.getBackground().setAlpha(166);
		} else {
			// for portrait and undefined
			// layout seems to be reset automatically to the vales from the xml layout file
			Switch serviceSwitch = (Switch) findViewById(R.id.service_running);
			serviceSwitch.setText(getString(R.string.service_running));
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Forces a new download of all data and updates the labels. Displays a
	 * Toast on completion.
	 */
	private void forceNewDownload() {
		rs = DataLoader.getData("temp", MainActivity.this);
		updateLabels();
		Toast.makeText(MainActivity.this, "Data updated", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Updates the temperature labels with the values from rs. Loads the lid
	 * data new and updates its label.
	 */
	private void updateLabels() {
		// update TextViews for temperature
		TextView tvCurrentTemperature = (TextView) findViewById(R.id.tv_current_temp);
		tvCurrentTemperature.setText(Math.round(rs.getFirstValue()*10)/10. + "°C");
		TextView tvCurrentMin = (TextView) findViewById(R.id.tv_real_min_temp);
		tvCurrentMin.setText(Math.round(rs.getMinimumValue()*10)/10. + "°C");
		TextView tvCurrentMax = (TextView) findViewById(R.id.tv_real_max_temp);
		tvCurrentMax.setText(Math.round(rs.getMaximumValue()*10)/10. + "°C");
		TextView tvLimitMin = (TextView) findViewById(R.id.tv_lim_min_temp);
		tvLimitMin.setText(sharedPrefs.getString(getString(R.string.pref_key_minTemp), getString(R.string.pref_default_minTemp)) + "°C");
		TextView tvLimitMax = (TextView) findViewById(R.id.tv_lim_max_temp);
		tvLimitMax.setText(sharedPrefs.getString(getString(R.string.pref_key_maxTemp), getString(R.string.pref_default_maxTemp)) + "°C");

		// update TextView for lid
		// download lid data
		ResultSet lidResultSet = DataLoader.getData("lidOpen", MainActivity.this);
		TextView tvLidOpen = (TextView) findViewById(R.id.label_lid);
		if(lidResultSet.getFirstValue() == 1) {
			// lid open
			tvLidOpen.setText(getString(R.string.lid_open));
			tvLidOpen.setTextColor(Color.RED);
		} else {
			// lid closed
			tvLidOpen.setText(getString(R.string.lid_closed));
			tvLidOpen.setTextColor(Color.BLACK);
		}
	}
    
	/**
	 * Open the SettingsActivity upon button click.
	 */
	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) {
		switch (keycode) {
		case KeyEvent.KEYCODE_MENU:
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			startActivity(settingsIntent);
			return true;
		}

		return super.onKeyDown(keycode, e);
	}
	
	/**
	 * Starts an intent for the PlotActivity.
	 * 
	 * @param view
	 */
	public void startPlotActivity(View view) {
		Intent intent = new Intent(this, PlotActivity.class);
	    startActivity(intent);
	}
	
	/**
	 * Returns the currently loaded ResultSet.
	 * 
	 * @return The currently loaded ResultSet.
	 */
	public ResultSet getResultSet() {
		return rs;
	}
}
