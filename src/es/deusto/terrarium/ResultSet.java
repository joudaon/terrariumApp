package es.deusto.terrarium;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author moritz
 *
 */
public class ResultSet {
	private String dataName = "";
	private String deviceId = "";
	private ArrayList<Double> values = null;
	private ArrayList<Long> timestamps = null;
	private ArrayList<Double> interleaved = null;
	
	public ResultSet(String dataName, String deviceId,
			ArrayList<Double> values, ArrayList<Long> timestamps, ArrayList<Double> interleaved) {
		this.dataName = dataName;
		this.deviceId = deviceId;
		this.values = values;
		this.timestamps = timestamps;
		this.interleaved = interleaved;
	}

	public String getDataName() {
		return dataName;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public ArrayList<Double> getValues() {
		return values;
	}

	public ArrayList<Long> getTimestamps() {
		return timestamps;
	}
	
	public ArrayList<Double> getInterleaved() {
		return interleaved;
	}
	
	/**
	 * @return The maximum value. 0 if empty.
	 */
	public double getMaximumValue() {
		if(values.isEmpty()) {
			return 0;
		}
		return Collections.max(values);
	}
	
	/**
	 * @return The minimum value. 0 if empty.
	 */
	public double getMinimumValue() {
		if(values.isEmpty()) {
			return 0;
		}
		return Collections.min(values);
	}

	/**
	 * @return The first value. 0 if empty.
	 */
	public double getFirstValue() {
		if(values.isEmpty()) {
			return 0;
		}
		return values.get(0);
	}

}
