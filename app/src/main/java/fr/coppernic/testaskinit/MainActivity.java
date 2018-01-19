package fr.coppernic.testaskinit;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

import fr.coppernic.sdk.ask.Defines;
import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.ask.ReaderListener;
import fr.coppernic.sdk.ask.RfidTag;
import fr.coppernic.sdk.ask.SearchParameters;
import fr.coppernic.sdk.ask.sCARD_SearchExt;
import fr.coppernic.sdk.powermgmt.PowerMgmt;
import fr.coppernic.sdk.powermgmt.PowerMgmtFactory;
import fr.coppernic.sdk.powermgmt.cone.identifiers.InterfacesCone;
import fr.coppernic.sdk.powermgmt.cone.identifiers.ManufacturersCone;
import fr.coppernic.sdk.powermgmt.cone.identifiers.ModelsCone;
import fr.coppernic.sdk.powermgmt.cone.identifiers.PeripheralTypesCone;
import fr.coppernic.sdk.utils.core.CpcBytes;
import fr.coppernic.sdk.utils.core.CpcDefinitions;
import fr.coppernic.sdk.utils.io.InstanceListener;

public class MainActivity extends AppCompatActivity  implements InstanceListener<Reader>, ReaderListener{
    public static final String TAG = "TestAskInit";
    private PowerMgmt powerMgmt;
    private Reader reader;
    private boolean testRunning = false;
    private int errors = 0;
    private int total = 0;
    private TextView tvErrors;
    private TextView tvTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvErrors = (TextView)findViewById(R.id.tvErrors);
        tvTotal = (TextView)findViewById(R.id.tvTotal);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testRunning = !testRunning;

                if (testRunning) {
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_24dp));
                    new Thread(testRunnable).start();
                } else {
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_24dp));
                }
            }
        });

        // Instantiates power management object
        powerMgmt = PowerMgmtFactory.get().
                setContext(this).
                setPeripheralTypes(PeripheralTypesCone.RfidSc).
                setManufacturers(ManufacturersCone.Ask).
                setModels(ModelsCone.Ucm108).
                setInterfaces(InterfacesCone.ExpansionPort).
                build();

        // Instantiates ASK reader object
        Reader.getInstance(this, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Runnable testRunnable = new Runnable() {
        @Override
        public void run() {
            // Powers on ASK reader
            powerMgmt.powerOn();
            Log.d(TAG, "powerOn");
            // Opens ASK reader
            int res = reader.cscOpen(CpcDefinitions.ASK_READER_PORT, 115200, false);
            Log.d(TAG, "cscOpen: " + Defines.errorLookUp(res));
            res = reader.cscSetTimings(2000,2000,0);
            Log.d(TAG, "cscSetTimings: " + Defines.errorLookUp(res));
            // Waits for reader to be ready
            //res = reader.cscResetCsc();
            //Log.d(TAG, "cscResetCsc: " + Defines.errorLookUp(res));
            SystemClock.sleep(1000);
            // Initializes communication
            StringBuilder sb = new StringBuilder();
            res = reader.cscVersionCsc(sb);

            if (res == Defines.RCSC_Timeout) {
                Log.e(TAG, "TIMEOUT!");
                res = reader.cscVersionCsc(sb);
            }

            if (res != Defines.RCSC_Ok) {
                errors++;
            }

            Log.d(TAG, "Version CSC: " + sb.toString());
            Log.e(TAG, "cscVersionCsc: " + Defines.errorLookUp(res));
            // Adds a test result to the ArrayList

            // Closes reader
            reader.cscClose();
            Log.d(TAG, "cscClose");
            // Powers off reader
            powerMgmt.powerOff();
            Log.d(TAG, "powerOff");
            total++;

            setResults();

            // Starts a new test
            if (testRunning) {
                new Thread(testRunnable).start();
            }
        }
    };

    @Override
    public void onCreated(Reader reader) {
        this.reader = reader;
    }

    @Override
    public void onDisposed(Reader reader) {

    }

    @Override
    public void onTagDiscovered(RfidTag rfidTag) {

        Log.d(TAG, "ATR: " + CpcBytes.byteArrayToString(rfidTag.getAtr()));


    }

    @Override
    public void onDiscoveryStopped() {
        Log.d(TAG, "Discovery stopped");
    }

    /**
     * Waits a random time between 0 and 4 seconds
     */
    private void waitRandomTime() {
        Random rand = new Random();
        int n = rand.nextInt(5) * 1000;
        Log.d(TAG, "Random wait: " + n + "ms");
        SystemClock.sleep(n);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(testRunnable).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void setResults() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvErrors.setText(Integer.toString(errors));
                tvTotal.setText(Integer.toString(total));
            }
        });
    }
}