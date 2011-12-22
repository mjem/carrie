package org.ohthehumanity.carrie;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

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
	private static final String TAG = "carrie";
	private SharedPreferences preferences;

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

	private class SendCommandTask extends AsyncTask<String, Integer, String> {
		private InputStream OpenHttpConnection(String urlString)
			throws IOException
		{
			InputStream in = null;
			int response = -1;

			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();

			Log.i(TAG, "Opening URL ".concat(urlString));

			if (!(conn instanceof HttpURLConnection))
				throw new IOException("Not an HTTP connection");

			//try{
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setAllowUserInteraction(false);
			httpConn.setInstanceFollowRedirects(true);
			httpConn.setRequestMethod("GET");
			//		  try {
			setStatus("connecting to ".concat(urlString));
			try {
				httpConn.connect();
			} catch (ConnectTimeoutException e) {
				Log.i(TAG, "timeout");
				setStatus("connection timeout");
			} catch (IOException e) {
				Log.i(TAG, "ioexception");
				setStatus("Cannot connect to " +
						  preferences.getString("server","") +
						  ":" +
						  preferences.getString("port", ""));
			}
			Log.i(TAG, "Sending request");

			response = httpConn.getResponseCode();
			if (response == HttpURLConnection.HTTP_OK) {
				in = httpConn.getInputStream();
			}
			Log.i(TAG, "Got result");

			return in;
		}

		/** Command the server process by requesting URL `commnad` on the server
		 **/

		private String send(String command)
		{
			Log.i(TAG, "send " + command);
			InputStream in = null;
			try {
				in = OpenHttpConnection("http://" +
										preferences.getString("server",null) +
										":" +
										preferences.getString("port", null) +
										"/" +
										command);
			} catch(ConnectTimeoutException e) {
				return "Connection timeout";
			} catch (IOException e) {
				return e.getMessage();
			}
			if (in == null) {
				return "server could not interpret command";
			}
			InputStreamReader isr = new InputStreamReader(in);
			int charRead;
			String str = "";
			int BUFFER_SIZE = 2000;
			char[] inputBuffer = new char[BUFFER_SIZE];
			try {
				while ((charRead = isr.read(inputBuffer))>0)
					{
						String readString =
							String.copyValueOf(inputBuffer, 0, charRead);
						str += readString;
						inputBuffer = new char[BUFFER_SIZE];
					}
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				return "";
			}
			return str;
		}

		protected void setStatus(String message) {
			final String m = new String(message);
			runOnUiThread(new Runnable() {
					public void run() {
						TextView updateView = (TextView) findViewById(R.id.status);
						updateView.setText(m);
					}
				});
		}

		protected String doInBackground(String... url) {
			Log.i(TAG, "doInBackground " + url[0]);
			setStatus(send(url[0]));
			return null;
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
		updateTitle();
		updateSkipLabels();
	}

	/** Update the window title bar to show server location
	 **/

	private void updateTitle() {
		if (preferences.getString("server", null) == null) {
			setTitle("Remote Control - server not set");
		} else {
			setTitle("Remote Control - " +
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
