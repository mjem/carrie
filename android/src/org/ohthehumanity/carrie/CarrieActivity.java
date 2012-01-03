// Carrie Remote Control
// Copyright (C) 2012 Mike Elson

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.ohthehumanity.carrie;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.Socket;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.lang.InterruptedException;
import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.apache.http.conn.ConnectTimeoutException;

import android.os.Bundle;
import android.os.AsyncTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.content.DialogInterface;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;

import org.ohthehumanity.carrie.CarriePreferences;

/** Main window for the Carrie application.
 **/

public class CarrieActivity extends Activity implements OnSharedPreferenceChangeListener
{
	public enum Status {
		OK, INTERNAL_ERROR, NO_CONNECTION, TIMEOUT, BAD_URL, NETWORK_ERROR, SERVER_ERROR };

	private static final String TAG = "carrie";
	private SharedPreferences mPreferences;
	//private string connection_status;
	private String mServerName;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mServerName = "";
		setContentView(R.layout.main);

		// instantiate our preferences backend
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		// set callback function when settings change
		mPreferences.registerOnSharedPreferenceChangeListener(this);

		Log.i(TAG,
			  "Startup, server '" +
			  mPreferences.getString("server","") +
			  "' port " +
			  mPreferences.getString("port", "") +
			  " END");

		if (mPreferences.getString("server", null) == null) {
			setStatus("Server not set");
		} else if (mPreferences.getString("port", null) == null) {
			setStatus("Port not configured");
		}

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo().getType() !=
			ConnectivityManager.TYPE_WIFI) {

			AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
			dlgAlert.setTitle("WiFi not active");
			dlgAlert.setMessage("This application is usually used on a local network over a WiFi. Open WiFi settings?");
			dlgAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch(which) {
						case DialogInterface.BUTTON_POSITIVE:
							//Yes button clicked
							final Intent intent = new Intent(Intent.ACTION_MAIN, null);
							intent.addCategory(Intent.CATEGORY_LAUNCHER);
							final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.wifi.WifiSettings");
							intent.setComponent(cn);
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity( intent);
							break;

						case DialogInterface.BUTTON_NEGATIVE:
							Log.i(TAG, "Not opening wifi");
							//No button clicked
							break;
						}
					}
				});

			dlgAlert.setNegativeButton("No", null);
			dlgAlert.setCancelable(true);
			dlgAlert.create().show();
		}

		updateTitle();
		updateSkipLabels();
		updateServerName();
	}

	/** ABC to send a single network command in a separate Task thread
	 **/

	private abstract class HTTPTask extends AsyncTask<String, Integer, Void> {
		// TBD split no connection into NO_ROUTE and NO_SERVER
		protected CarrieActivity.Status status;

		protected String response;

		protected String url;

		protected void retrieve(String request)
		{
			url = request;
			URL urlobj;
			try {
				urlobj = new URL(request);
			} catch (MalformedURLException e) {
				status = CarrieActivity.Status.BAD_URL;
				return;
			}
			URLConnection conn;
			try {
				conn = urlobj.openConnection();
			} catch (IOException e) {
				status = CarrieActivity.Status.NO_CONNECTION;
				return;
			}
			Log.i(TAG, "Opening URL " + request);

			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setAllowUserInteraction(false);
			httpConn.setInstanceFollowRedirects(true);
			try {
				httpConn.setRequestMethod("GET");
			} catch (ProtocolException e) {
				status = CarrieActivity.Status.INTERNAL_ERROR;
				return;
			}
			try {
				httpConn.connect();
			} catch (ConnectTimeoutException e) {
				Log.i(TAG, "timeout");
				status = CarrieActivity.Status.TIMEOUT;
				return;
			} catch (IOException e) {
				Log.i(TAG, "ioexception");
				status = CarrieActivity.Status.NO_CONNECTION;
				return;
			}
			Log.i(TAG, "Sending request");

			int response_code;
			try {
				response_code = httpConn.getResponseCode();
			} catch (IOException e) {
				status = CarrieActivity.Status.INTERNAL_ERROR;
				Log.e(TAG, "Exception reading response code");
				return;
			}
			if (response_code != HttpURLConnection.HTTP_OK) {
				status = CarrieActivity.Status.SERVER_ERROR;
				Log.e(TAG, "Server returned " + response_code);
				return;
			}
			Log.i(TAG, "Got response code " + response_code);
			InputStream in;
			try {
				in = httpConn.getInputStream();
			} catch (IOException e) {
				status = CarrieActivity.Status.INTERNAL_ERROR;
				return;
			}

			InputStreamReader isr = new InputStreamReader(in);
			int charRead;
			int BUFFER_SIZE = 2000;
			char[] inputBuffer = new char[BUFFER_SIZE];
			response = "";
			try {
				while ((charRead = isr.read(inputBuffer))>0)
					{
						String readString =
							String.copyValueOf(inputBuffer, 0, charRead);
						response += readString;
						inputBuffer = new char[BUFFER_SIZE];
					}
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				status = CarrieActivity.Status.NETWORK_ERROR;
				return;
			}
			status = CarrieActivity.Status.OK;
			Log.i(TAG, "Server response was " + response);
		}

		protected String statusString() {
			switch(status) {
			case NO_CONNECTION:
				return "Cannot connect";
			case INTERNAL_ERROR:
				return "Internal error";
			case TIMEOUT:
				return "Timeout";
			case BAD_URL:
				return "Bad url: " + url;
			case NETWORK_ERROR:
				return "Network error";
			case SERVER_ERROR:
				return "Server error";
			default:
				return "ok";
			}
		}
	}

	/** Request the server name from a Carrie server and update the status
		bar **/

	private class GetServerNameTask extends HTTPTask {
		protected Void doInBackground(String... url) {
			Log.d(TAG, "GetServerNameTask.doInBackground");
			String target = mPreferences.getString("server",null) +
				":" +
				mPreferences.getString("port", null);
			Log.i(TAG, "Calling retrieve");
			retrieve("http://" +
					 target +
					 "/carrie/hello");
			Log.i(TAG, "Done retrieve, status is " + status);
			return null;
		}

		protected void onPostExecute(Void dummy) {
			Log.i(TAG, "GetServerName.onPostExecute");
			if (status == CarrieActivity.Status.OK) {
				setStatus("Connected to " + response);
				mServerName = response;
			} else {
				setStatus(statusString() + " " + mPreferences.getString("server",null));
			}
			updateTitle();
		}
	}

	/** Send a command to the server and show the status
	 **/

	private class SendCommandTask extends HTTPTask {
		protected Void doInBackground(String... url) {
			String target = mPreferences.getString("server", null) +
				":" +
				mPreferences.getString("port", null);
			Log.i(TAG, "Calling retrieve");
			retrieve("http://" + target + "/" + url[0]);
			Log.i(TAG, "Done retrieve, status is " + status);
			return null;
		}

		protected void onPostExecute(Void dummy) {
			Log.i(TAG, "onPostExecute");
			if (status == CarrieActivity.Status.OK) {
				setStatus(response);
			} else {
				setStatus(statusString());
			}
		}
	}

	private class ScanServersTask extends AsyncTask<Void, String, LinkedList<String> > {
		protected LinkedList<String> mServers;
		//protected Object mLock;
		protected CarrieActivity mActivity;

		public ScanServersTask(CarrieActivity activity) {
			super();
			mActivity = activity;
		}

		private class Scanner extends Thread {
			String mBaseAddr;
			int mOffsetAddr;
			int mPort;
			int mTimeout;
			ScanServersTask mTask;

			public Scanner(String baseAddr, int offsetAddr, int port, int timeout, ScanServersTask task) {
				mBaseAddr = baseAddr;
				mOffsetAddr = offsetAddr;
				mPort = port;
				mTask = task;
			}

			public void run() {
				//Log.i(TAG, "threadstart");
				Socket socket = new Socket();
				String server = mBaseAddr + mOffsetAddr;
				try {
					socket.connect(new InetSocketAddress(InetAddress.getByName(server), mPort),
								   mTimeout);
				} catch(IOException e) {
					//Log.i(TAG, "    cannot connect");
					try {
						socket.close();
					} catch(IOException e2) {
						// ignore anything thrown by close()
					}
					return;
				}
				try {
					socket.close();
				} catch(IOException e) {
				}
				Log.i(TAG, "    connection on " + server);
				mTask.callback(server);
			}
		}

		/** As each thread completes and exits, call this function to record the new server
			if found
		 **/

		public void callback(String addr) {
			Log.i(TAG, "Callback " + addr);
			//publishProgress(addr);
			synchronized(this) {
				mServers.add(addr);
			}
		}

		/** Wrapper to update the status area of the main activity
		 **/

		protected void setStatus(String message) {
			final String inner_message = new String(message);
			runOnUiThread(new Runnable() {
					public void run() {
						TextView updateView = (TextView)findViewById(R.id.status);
						updateView.setText(inner_message);
					}
				});
		}

		/** Initiate scan.
			Each request is sent by a seperate thread so all 256 possible
			IP addresses on the local /8 subnet can be tested in parallel
		**/

		protected LinkedList<String> doInBackground(Void... params) {
			mServers = new LinkedList<String>();
			String full_ip = getLocalIpAddress();
			int index = full_ip.lastIndexOf(".");
			if (index == -1) {
				Log.e(TAG, "Cannot decode IP " + full_ip);
				return mServers;
			}
			String subnet = full_ip.substring(0, index + 1);
			Log.i(TAG, "Begin scan of " + subnet);
			setStatus("Scanning " + subnet + "* ...");
			int timeout = 500; // ms
			int port = Integer.parseInt(mPreferences.getString("port", null));
			LinkedList<Scanner> scanners = new LinkedList<Scanner>();
			for(int i=0; i<=255; i++) {
				Scanner t = new Scanner(subnet, i, port, timeout, this);
				scanners.add(t);
				t.start();
			}
			Log.i(TAG, "End scan initiation, waiting for threads to exit");
			for(Iterator<Scanner> i = scanners.iterator(); i.hasNext(); ) {
				try {
					i.next().join();
				} catch(InterruptedException e) {
				}
			}
			Log.i(TAG, "Threads finished, found " + mServers.size() + " servers");
			return mServers;
		}

		/** After scan completed update the UI
		 **/

		protected void onPostExecute(LinkedList<String> servers) {
			Log.d(TAG, "ScanServersTask.onPostExecute");
			setStatus("Found " + mServers.size() + " servers");
			switch(mServers.size()) {
			case 0:
				// no servers found
				return;
			case 1:
				// single server found, automatically connect
				Log.d(TAG, "Updating preferences");
				setServer(servers.getFirst());
				return;
			default:
				// multiple servers found, present the user with a choice dialog
				AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
				alert.setTitle("Choose server");
				final String[] inner_servers = new String[servers.size()];
				servers.toArray(inner_servers);
				alert.setItems(inner_servers,
							   new DialogInterface.OnClickListener() {
								   public void onClick(DialogInterface dialog, int item) {
									   setServer(inner_servers[item]);
								   }
							   });
				alert.show();
				return;
			}
		}
	}

	/** Write a new string into our `server` preference and ping it to check
		for the hostname
	**/

	public void setServer(String server) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString("server", server);
		editor.commit();
		updateServerName();
	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
				 en.hasMoreElements();) {

				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
					 enumIpAddr.hasMoreElements();) {

					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(TAG, ex.toString());
		}
		return "";
	}

	/** Set windoww status field
	 **/

	public void setStatus(String message) {
		TextView updateView = (TextView) findViewById(R.id.status);
		if (updateView == null) {
			Log.e(TAG, "R.id.status is null");
		} else {
			updateView.setText(message);
		}
	}

	/** Tell our worker thread to send a command over the network
	 **/

	public void command(String message) {
		if (mPreferences.getString("server", null) == null ||
			mPreferences.getString("port", null) == null) {
			startActivity(new Intent(this, CarriePreferences.class));
		} else {
			Log.i(TAG, "command");
			setStatus("Connecting...");
			new SendCommandTask().execute(message);
		}
	}

	/** Handle click on Play/Pause button
	 **/

	public void onPlay(View view) {
		setStatus("on play");
		Log.i(TAG, "onPlay");
		//String res = send("play");
		command("pause");
	}

	/** Handle click on Fullscreen button
	 **/

	public void onFullscreen(View view) {
		setStatus("on fullscreen");
		command("fullscreen");
	}

	/** Handle click on little backwards button
	 **/

	public void onBackwards(View view) {
		command("backward/" + mPreferences.getString("small_skip", "7"));
	}

	/** Handle click on little forwards button
	 **/

	public void onForwards(View view) {
		command("forward/" + mPreferences.getString("small_skip", "7"));
	}

	public void onBBackwards(View view) {
		command("backward/" + mPreferences.getString("large_skip", "60"));
	}

	public void onFForwards(View view) {
		command("forward/" + mPreferences.getString("large_skip", "60"));
	}

	public void onOSDOn(View view) {
		command("osdon");
	}

	public void onOSDOff(View view) {
		command("osdoff");
	}

	public void onMute(View view) {
		command("mute");
	}

	public void onVoldown(View view) {
		command("voldown");
	}

	public void onVolup(View view) {
		command("volup");
	}

	public void onSub(View view) {
		command("sub");
	}

	public void onSubLang(View view) {
		//new ScanServersTask().execute();
		command("sublang");
	}

	public void onAudLang(View view) {
		command("audlang");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "onCreateOptionsMenu");
		//startActivity(new Intent(this, CarriePreferences.class));
		startActivityForResult(new Intent(this, CarriePreferences.class), 1);
		return false;
	}

	/** Automatically called when an activity (our preferences) that was started with
		startActivityForResult exits **/

	protected void onActivityResult(int requestCode,
									int resultCode,
									Intent data) {
		if (data == null) {
			Log.i(TAG, "Preferences closed no scan requested");
		} else {
			Log.i(TAG, "Preferences closed scan is " + data.getBooleanExtra("scan", false));
		}

		if (data != null && data.getBooleanExtra("scan", false) == true) {
			Log.i(TAG, "Begin scan in correct thread");
			new ScanServersTask(this).execute();
		} else {
			Log.d(TAG, "Updating server name");
			updateServerName();
		}
	}

	/** Callback when a preference is changed, either through code or the preferences screen
	 **/

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		mServerName = "";
		// Change the title bar to show target address and port
		updateTitle();
		// Change the labels showing how far jumps go
		updateSkipLabels();
		// Test the network connection
		//setStatus("Requesting server name");
		//new GetServerNameTask().execute();
	}

	/** Update the window title and status area to show the target server name
		as returned by a check on url http://<servername>:<port>/carrie/hello
	 **/

	private void updateServerName() {
		setStatus("Requesting server name");
		Log.d(TAG, "Starting GetServerNameTask");
		new GetServerNameTask().execute();
	}

	/** Update the window title bar to show server location
	 **/

	private void updateTitle() {
		String prelude = "Remote Control - ";
		if (mPreferences.getString("server", null) == null) {
			setTitle(prelude + "server not set");
		} else if (mServerName.equals("")) {
			setTitle(prelude +
					 mPreferences.getString("server", "") +
					 ":" +
					 mPreferences.getString("port", "5505"));
		} else {
			setTitle(prelude + mServerName);
		}
	}

	/** On startup and after changing settings update the labels in between
		the skip back/forwards buttons to show the current skip distances
		in seconds
	**/

	private void updateSkipLabels() {
		((TextView)findViewById(R.id.nudge_seconds)).
			setText(mPreferences.getString("small_skip", "7") + "s");
		((TextView)findViewById(R.id.skip_seconds)).
			setText(mPreferences.getString("large_skip", "60") + "s");
	}
}
