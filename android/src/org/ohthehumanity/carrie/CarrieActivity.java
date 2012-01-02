package org.ohthehumanity.carrie;

import java.lang.InterruptedException;
import java.util.concurrent.ExecutionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.Socket;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
//import java.util.concurrent.LinkedBlockingDeque;

import org.apache.http.conn.ConnectTimeoutException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
//import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference;
import android.app.AlertDialog;

import org.ohthehumanity.carrie.CarriePreferences;

/** Main window for the Carrie application.
 **/

public class CarrieActivity extends Activity implements OnSharedPreferenceChangeListener//,
														//														OnPreferenceClickListener
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
		//CarrieActivity.this.findPreference("scan").setOnPreferenceClickListener(this);

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

		updateTitle();
		updateSkipLabels();
		setStatus("Ready");
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
				return;
			}
			if (response_code != HttpURLConnection.HTTP_OK) {
				status = CarrieActivity.Status.SERVER_ERROR;
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

		/** Update the status area in the main screen
		 **/
		// protected void setStatus(String message) {
		// 	final String m = new String(message);
		// 	runOnUiThread(new Runnable() {
		// 			public void run() {
		// 				TextView updateView = (TextView) findViewById(R.id.status);
		// 				updateView.setText(m);
		// 			}
		// 		});
		// }

		protected String statusString() {
			switch(status) {
			case NO_CONNECTION:
				return "Cannot connect to server";
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

	// private class PingServerTask extends HTTPTask {
	// 	protected String doInBackground(String... url) {
	// 		Log.i(TAG, "Pinging address " + url[0]);
	// 		retrieve("http://" + url[0] + ":5505");
	// 		if (status == CarrieActivity.Status.OK) {
	// 			Log.i(TAG, "  retrieve ok, response " + response);
	// 			return response;
	// 		} else {
	// 			Log.i(TAG, "  retrieve nok, status is " + status);
	// 			return "";
	// 		}
	// 	}
	// }

	// private class ScanServersTask extends HTTPTask {
	// 	protected String doInBackground(String... url)
	// 	{
	// 		Log.i(TAG, "Local IP is " + getLocalIpAddress().toString());
	// 		List< PingServerTask > res = new LinkedList< PingServerTask >();
	// 		for(int i=100; i<=120; i++) {
	// 			String address = "192.168.0." + i;
	// 			Log.i(TAG, "Scanning address " + address);
	// 			//String res = "";
	// 			PingServerTask t = new PingServerTask();
	// 			t.execute(address);
	// 			res.add(t);
	// 		}
	// 		for(PingServerTask t : res) {
	// 			try {
	// 				Log.i(TAG, "PingServerTask returns " + t.get());
	// 			} catch (InterruptedException e) {
	// 				return "";
	// 			} catch (ExecutionException e) {
	// 				return "";
	// 			}
	// 		}
	// 		return "";
	// 	}
	// }

	private class ScanServersTask extends AsyncTask<Void, String, LinkedList<String> > {
		protected LinkedList<String> mServers;
		//protected Object mLock;
		protected CarrieActivity mActivity;

		public ScanServersTask(CarrieActivity activity) {
			super();
			mActivity = activity;
		}

		private class Scanner extends Thread {
			String _base_addr;
			int _offset_addr;
			int _port;
			int _timeout;
			ScanServersTask _task;
			public Scanner(String base_addr, int offset_addr, int port, int timeout, ScanServersTask task) {
				_base_addr = base_addr;
				_offset_addr = offset_addr;
				_port = port;
				_task = task;
			}

			public void run() {
				//Log.i(TAG, "threadstart");
				Socket socket = new Socket();
				String server = _base_addr + _offset_addr;
				try {
					socket.connect(new InetSocketAddress(InetAddress.getByName(server), _port), _timeout);
				} catch(IOException e) {
					//Log.i(TAG, "    cannot connect");
					return;
				}
				Log.i(TAG, "    connection on " + server);
				_task.callback(server);
			}
		}

		public void callback(String addr) {
			Log.i(TAG, "Callback " + addr);
			//publishProgress(addr);
			synchronized(this) {
				mServers.add(addr);
			}
		}

		protected void setStatus(String message) {
			final String inner_message = new String(message);
			runOnUiThread(new Runnable() {
					public void run() {
						TextView updateView = (TextView)findViewById(R.id.status);
						updateView.setText(inner_message);
					}
				});
		}

		protected LinkedList<String> doInBackground(Void... params) {
			mServers = new LinkedList<String>();
			Log.i(TAG, "Begin scan");
			String subnet = "192.168.0.";
			setStatus("Scanning " + subnet + "* ...");
			int timeout = 1000;
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
			Log.i(TAG, "Threads dead, found " + mServers.size() + " servers");
			return mServers;
		}

		protected void onPostExecute(LinkedList<String> servers) {
			setStatus("Found " + mServers.size() + " servers");
			switch(mServers.size()) {
			case 0:
				return;
			case 1:
				SharedPreferences.Editor editor = mPreferences.edit();
				editor.putString("server", mServers.getFirst());
				editor.commit();
				return;
			default:
				AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
				alert.setTitle("Choose server");
				alert.setMessage("many - " + mServers.size());
				alert.show();
				return;
			}
		}
	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(TAG, ex.toString());
		}
		return null;
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

	public void onPlay(View view) {
		setStatus("on play");
		Log.i(TAG, "onPlay");
		//String res = send("play");
		command("pause");
	}

	public void onFullscreen(View view) {
		setStatus("on fullscreen");
		command("fullscreen");
	}

	public void onBackwards(View view) {
		command("backward/" + mPreferences.getString("small_skip", "7"));
	}

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
		Log.i(TAG, "activity result, request " + requestCode + " result " + resultCode);
		if (requestCode == 1) { // change to check intent.scan = true
			if (resultCode == RESULT_OK) {
				// A contact was picked.  Here we will just display it
				// to the user.
				//				startActivity(new Intent(Intent.ACTION_VIEW, data));
				Log.i(TAG, "Begin scan in correct thread");
				new ScanServersTask(this).execute();
			}
		}
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		mServerName = "";
		// Change the title bar to show target address and port
		updateTitle();
		// Change the labels showing how far jumps go
		updateSkipLabels();
		// Test the network connection
		setStatus("Requesting server name");
		new GetServerNameTask().execute();
	}

	// public boolean onPreferenceClick (Preference preference) {
	// 	Log.i(TAG, "onPreferencesClick " + preference.getTitle());
	// 	if (preference.getTitle() == "Scan") {
	// 		Log.i(TAG, "Scan request");
	// 		return true;
	// 	} else if (preference.getTitle() == "Homepage") {
	// 		Log.i(TAG, "Open homepage");
	// 		return true;
	// 	} else {
	// 		return false;
	// 	}
	// }

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
