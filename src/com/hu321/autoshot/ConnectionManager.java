package com.hu321.autoshot;

import java.util.ArrayList;

import android.util.Log;

public class ConnectionManager {
	
	public static final int MAX_CONNECTIONS = 5;
	public static final int MAX_QUEUE = 10;

	private ArrayList<HttpConnection> active = new ArrayList<HttpConnection>();
	private ArrayList<HttpConnection> queue = new ArrayList<HttpConnection>();

	private static ConnectionManager instance;

	public static ConnectionManager getInstance() {
		if (instance == null)
			instance = new ConnectionManager();
		return instance;
	}

	public void push(HttpConnection runnable) {
		queue.add(runnable);
		if (active.size() < MAX_CONNECTIONS)
		{
			startNext();
		}
		else
		{
			HttpConnection old = active.get(0);
			active.remove(0);
			old.stop();
			startNext();
		}
	}

	private void startNext() {
		if (!queue.isEmpty()) {
			HttpConnection next = queue.get(0);
			queue.remove(0);
			active.add(next);

			next.start();
		}
	}

	public void didComplete(HttpConnection runnable) {
		active.remove(runnable);
		startNext();
	}
}
