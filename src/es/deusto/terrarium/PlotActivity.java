package es.deusto.terrarium;



import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;

import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.XPositionMetric;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.YValueMarker;

public class PlotActivity extends Activity 
{
	private XYPlot plot;
	private SharedPreferences sharedPrefs = null;
	private ResultSet rs = null;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_plot);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		plot = (XYPlot) findViewById(R.id.grafica);
        
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// download data only if interval passed or settings changed, otherwise reload data
		rs = DataLoader.getDataIfIntervalPassedOrSettingsChanged("temp", this);
        
        ArrayList<Double> interleavedData = rs.getInterleaved();
        XYSeries series = new SimpleXYSeries(interleavedData, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED,"Current temperature");
        LineAndPointFormatter seriesFormat = new LineAndPointFormatter(Color.rgb(127, 255, 0 ),0x000000,0x000000,null);
        plot.clear();
        plot.addSeries(series, seriesFormat);
		
        // min/max markers if set by the user
		plot.removeMarkers();
		boolean showLimits = sharedPrefs.getBoolean(getString(R.string.pref_key_show_limits), true);
		setPlotBoundaries(showLimits);
		if (showLimits) {
			setPlotMarkers();
		}

		plot.redraw();
    }
	
	/**
	 * Sets the boundaries of the plot depending on the actual values and the
	 * user defined limits.
	 * 
	 * @param showLimits
	 *            defines if the boundaries have to take the user defined limits
	 *            into account
	 */
    private void setPlotBoundaries(boolean showLimits)
    {
    	if(showLimits) {
    		int minimumLimit = Integer.valueOf(sharedPrefs.getString(getString(R.string.pref_key_minTemp), getString(R.string.pref_default_minTemp)));
        	double minimumValue = Math.min(rs.getMinimumValue(), minimumLimit) - 0.5;
        	int maximumLimit = Integer.valueOf(sharedPrefs.getString(getString(R.string.pref_key_maxTemp), getString(R.string.pref_default_maxTemp)));
        	double maximumValue = Math.max(rs.getMaximumValue(), maximumLimit) + 0.5;
        	
        	plot.setRangeBoundaries(minimumValue, BoundaryMode.FIXED, maximumValue, BoundaryMode.FIXED);
    	} else {
    		plot.setRangeBoundaries(rs.getMinimumValue()-0.5, BoundaryMode.FIXED, rs.getMaximumValue()+0.5, BoundaryMode.FIXED);
    	}
    }
    
    private void setPlotMarkers() 
    {
        int maximumLimit = Integer.valueOf(sharedPrefs.getString(getString(R.string.pref_key_maxTemp), getString(R.string.pref_default_maxTemp)));
        YValueMarker maxTempMarker = new YValueMarker(
                maximumLimit,                                        // y-val to mark
                "max",                      // marker label
                new XPositionMetric(                        // object instance to set text positioning on the marker
                        PixelUtils.dpToPix(5),              // 5dp offset
                        XLayoutStyle.ABSOLUTE_FROM_LEFT),  // offset origin
                Color.RED,                                 // line paint color
                Color.RED);                                // text paint color 
        
    	int minimumLimit = Integer.valueOf(sharedPrefs.getString(getString(R.string.pref_key_minTemp), getString(R.string.pref_default_minTemp)));
        YValueMarker minTempMarker = new YValueMarker(
                minimumLimit,                                        // y-val to mark
                "min",                         // marker label
                new XPositionMetric(                        // object instance to set text positioning on the marker
                        PixelUtils.dpToPix(5),              // 5dp offset
                        XLayoutStyle.ABSOLUTE_FROM_LEFT),  // offset origin
                Color.BLUE,                                 // line paint color
                Color.BLUE);                                // text paint color


        maxTempMarker.getTextPaint().setTextSize(PixelUtils.dpToPix(14));
        minTempMarker.getTextPaint().setTextSize(PixelUtils.dpToPix(14));

        DashPathEffect dpe = new DashPathEffect(new float[]{PixelUtils.dpToPix(2), PixelUtils.dpToPix(2)}, 0);

        maxTempMarker.getLinePaint().setPathEffect(dpe);
        minTempMarker.getLinePaint().setPathEffect(dpe);

        plot.addMarker(maxTempMarker);
        plot.addMarker(minTempMarker);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.edit_values, menu);
		return true;
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
	 * Returns the currently loaded ResultSet.
	 * 
	 * @return The currently loaded ResultSet.
	 */
	public ResultSet getResultSet() {
		return rs;
	}

}
