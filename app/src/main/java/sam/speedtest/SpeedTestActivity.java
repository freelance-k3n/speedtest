package sam.speedtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

public class SpeedTestActivity extends AppCompatActivity {

    private ConnectivityManager connManager;

    private String url = "http://ubuntu.excellmedia.net/releases/16.04.1/ubuntu-16.04.1-desktop-amd64.iso";
    private long FIVE_MB = 1024 * 1024 * 10;

    private TextView wifiSpeed = null;
    private TextView mobileSpeed = null;

    private TextView wifiDuration = null;
    private TextView mobileDuration = null;

    private TextView wifiAvg = null;
    private TextView mobileAvg = null;

    private ToggleButton wifiTglBtn = null;
    private ToggleButton ntwTglBtn = null;

    private ProgressBar wifiProgress = null;
    private ProgressBar mobileProgress = null;

    private boolean testInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);

        wifiTglBtn = (ToggleButton) findViewById(R.id.wifiTglBtn);
        ntwTglBtn = (ToggleButton) findViewById(R.id.ntwTglBtn);

        wifiSpeed = (TextView) findViewById(R.id.wifiSpeed);
        mobileSpeed = (TextView) findViewById(R.id.mobileSpeed);

        wifiDuration = (TextView) findViewById(R.id.wifiDuration);
        mobileDuration = (TextView) findViewById(R.id.mobileDuration);

        wifiAvg = (TextView) findViewById(R.id.wifiAvg);
        mobileAvg = (TextView) findViewById(R.id.mobileAvg);

        wifiProgress = (ProgressBar) findViewById(R.id.wifiProgress);
        mobileProgress = (ProgressBar) findViewById(R.id.mobileProgress);

        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (networkInfo != null) {
                    int type = networkInfo.getType();
                    toggleButtons(type == ConnectivityManager.TYPE_WIFI, type == ConnectivityManager.TYPE_MOBILE);
                } else {
                    toggleButtons(false, false);
                }
            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void toggleButtons(boolean wifiEnabled, boolean mobileEnabled) {
        wifiTglBtn.setEnabled(wifiEnabled);
        ntwTglBtn.setEnabled(mobileEnabled);
    }

    public void startWifiTest(View view) {
        startTest((ToggleButton) view, wifiSpeed, wifiDuration, wifiAvg, wifiProgress, url, FIVE_MB);
    }

    public void startNetworkTest(View view) {
        startTest((ToggleButton) view, mobileSpeed, mobileDuration, mobileAvg, mobileProgress, url, FIVE_MB);
    }

    private void startTest(ToggleButton view, TextView speedHolder, TextView duration, TextView speedAvg, ProgressBar progress, String url, long testSize) {
        ToggleButton btn = view;
        final boolean state = btn.isChecked();
        SpeedTester tester = new SpeedTester(btn, progress, speedHolder, duration, speedAvg, testSize);
        if (!state) {
            tester.execute(url);
        }
    }
}

class SpeedTester extends AsyncTask<String, Long, Void> {

    private TextView instantSpeed;
    private TextView duration;
    private TextView average;
    private ToggleButton tglBtn;
    private ProgressBar progress;
    private boolean testInProgress = true;
    private long testSize;

    private String exception = null;

    private int count = 0;

    private long time = 0, start = 0;

    private double total = 0;

    public SpeedTester(ToggleButton tglBtn, ProgressBar progress, TextView instantSpeed, TextView duration, TextView speedAvg, long testSize) {
        this.tglBtn = tglBtn;
        this.instantSpeed = instantSpeed;
        this.progress = progress;
        this.testSize = testSize;
        this.average = speedAvg;
        this.duration = duration;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progress.setProgress(0);
    }

    @Override
    protected Void doInBackground(String... url) {
        try {
            InputStream is = new URL(url[0]).openStream();
            byte[] buffer = new byte[1024];

            long bytesTransferred = 0, totalBytesTransferred = 0, bytesPerSec = 0;
            start = time = System.currentTimeMillis();
            count = 0;
            while ((bytesTransferred = is.read(buffer)) > 0 && totalBytesTransferred <= testSize && testInProgress) {
                long d = System.currentTimeMillis() - time;
                if(d > 1000L) {
                    publishProgress(bytesPerSec, totalBytesTransferred);
                    bytesPerSec = 0;
                    time = System.currentTimeMillis();
                } else {
                    bytesPerSec += bytesTransferred;
                    totalBytesTransferred += bytesTransferred;
                }
            }
            try {
                if(null != is) {
                    is.close();
                }
            } catch (Exception e1) { }
        } catch (Exception e) {
            testInProgress = false;
            publishProgress(0L, 1L);
            exception = e.getMessage();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        super.onProgressUpdate(values);
        count++;
        double instant = kb(values[0]);
        total += instant;
        instantSpeed.setText(Double.toString(instant) + " KB/s");
        average.setText(Double.toString(twoDigits(total / count)) + " KB/s");
        duration.setText(((System.currentTimeMillis() - start) / 1000) + " Secs");
        progress.setProgress((int) (values[1] * 100/ testSize));
        if(testInProgress == false) {
            tglBtn.setChecked(true);
        } else {
            testInProgress = !tglBtn.isChecked();
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        tglBtn.setChecked(true);
        if(null != exception) {
            Toast.makeText(tglBtn.getContext(), "Something went wrong! - " + exception, Toast.LENGTH_SHORT).show();
        } else {
            progress.setProgress(100);
        }
    }

    private double kb(long speedInBytes) {
        return ((long)(((double)speedInBytes) * 100.0 / 1024.0)) / 100.0;
    }

    private double twoDigits(double value) {
        return ((long)(value * 100.0)) / 100.0;
    }
}