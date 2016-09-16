package sam.speedtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.net.URL;

public class SpeedTestActivity extends AppCompatActivity {

    private ConnectivityManager connManager;

    private String url = "http://ubuntu.excellmedia.net/releases/16.04.1/ubuntu-16.04.1-desktop-amd64.iso";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);

        final Button wifiTestBtn = (Button) findViewById(R.id.wifiTestBtn);
        final Button ntwTestBtn = (Button) findViewById(R.id.ntwTestBtn);

        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

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
        startTest();
    }

    public void startNetworkTest(View view) {

    }

    private void startTest() {
        try {
            InputStream bis = new URL(url).openStream();

            long start = System.nanoTime();
            long totalRead = 0;
            final double NANOS_PER_SECOND = 1000000000.0;
            final double FIVE_MB = 5 * 1024 * 1024;

            int bytesTransferred = 0;
            long totalBytesTransferred = 0, kbsTotal = 0;
            byte[] buffer = new byte[1024];

            long time = System.currentTimeMillis();

            double percentCompleted = 0.0;
            Log.w("mLog", "STARTED");
            while ((bytesTransferred = bis.read(buffer, 0, 1024)) > 0 || totalBytesTransferred <= FIVE_MB) {
                percentCompleted = ((long)(totalBytesTransferred / FIVE_MB * 10000.0D) / 100.0D);
                long d = System.currentTimeMillis() - time;
                if(d > 1000L) {
                    time = System.currentTimeMillis();
                    long currentkBRate = kbsTotal / 1024L;
                    Log.w("mLog", "Current: " + currentkBRate + " KB/s");
                    Log.w("mLog", "Time: " + ((time - start) / 1000L));
                } else {
                    kbsTotal += bytesTransferred;
                }
                totalRead += bytesTransferred;
            }
        } catch (Exception e) {
            Log.w("mLog", e.getMessage());
        }
    }
}
