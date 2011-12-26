package org.ohthehumanity.carrie;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;

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

import org.ohthehumanity.carrie.settings.Settings;

/** Main window for the Carrie application.
 **/

public class CarrieActivity extends Activity implements OnSharedPreferenceChangeListener {
public enum Status {
	OK, INTERNAL_ERROR, NO_CONNECTION, TIMEOUT, BAD_URL, NETWORK_ERROR, SERVER_ERROR };

	private static final String TAG = "carrie";
	private SharedPreferences preferences;
	//private string connection_status;
	//private string server_name;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// instantiate our preferences backend
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// set callback function when settings change
		preferences.registerOnSharedPreferenceChangeListener(this);

		Log.i(TAG,
			  "Startup, server '" +
			  preferences.getString("server","") +
			  "' port " +
			  preferences.getString("port", "") +
			  " END");

		if (preferences.getString("server", null) == null) {
			setStatus("Server not set");
		} else if (preferences.getString("port", null) == null) {
			setStatus("Port not configured");
		}

		updateTitle();
		updateSkipLabels();
		setStatus("Ready");
	}

	/** Handle network commands in a separate Task thread
	 **/

	private abstract class HTTPTask extends AsyncTask<String, Integer, String> {
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
		protected void setStatus(String message) {
			final String m = new String(message);
			runOnUiThread(new Runnable() {
					public void run() {
						TextView updateView = (TextView) findViewById(R.id.status);
						updateView.setText(m);
					}
				});
		}

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

	private class PingServerTask extends HTTPTask {
		protected String doInBackground(String... url) {
			setStatus("Requesting server name");
			String target = preferences.getString("server",null) +
				":" +
				preferences.getString("port", null);
			Log.i(TAG, "Calling retrieve");
			retrieve("http://" +
					 target +
					 "/carrie/hello");
			Log.i(TAG, "Done retrieve, status is " + status);
			if (status == CarrieActivity.Status.OK) {
				setStatus("Connected to " + response);
			} else {
				setStatus(statusString());
			}
			return "";
		}
	}

	private class SendCommandTask extends HTTPTask {
		protected String doInBackground(String... url) {
			setStatus("Connecting...");
			String target = preferences.getString("server",null) +
				":" +
				preferences.getString("port", null);
			Log.i(TAG, "Calling retrieve");
			retrieve("http://" + target + "/" + url[0]);
			Log.i(TAG, "Done retrieve, status is " + status);
			if (status == CarrieActivity.Status.OK) {
				setStatus(response);
			} else {
				setStatus(statusString());
			}
			return "";
		}
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
		if (preferences.getString("server", null) == null ||
			preferences.getString("port", null) == null) {
			startActivity(new Intent(this, Settings.class));
		} else {
			Log.i(TAG, "command");
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
		command("backward/" + preferences.getString("small_skip", "7"));
	}

	public void onForwards(View view) {
		command("forward/" + preferences.getString("small_skip", "7"));
	}

	public void onBBackwards(View view) {
		command("backward/" + preferences.getString("large_skip", "60"));
	}

	public void onFForwards(View view) {
		command("forward/" + preferences.getString("large_skip", "60"));
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
		command("sublang");
	}

	public void onAudLang(View view) {
		command("audlang");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "onCreateOptionsMenu");
		startActivity(new Intent(this, Settings.class));
		return false;
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// Change the title bar to show target address and port
		updateTitle();
		// Change the labels showing how far jumps go
		updateSkipLabels();
		// Test the network connection
		new PingServerTask().execute();
	}

	/** Update the window title bar to show server location
	 **/

	private void updateTitle() {
		String vvv = "";
		if (preferences.getString("server", null) == null) {
			setTitle(vvv + "Remote Control - server not set");
		} else {
			setTitle(vvv + "Remote Control - " +
					 preferences.getString("server", "") +
					 ":" +
					 preferences.getString("port", "5505"));
		}
	}

	/** On startup and after changing settings update the labels in between
		the skip back/forwards buttons to show the current skip distances
		in seconds
	**/

	private void updateSkipLabels() {
		((TextView)findViewById(R.id.nudge_seconds)).
			setText(preferences.getString("small_skip", "7") + "s");
		((TextView)findViewById(R.id.skip_seconds)).
			setText(preferences.getString("large_skip", "60") + "s");
	}
}
