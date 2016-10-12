package es.deusto.terrarium;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * DataLoader capsules the connection to the server to retrieve certain values.
 * It offers methods to supply the data in appropriate formats.\n\n It reads the
 * server address, deviceId, and number of items from the SharedPreferences.
 * 
 * @author moritz
 * 
 */
public class DataLoader {
	
	private static ResultSet rs = null;
	private static String serverUrl = "";
	private static String deviceId = "";
	private static int nItems = -1;
	private static Date lastDownload = null;

	/**
	 * Downloads the data identified by dataIdentifier according to the
	 * SharedPreferences settings in context (server URL, deviceID, number of
	 * items). Stores the values from new to old.
	 * 
	 * On error it displays a Toast and returns the old ResultSet if available.
	 * Otherwise an empty set.
	 * 
	 * @param dataIdentifier
	 * @param context
	 * @return
	 */
	public static ResultSet getData(String dataIdentifier, Context context) {
		DownloadData downloadTask = new DownloadData(context);
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		serverUrl = sharedPref.getString(context.getString(R.string.pref_key_upload_server_address),
				context.getString(R.string.pref_default_upload_server_address));
		deviceId = sharedPref.getString(context.getString(R.string.pref_key_device_id),
				context.getString(R.string.pref_default_device_id));
		nItems = Integer.valueOf(sharedPref.getString(context.getString(R.string.pref_key_num_items),
				context.getString(R.string.pref_default_num_items)));
		downloadTask.execute(serverUrl + "?device_id=" + deviceId
				+ "&data_name=" + dataIdentifier + "&nitems=" + nItems);
		try {
			JSONArray result = downloadTask.get();
			// check if null returned (no connection or bad URL)
			if(result != null) {
				ArrayList<Long> timestamps = new ArrayList<Long>();
				ArrayList<Double> values = new ArrayList<Double>();
				ArrayList<Double> interleaved = new ArrayList<Double>();
				for(Object object : result) {
					JSONObject obj = (JSONObject) object;
					String timestampString = (String)obj.get("timestamp");
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
					Date date = df.parse(timestampString);
					Double value = Double.valueOf(obj.get("data_value").toString());

					timestamps.add(date.getTime());
					values.add(value);

					interleaved.add(Double.valueOf(date.getTime()));
					interleaved.add(value);
				}
				ResultSet resultSet = new ResultSet(dataIdentifier, deviceId, values, timestamps, interleaved);
				if(dataIdentifier.equals("temp")) {
					//TODO make this better (for example Map for different identifiers)!!
					lastDownload = new Date();
					rs = resultSet;
				}
				return resultSet;
			} else {
				// IOException and MalformedURLException already handled inside task
			}
		} catch (InterruptedException e) {
			Toast.makeText(context, "Error: Interrupted when downloading data", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} catch (ExecutionException e) {
			Toast.makeText(context, "Download error", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} catch (ParseException e) {
			Toast.makeText(context, "ParseException when parsing downloaded data. Please contact the developers.", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}

		// if error retrieving the data (e.g. no connection) return old ResultSet
		if(rs != null) {
			return rs;
		} else {
			// if not available, return empty ResulSet
			ArrayList<Long> timestamps = new ArrayList<Long>();
			ArrayList<Double> values = new ArrayList<Double>();
			ArrayList<Double> interleaved = new ArrayList<Double>();
			ResultSet errorSet = new ResultSet(dataIdentifier, deviceId, values, timestamps, interleaved);
			return errorSet;
		}
	}
	
	/**
	 * Downloads the data identified by dataIdentifier according to the
	 * SharedPreferences settings in context (server URL, deviceID, number of
	 * items) ONLY if the defined download interval has passed or the settings
	 * since the last download have changed. Otherwise it returns the same
	 * ResultSet as the last download. Stores the values from new to old.
	 * 
	 * On error it displays a Toast and returns the old ResultSet if available.
	 * Otherwise an empty set.
	 * 
	 * @param dataIdentifier
	 * @param context
	 * @return
	 */
	public static ResultSet getDataIfIntervalPassedOrSettingsChanged(String dataIdentifier, Context context) {
		if(lastDownload == null || rs == null) {
			// download for the first time
			return getData(dataIdentifier, context);
		}
		
		Date now = new Date();
		long differenceMs = now.getTime() - lastDownload.getTime();
		
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String serverUrlNew = sharedPref.getString(context.getString(R.string.pref_key_upload_server_address),
				context.getString(R.string.pref_default_upload_server_address));
		String deviceIdNew = sharedPref.getString(context.getString(R.string.pref_key_device_id),
				context.getString(R.string.pref_default_device_id));
		int nItemsNew = Integer.valueOf(sharedPref.getString(context.getString(R.string.pref_key_num_items),
				context.getString(R.string.pref_default_num_items)));
		int intervalMinutes = Integer.valueOf(sharedPref.getString(context.getString(R.string.pref_key_sync_frequency),
				context.getString(R.string.pref_default_sync_frequency)));
		long intervalMs = intervalMinutes * 60000; 
		
		if(differenceMs < intervalMs
				&& serverUrlNew.equals(serverUrl)
				&& deviceIdNew.equals(deviceId)
				&& nItemsNew == nItems) {
			// return previously downloaded ResultSet
			return rs;
		}

		// else download again
		return getData(dataIdentifier, context);
	}

	/*
	 * Convenience AsyncTask for downloading the data
	 */
	private static class DownloadData extends AsyncTask<String, Void, JSONArray> {
		private Context context = null;
		Activity activity = null;

		public DownloadData(Context context) {
			this.context = context;
			try {
				activity = (Activity) context;
			} catch (ClassCastException e) {
				// service called this, no error toast needed
			}
		}

		@Override
		protected JSONArray doInBackground(String... urls) {
			try {
				URL dataServer = new URL(urls[0]);

				HttpURLConnection conn = (HttpURLConnection) dataServer
						.openConnection();
				conn.setReadTimeout(10000 /* milliseconds */);
				conn.setConnectTimeout(15000 /* milliseconds */);
				conn.setRequestMethod("GET");
				conn.setDoInput(true);
				InputStream stream = conn.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						stream));
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();

				Reader in = new InputStreamReader(dataServer.openStream());

				Object obj = JSONValue.parse(in);
				JSONArray array = (JSONArray) obj;

				return array;
			} catch (MalformedURLException e) {
				if(activity != null) {
					activity.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(context, "Connection error: check the server URL", Toast.LENGTH_SHORT).show();
						}
					});
				}
				e.printStackTrace();
			} catch (IOException e) {
				if(activity != null) {
					activity.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show();
						}
					});
				}
				e.printStackTrace();
			}
			return null;
		}
	}

}