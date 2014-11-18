package org.igarape.copcast.views;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.igarape.copcast.R;
import org.igarape.copcast.service.GcmIntentService;
import org.igarape.copcast.service.LocationService;
import org.igarape.copcast.service.RecorderService;
import org.igarape.copcast.service.UploadService;
import org.igarape.copcast.utils.ApiClient;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.NetworkUtils;

import static org.igarape.copcast.utils.Globals.getDirectorySize;
import static org.igarape.copcast.utils.FileUtils.formatMegaBytes;
import static org.igarape.copcast.utils.Globals.getDirectoryUploadedSize;


public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = MainActivity.class.getName();
    public static SurfaceView mSurfaceView;
    public static SurfaceHolder mSurfaceHolder;
    public static Camera mCamera;
    public static boolean mPreviewRunning;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        ActionBar ab = getActionBar(); //needs  import android.app.ActionBar;
        ab.setTitle(Globals.getUserName());
        ab.setSubtitle(Globals.getUserLogin(this));

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(UploadService.UPLOAD_PROGRESS_ACTION)) {
                    long size = intent.getLongExtra(UploadService.FILE_SIZE, 0);
                    Log.d(TAG, "Progress upload received:" + size);
                    Globals.setDirectoryUploadedSize(getDirectoryUploadedSize() + size);
                    ((ProgressBar) findViewById(R.id.progressBar)).setProgress(getDirectoryUploadedSize().intValue());
                    ((TextView) findViewById(R.id.uploadingLabel)).setText(getString(R.string.uploading_size, formatMegaBytes(getDirectoryUploadedSize()), formatMegaBytes(getDirectorySize())));
                } else if (intent.getAction().equals(GcmIntentService.START_STREAMING)) {
                    Toast.makeText(getApplicationContext(), "START STREAMING", Toast.LENGTH_LONG).show();
                } else if (intent.getAction().equals(GcmIntentService.STOP_STREAMING)) {
                    Toast.makeText(getApplicationContext(), "STOP STREAMING", Toast.LENGTH_LONG).show();
                } else {
                    findViewById(R.id.uploadLayout).setVisibility(View.VISIBLE);
                    findViewById(R.id.uploadingLayout).setVisibility(View.GONE);
                    findViewById(R.id.streamLayout).setVisibility(View.GONE);

                    Intent intentAux = new Intent(MainActivity.this, UploadService.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    stopService(intentAux);
                    if (intent.getAction().equals(UploadService.CANCEL_UPLOAD_ACTION)) {
                        Toast.makeText(getApplicationContext(), getString(R.string.upload_stopped), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.upload_completed), Toast.LENGTH_LONG).show();
                    }
                }
            }
        };

        ApiClient.get("/pictures/small/show", null, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Bitmap bm = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.length);
                Globals.setUserImage(bm);
                getActionBar().setIcon(new BitmapDrawable(MainActivity.this.getResources(), bm));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });

        ((ProgressBar)findViewById(R.id.progressBar)).setMax(getDirectorySize().intValue());

        ((TextView)findViewById(R.id.uploadingLabel)).setText(getString(R.string.uploading_size, 0, formatMegaBytes(getDirectorySize())));
        ((TextView)findViewById(R.id.uploadData)).setText(getString(R.string.upload_data_size, formatMegaBytes(getDirectorySize())));

        final Button starMissionButton = (Button) findViewById(R.id.startMissionButton);
        starMissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                starMissionButton.setVisibility(View.GONE);

                findViewById(R.id.settingsLayout).setVisibility(View.VISIBLE);
                ((TextView)findViewById(R.id.welcome)).setText(getString(R.string.mission_start));
                ((TextView)findViewById(R.id.welcomeDesc)).setText(getString(R.string.mission_start_desc));
                findViewById(R.id.uploadLayout).setVisibility(View.GONE);
                findViewById(R.id.uploadingLayout).setVisibility(View.GONE);
                findViewById(R.id.streamLayout).setVisibility(View.VISIBLE);
                findViewById(R.id.recBall).setVisibility(View.VISIBLE);

                Intent intent = new Intent(MainActivity.this, RecorderService.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startService(intent);

                intent = new Intent(MainActivity.this, LocationService.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startService(intent);
            }
        });

        final Button endMissionButton = (Button) findViewById(R.id.endMissionButton);
        endMissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                starMissionButton.setVisibility(View.VISIBLE);

                findViewById(R.id.settingsLayout).setVisibility(View.GONE);
                ((TextView)findViewById(R.id.welcome)).setText(getString(R.string.welcome));
                ((TextView)findViewById(R.id.welcomeDesc)).setText(getString(R.string.welcome_desc));

                findViewById(R.id.uploadLayout).setVisibility(View.VISIBLE);
                findViewById(R.id.uploadingLayout).setVisibility(View.GONE);
                findViewById(R.id.streamLayout).setVisibility(View.GONE);
                findViewById(R.id.recBall).setVisibility(View.INVISIBLE);

                Intent intent = new Intent(MainActivity.this, RecorderService.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                stopService(intent);

                intent = new Intent(MainActivity.this, LocationService.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                stopService(intent);
            }
        });


        ((Button) findViewById(R.id.uploadButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (NetworkUtils.canUpload(getApplicationContext(), getIntent())) {
                    findViewById(R.id.uploadLayout).setVisibility(View.GONE);
                    findViewById(R.id.uploadingLayout).setVisibility(View.VISIBLE);
                    findViewById(R.id.streamLayout).setVisibility(View.GONE);

                    Intent intent = new Intent(MainActivity.this, UploadService.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startService(intent);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.upload_disabled), Toast.LENGTH_LONG).show();
                }
            }
        });

        ((ImageView) findViewById(R.id.uploadCancelButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.uploadLayout).setVisibility(View.VISIBLE);
                findViewById(R.id.uploadingLayout).setVisibility(View.GONE);
                findViewById(R.id.streamLayout).setVisibility(View.GONE);

                Intent intent = new Intent(MainActivity.this, UploadService.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                stopService(intent);
            }
        });

        ((Button)findViewById(R.id.pauseRecordingButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), getString(R.string.under_construction), Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    protected void onDestroy() {
        Globals.clear(MainActivity.this);
        ApiClient.setToken(null);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_logout){
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        Globals.clear(MainActivity.this);
        ApiClient.setToken(null);
        stopService(new Intent(MainActivity.this, RecorderService.class));
        stopService(new Intent(MainActivity.this, LocationService.class));
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        MainActivity.this.finish();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(UploadService.UPLOAD_PROGRESS_ACTION);
        filter.addAction(UploadService.CANCEL_UPLOAD_ACTION);
        filter.addAction(UploadService.COMPLETED_UPLOAD_ACTION);
        filter.addAction(GcmIntentService.START_STREAMING);
        filter.addAction(GcmIntentService.STOP_STREAMING);
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), filter);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }
}
