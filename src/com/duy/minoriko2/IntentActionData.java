package com.duy.minoriko2;

import java.util.ArrayList;

public class IntentActionData {
	private static volatile IntentActionData _self = null;
	private String _output = null;
	private boolean _active = false;
	private ArrayList<String> _files = new ArrayList<String>();
	
	public static void addFile(String fname) {
		getSingleton()._files.add(fname);
	}
	
	public static String[] getFiles() {
		return getSingleton()._files.toArray(new String[0]);
	}
	
	public static boolean isSaved() {
		return !getSingleton()._files.isEmpty();
	}

	public static boolean isActive() {
		return getSingleton()._active;
	}

	public static String getOutput() {
		return getSingleton()._output;
	}

	public static void setData(String output) {
		getSingleton()._output = output;
		getSingleton()._active = true;
		getSingleton()._files.clear();
	}

	public static void clearData() {
		getSingleton()._output = null;
		getSingleton()._active = false;
		getSingleton()._files.clear();
	}

	static IntentActionData getSingleton() {
		IntentActionData localInstance = _self;
		if (localInstance == null) {
			synchronized (IntentActionData.class) {
				localInstance = _self;
				if (localInstance == null) {
					_self = localInstance = new IntentActionData();
				}
			}
		}
		return localInstance;
	}
}
