package sam.speedtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

public class SpeedTestActivity extends AppCompatActivity {

    private ConnectivityManager connManager;

    private String url = "http://ubuntu.excellmedia.net/releases/16.04.1/ubuntu-16.04.1-desktop-amd64.iso";
    private double FIVE_MB = 1024 * 1024 * 1;

    private TextView wifiSpeed = null;
    private Button wifiTestBtn = null;
    private Button wifiTestStopBtn = null;
    private Button ntwTestBtn = null;
    private boolean testInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_speed_test);

        wifiTestBtn = (Button) findViewById(R.id.wifiTestBtn);
        wifiTestStopBtn = (Button) findViewById(R.id.wifiTestStopBtn);

        ntwTestBtn = (Button) findViewById(R.id.ntwTestBtn);
        wifiSpeed = (TextView) findViewById(R.id.wifiSpeed);

        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        wifiTestStopBtn.setVisibility(View.GONE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (networkInfo != null) {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        wifiTestBtn.setEnabled(true);
                        ntwTestBtn.setEnabled(false);
                    } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        wifiTestBtn.setEnabled(false);
                        ntwTestBtn.setEnabled(true);
                    } else {
                        wifiTestBtn.setEnabled(false);
                        ntwTestBtn.setEnabled(false);
                    }
                } else {
                    wifiTestBtn.setEnabled(false);
                    ntwTestBtn.setEnabled(false);
                }
            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
    }

    public void startWifiTest(View view) {
        startTest(wifiTestBtn, wifiTestStopBtn, wifiSpeed);
    }

    public void startNetworkTest(View view) {

    }

    private void startTest(Button startTestBtn, Button stopTestBtn, TextView wifiSpeed) {
        try {
            InputStream bis = new URL(url).openStream();

            int bytesTransferred = 0;
            long totalBytesTransferred = 0, kbsTotal = 0;
            byte[] buffer = new byte[1024];

            long time = System.currentTimeMillis();
            long start = System.currentTimeMillis();

            double percentCompleted = 0.0;
            startTestBtn.setEnabled(false);
            stopTestBtn.setEnabled(true);
            testInProgress = true;
            while ((bytesTransferred = bis.read(buffer)) > 0 && testInProgress) {
                percentCompleted = ((long)(totalBytesTransferred / FIVE_MB * 10000.0D) / 100.0D);
                long d = System.currentTimeMillis() - time;
                if(d > 1000L) {
                    time = System.currentTimeMillis();
                    long currentkBRate = kbsTotal / 1024L;
                    wifiSpeed.setText(currentkBRate + " KB/s");
                    kbsTotal = 0;
                } else {
                    kbsTotal += bytesTransferred;
                }
            }
            startTestBtn.setEnabled(true);
            stopTestBtn.setEnabled(false);
        } catch (Exception e) {
            Log.w("mLog", "" + e.getMessage());
        }
    }

    public void stopWifiTest(View view) {
        testInProgress = false;
    }
}