package org.igarape.copcast.views;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.igarape.copcast.R;
import org.igarape.copcast.service.GcmIntentService;
import org.igarape.copcast.service.LocationService;
import org.igarape.copcast.service.StreamService;
import org.igarape.copcast.service.UploadService;
import org.igarape.copcast.service.VideoRecorderService;
import org.igarape.copcast.state.State;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HistoryUtils;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.NetworkUtils;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.igarape.copcast.utils.FileUtils.formatMegaBytes;
import static org.igarape.copcast.utils.Globals.getDirectorySize;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();
    public static SurfaceView mSurfaceView;
    public static SurfaceHolder mSurfaceHolder;
    public static Camera mCamera;
    public static boolean mPreviewRunning;
    private BroadcastReceiver receiver;
    private Button mStarMissionButton;
    private Button mEndMissionButton;
    private Button mPauseRecordingButton;
    private Button mResumeMissionButton;
    private TextView mPauseCounter;
    private CountDownPausedTimer mCountDownThirtyPaused;
    private CountDownPausedTimer mCountDownTenPaused;
    private Switch mStreamSwitch;
    private CompoundButton.OnCheckedChangeListener mStreamListener;

    private MediaPlayer mySongclick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStreamSwitch = (Switch) findViewById(R.id.streamSwitch);
        mStarMissionButton = (Button) findViewById(R.id.startMissionButton);
        mEndMissionButton = (Button) findViewById(R.id.endMissionButton);
        mPauseRecordingButton = (Button) findViewById(R.id.pauseRecordingButton);
        mResumeMissionButton = (Button) findViewById(R.id.resumeMissionButton);
        mPauseCounter = (TextView) findViewById(R.id.pauseCounter);

        mCountDownThirtyPaused = new CountDownPausedTimer(1800000, 1000);
        mCountDownTenPaused = new CountDownPausedTimer(600000, 1000);
        mStreamListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Globals.setToggling(true);
                if (isChecked) {
                    HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.STREAMING, Globals.getUserLogin(MainActivity.this));

                    Intent intentAux = new Intent(MainActivity.this, VideoRecorderService.class);
                    intentAux.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    stopService(intentAux);

//                    intentAux = new Intent(MainActivity.this, StreamService.class);
//                    intentAux.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startService(intentAux);

                } else {
                    HistoryUtils.registerHistory(getApplicationContext(), State.STREAMING, State.RECORDING_ONLINE, Globals.getUserLogin(MainActivity.this));

                    Intent intentAux = new Intent(MainActivity.this, StreamService.class);
                    intentAux.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    stopService(intentAux);

//                    intentAux = new Intent(MainActivity.this, VideoRecorderService.class);
//                    intentAux.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startService(intentAux);

                }
            }
        };

        ActionBar ab = getActionBar(); //needs  import android.app.ActionBar;
        ab.setTitle(Globals.getUserName(getApplicationContext()));
        ab.setSubtitle(Globals.getUserLogin(this));

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(UploadService.UPLOAD_PROGRESS_ACTION)) {
                    updateProgressBar();
                } else if (intent.getAction().equals(GcmIntentService.START_STREAMING_ACTION)) {
                    if (isMissionStarted()) {
                        mStreamSwitch.setChecked(true);
                    }
                } else if (intent.getAction().equals(GcmIntentService.STOP_STREAMING_ACTION)) {
                    if (isMissionStarted()) {
                        mStreamSwitch.setChecked(false);
                    }
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

        NetworkUtils.get(getApplicationContext(), "/pictures/icon/show", NetworkUtils.Response.BYTEARRAY, new HttpResponseCallback() {
            @Override
            public void unauthorized() {

            }

            @Override
            public void failure(int statusCode) {

            }

            @Override
            public void noConnection() {

            }

            @Override
            public void badConnection() {

            }

            @Override
            public void badRequest() {

            }

            @Override
            public void badResponse() {

            }

            @Override
            public void success(byte[] response) {
                final byte[] responseBody = response;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Bitmap bm = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.length);
                        Globals.setUserImage(bm);
                        getActionBar().setIcon(new BitmapDrawable(MainActivity.this.getResources(), bm));
                    }
                });
            }
        });

         ((ProgressBar) findViewById(R.id.progressBar)).setMax(getDirectorySize(getApplicationContext()).intValue());

        ((TextView) findViewById(R.id.uploadingLabel)).setText(getString(R.string.uploading_size, 0, formatMegaBytes(getDirectorySize(getApplicationContext()))));
        ((TextView) findViewById(R.id.uploadData)).setText(getString(R.string.upload_data_size, formatMegaBytes(getDirectorySize(getApplicationContext()))));

        mStarMissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isUploading()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    builder.setMessage(R.string.stop_uploading)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    stopUploading();
                                    startMission();

                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });
                    // Create the AlertDialog object and return it
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } else {
                    startMission();
                }
            }

            private void startMission() {
                mStarMissionButton.setVisibility(View.GONE);

                vibrate(200); //vibrate when touch a button
                talk("mission_started");
                // Log.d("talk","mission started");

                findViewById(R.id.settingsLayout).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.welcome)).setText(getString(R.string.mission_start));
                ((TextView) findViewById(R.id.welcomeDesc)).setText(getString(R.string.mission_start_desc));
                findViewById(R.id.uploadLayout).setVisibility(View.GONE);
                findViewById(R.id.uploadingLayout).setVisibility(View.GONE);
                findViewById(R.id.streamLayout).setVisibility(View.VISIBLE);
                findViewById(R.id.recBall).setVisibility(View.VISIBLE);

                Intent intent = new Intent(MainActivity.this, VideoRecorderService.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startService(intent);

                intent = new Intent(MainActivity.this, LocationService.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startService(intent);

                HistoryUtils.registerHistory(getApplicationContext(), State.LOGGED, State.RECORDING_ONLINE, Globals.getUserLogin(MainActivity.this));
            }


        });

        mEndMissionButton.setOnClickListener(new View.OnClickListener()

                                             {
                                                 @Override
                                                 public void onClick(View view) {
                                                     missionCompleted();

                                                     mStarMissionButton.setVisibility(View.VISIBLE);
                                                     mPauseCounter.setVisibility(View.GONE);
                                                     findViewById(R.id.pauseRecordingButton).setVisibility(View.VISIBLE);
                                                     findViewById(R.id.pausedLayout).setVisibility(View.GONE);
                                                     findViewById(R.id.resumeMissionButton).setVisibility(View.GONE);
                                                     findViewById(R.id.settingsLayout).setVisibility(View.GONE);
                                                     ((TextView) findViewById(R.id.welcome)).setText(getString(R.string.welcome));
                                                     ((TextView) findViewById(R.id.welcomeDesc)).setText(getString(R.string.welcome_desc));

                                                     findViewById(R.id.uploadLayout).setVisibility(View.VISIBLE);
                                                     findViewById(R.id.uploadingLayout).setVisibility(View.GONE);
                                                     findViewById(R.id.streamLayout).setVisibility(View.GONE);
                                                     findViewById(R.id.recBall).setVisibility(View.INVISIBLE);

                                                     mStreamSwitch.setOnCheckedChangeListener(null);
                                                     mStreamSwitch.setChecked(false);
                                                     mStreamSwitch.setOnCheckedChangeListener(mStreamListener);


                                                     Intent intent = new Intent(MainActivity.this, StreamService.class);
                                                     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                     stopService(intent);

                                                     intent = new Intent(MainActivity.this, VideoRecorderService.class);
                                                     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                     stopService(intent);

                                                     intent = new Intent(MainActivity.this, LocationService.class);
                                                     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                     stopService(intent);

                                                     mCountDownTenPaused.cancel();
                                                     mCountDownThirtyPaused.cancel();

                                                     HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.LOGGED, Globals.getUserLogin(MainActivity.this));

                                                     Globals.setDirectorySize(getApplicationContext(), FileUtils.getDirectorySize());
                                                     ((TextView) findViewById(R.id.uploadData)).setText(getString(R.string.upload_data_size, formatMegaBytes(getDirectorySize(getApplicationContext()))));
                                                 }


                                             }


        );




        ((Button)findViewById(R.id.uploadButton)).setOnClickListener(new View.OnClickListener() {
                                                                         @Override
                                                                         public void onClick(View view) {
                                                                             if (NetworkUtils.canUpload(getApplicationContext(), getIntent())) {
                                                                                 findViewById(R.id.uploadLayout).setVisibility(View.GONE);
                                                                                 findViewById(R.id.uploadingLayout).setVisibility(View.VISIBLE);
                                                                                 findViewById(R.id.streamLayout).setVisibility(View.GONE);

                                                                                 Intent intent = new Intent(MainActivity.this, UploadService.class);
                                                                                 intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                                                 startService(intent);

                                                                                 HistoryUtils.registerHistory(getApplicationContext(), State.LOGGED, State.UPLOADING, Globals.getUserLogin(MainActivity.this));
                                                                             } else {
                                                                                 Toast.makeText(getApplicationContext(), getString(R.string.upload_disabled), Toast.LENGTH_LONG).show();
                                                                             }
                                                                         }
                                                                     }
        );

        ((ImageView)findViewById(R.id.uploadCancelButton)).setOnClickListener(new View.OnClickListener() {
                                                                                  @Override
                                                                                  public void onClick(View view) {
                                                                                      stopUploading();
                                                                                  }
                                                                              }
        );


        mPauseRecordingButton.setOnClickListener(new View.OnClickListener()
                                                 {
                                                     @Override
                                                     public void onClick(View view) {
                                                         mPauseRecordingButton.setVisibility(View.GONE);
                                                         findViewById(R.id.pausedLayout).setVisibility(View.VISIBLE);
                                                     }
                                                 }
        );

        ((Button)findViewById(R.id.tenMinutesButton)).
                setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           startPausedCountdown();
                                           mCountDownTenPaused.start();
                                       }
                                   }
                );

        ((Button)findViewById(R.id.thirtyMinutesButton)).
                setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           startPausedCountdown();
                                           mCountDownThirtyPaused.start();
                                       }
                                   }
                );

        mResumeMissionButton.setOnClickListener(new View.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(View view) {
                                                        resumeMission();
                                                    }
                                                }
        );
        ((Button)findViewById(R.id.pauseCancelButton)).setOnClickListener(new View.OnClickListener() {
                                                                              @Override
                                                                              public void onClick(View view) {
                                                                                  mPauseRecordingButton.setVisibility(View.VISIBLE);
                                                                                  findViewById(R.id.pausedLayout).setVisibility(View.GONE);
                                                                              }
                                                                          }
        );


        mStreamSwitch.setOnCheckedChangeListener(mStreamListener);
    }

    public void missionCompleted()
    {
        vibrate(200); //vibrate when touch a button
        talk("mission_completed");
        talk("wait");


        // Log.d("talk","mission completed...!!");

    }

    private void startPausedCountdown() {
        findViewById(R.id.pausedLayout).setVisibility(View.GONE);
        findViewById(R.id.resumeMissionButton).setVisibility(View.VISIBLE);

        Intent intent = new Intent(this, VideoRecorderService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        stopService(intent);

        intent = new Intent(this, StreamService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        stopService(intent);

        mStreamSwitch.setOnCheckedChangeListener(null);
        mStreamSwitch.setChecked(false);
        mStreamSwitch.setOnCheckedChangeListener(mStreamListener);

        HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.PAUSED, Globals.getUserLogin(getApplicationContext()));

        mPauseCounter.setVisibility(View.VISIBLE);
        findViewById(R.id.recBall).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.welcomeDesc)).setText(getString(R.string.pause_desc));
        ((TextView) findViewById(R.id.welcome)).setText(getString(R.string.pause_title));
    }

    private void resumeMission() {
        mResumeMissionButton.setVisibility(View.GONE);
        mPauseRecordingButton.setVisibility(View.VISIBLE);
        mPauseCounter.setVisibility(View.GONE);
        findViewById(R.id.recBall).setVisibility(View.VISIBLE);
        findViewById(R.id.pausedLayout).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.welcome)).setText(getString(R.string.mission_start));
        ((TextView) findViewById(R.id.welcomeDesc)).setText(getString(R.string.mission_start_desc));
        mCountDownTenPaused.cancel();
        mCountDownThirtyPaused.cancel();

        Intent intent = new Intent(MainActivity.this, VideoRecorderService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(intent);

        HistoryUtils.registerHistory(getApplicationContext(), State.PAUSED, State.RECORDING_ONLINE, Globals.getUserLogin(getApplicationContext()));

        vibrate(200); //vibrate when touch a button
    }


    private String formatCounterTime(long millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    private void updateProgressBar() {
        ((ProgressBar) findViewById(R.id.progressBar)).setProgress(Globals.getDirectoryUploadedSize(getApplicationContext()).intValue());
        ((TextView) findViewById(R.id.uploadingLabel)).setText(getString(R.string.uploading_size, formatMegaBytes(Globals.getDirectoryUploadedSize(getApplicationContext())), formatMegaBytes(getDirectorySize(getApplicationContext()))));
    }

    private void stopUploading() {
        findViewById(R.id.uploadLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.uploadingLayout).setVisibility(View.GONE);
        findViewById(R.id.streamLayout).setVisibility(View.GONE);

        Intent intent = new Intent(MainActivity.this, UploadService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        stopService(intent);

        HistoryUtils.registerHistory(getApplicationContext(), State.UPLOADING, State.LOGGED, Globals.getUserLogin(MainActivity.this));
    }

    private boolean isUploading() {
        return findViewById(R.id.uploadingLayout).getVisibility() == View.VISIBLE;
    }

    private boolean isMissionStarted() {
        return findViewById(R.id.startMissionButton).getVisibility() != View.VISIBLE;
    }

    /*
        Create a function to vibrate the cell phone in milliseconds
     */
    private void vibrate(int mili)
    {
        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for (mili) milliseconds
        v.vibrate(mili);

    }
    private void msgBox(String text)
    {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    /*
        Create a function to play mp3 songs to simulate voice response
     */
    public void talk(String text)
    {
        try{

            if (text.equals("mission_started")) {
                mySongclick = MediaPlayer.create(this, R.raw.mission_started);
            } else if (text.equals("mission_completed")) {
                mySongclick = MediaPlayer.create(this, R.raw.mission_completed);
            }

            if (text.equals("wait")) { // according some song length is necessary to wait a time
                try {
                    Thread.sleep(1500); // Sleep for one second
                    mySongclick.release(); //release the song variable
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            else
            {
                mySongclick.start();  //start song
            }

        } catch (Exception e)
        {
            msgBox("Talk - Feature not supported in your device");
        }

    }

    @Override
    protected void onDestroy() {

        Globals.clear(MainActivity.this);
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
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        //TODO needs current state?
        HistoryUtils.registerHistory(getApplicationContext(), State.LOGGED, State.NOT_LOGGED, Globals.getUserLogin(MainActivity.this));

        Globals.clear(MainActivity.this);
        stopService(new Intent(MainActivity.this, StreamService.class));
        stopService(new Intent(MainActivity.this, LocationService.class));
        stopService(new Intent(MainActivity.this, VideoRecorderService.class));
        stopService(new Intent(MainActivity.this, UploadService.class));
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        MainActivity.this.finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(UploadService.UPLOAD_PROGRESS_ACTION);
        filter.addAction(UploadService.CANCEL_UPLOAD_ACTION);
        filter.addAction(UploadService.COMPLETED_UPLOAD_ACTION);
        filter.addAction(GcmIntentService.START_STREAMING_ACTION);
        filter.addAction(GcmIntentService.STOP_STREAMING_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), filter);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();

        //Log.d("state","onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Globals.getAccessToken(getApplicationContext()) == null) {
            logout();
        }
        updateProgressBar();
        WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()){
            showSettingsAlert();
        }
        //Log.d("state","onResume");
    }

    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);

        Resources res = getResources();
        alertDialog.setTitle(res.getString(R.string.wifi_dialog_title));
        alertDialog.setMessage(res.getString(R.string.wifi_dialog_msg));

        alertDialog.setPositiveButton(res.getText(R.string.settings_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }
        });

        alertDialog.setNegativeButton(res.getText(R.string.cancel_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    private class CountDownPausedTimer extends CountDownTimer {
        public CountDownPausedTimer(int millisInFuture, int countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public void onTick(long millisUntilFinished) {
            mPauseCounter.setText(formatCounterTime(millisUntilFinished));
        }

        public void onFinish() {
            resumeMission();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    /*
        Detect volumen key down event
     */
        switch(keyCode){
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                //Log.d("Keydown","Volumen Down pressed");
                //vibrate(500);
                //mStarMissionButton.callOnClick();

                //return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void flagOcurrence()
    {
        //Log.d("flag","flag to database!");
    }
}
