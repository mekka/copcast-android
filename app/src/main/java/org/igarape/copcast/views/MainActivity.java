package org.igarape.copcast.views;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
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
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.alexbbb.uploadservice.UploadService;

import org.igarape.copcast.R;
import org.igarape.copcast.receiver.AlarmHeartBeatReceiver;
import org.igarape.copcast.receiver.BatteryReceiver;
import org.igarape.copcast.service.CopcastGcmListenerService;
import org.igarape.copcast.service.LocationService;
import org.igarape.copcast.service.StreamService;
import org.igarape.copcast.service.VideoRecorderService;
import org.igarape.copcast.state.IncidentFlagState;
import org.igarape.copcast.state.State;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HistoryUtils;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.IncidentUtils;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.utils.UploadManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.igarape.copcast.utils.FileUtils.formatMegaBytes;
import static org.igarape.copcast.utils.Globals.getDirectorySize;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();
    private BroadcastReceiver broadcastReceiver;
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
    private UploadManager uploadManager;
    private Long first_keydown;
    private final int FLAG_TRIGGER_WAIT_TIME = 1000;
    private ProgressDialog pDialog;

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
                //When toogling, the stopped service will start the other one
                if (isChecked) {
                    HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.STREAMING, Globals.getUserLogin(MainActivity.this), null);

                    Intent intentAux = new Intent(MainActivity.this, VideoRecorderService.class);
                    intentAux.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    stopService(intentAux);
                } else {
                    HistoryUtils.registerHistory(getApplicationContext(), State.STREAMING, State.RECORDING_ONLINE, Globals.getUserLogin(MainActivity.this), null);

                    Intent intentAux = new Intent(MainActivity.this, StreamService.class);
                    intentAux.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    stopService(intentAux);

                }
            }
        };

        ActionBar ab = getActionBar(); //needs  import android.app.ActionBar;
        ab.setTitle(Globals.getUserName(getApplicationContext()));
        ab.setSubtitle(Globals.getUserLogin(this));
        FileUtils.init(getApplicationContext());

        broadcastReceiver
                = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BatteryReceiver.BATTERY_LOW_MESSAGE)) {
                    stopUploading();
                    stopAlarmReceiver();
                } else if (intent.getAction().equals(BatteryReceiver.BATTERY_OKAY_MESSAGE)) {
                    //TODO check if it's already running. if not, start startAlarmLocationReceiver()
                }
                else if (intent.getAction().equals(UploadManager.UPLOAD_FAILED_ACTION)) {
                    if (uploadManager != null) {
                        uploadManager.runUpload();
                    }
                }
                else if (intent.getAction().equals(UploadManager.UPLOAD_PROGRESS_ACTION)) {
                    updateProgressBar();
                    if (uploadManager != null) {
                        uploadManager.deleteVideoFile();
                        uploadManager.runUpload();
                    }

                } else if (intent.getAction().equals(CopcastGcmListenerService.START_STREAMING_ACTION)) {
                    if (isMissionStarted()) {
                        mStreamSwitch.setChecked(true);
                    }
                } else if (intent.getAction().equals(CopcastGcmListenerService.STOP_STREAMING_ACTION)) {
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
                    uploadManager = null;
                    if (intent.getAction().equals(UploadManager.CANCEL_UPLOAD_ACTION)) {
                        Toast.makeText(getApplicationContext(), getString(R.string.upload_stopped), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.upload_completed), Toast.LENGTH_LONG).show();
                    }
                    resetStatusUpload();
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

        resetStatusUpload();

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

                HistoryUtils.registerHistory(getApplicationContext(), State.LOGGED, State.RECORDING_ONLINE, Globals.getUserLogin(MainActivity.this), null);

                startAlarmLocationReceiver();
            }


        });

        mEndMissionButton.setOnClickListener(new View.OnClickListener()

                                             {
                                                 @Override
                                                 public void onClick(View view) {

                                                     if (isStreaming()) {
                                                         HistoryUtils.registerHistory(getApplicationContext(), State.STREAMING, State.LOGGED, Globals.getUserLogin(MainActivity.this), null);
                                                     } else if (isRecording()){
                                                         HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.LOGGED, Globals.getUserLogin(MainActivity.this), null);
                                                     } else if (isPaused()){
                                                         HistoryUtils.registerHistory(getApplicationContext(), State.PAUSED, State.LOGGED, Globals.getUserLogin(MainActivity.this), null);
                                                     }

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

                                                     //reset upload values
                                                     resetStatusUpload();

                                                     stopAlarmReceiver();
                                                 }


                                             }


        );


        ((Button) findViewById(R.id.uploadButton)).setOnClickListener(new View.OnClickListener() {
                                                                          @Override
                                                                          public void onClick(View view) {
                                                                              if (NetworkUtils.canUpload(getApplicationContext(), getIntent())) {
                                                                                  findViewById(R.id.uploadLayout).setVisibility(View.GONE);
                                                                                  findViewById(R.id.uploadingLayout).setVisibility(View.VISIBLE);
                                                                                  findViewById(R.id.streamLayout).setVisibility(View.GONE);

                                                                                  uploadManager = new UploadManager(getApplicationContext());
                                                                                  uploadManager.runUpload();

                                                                                  HistoryUtils.registerHistory(getApplicationContext(), State.LOGGED, State.UPLOADING, Globals.getUserLogin(MainActivity.this), null);
                                                                                  updateProgressBar();
                                                                              } else {
                                                                                  Toast.makeText(getApplicationContext(), getString(R.string.upload_disabled), Toast.LENGTH_LONG).show();
                                                                              }
                                                                          }
                                                                      }
        );

        ((ImageView) findViewById(R.id.uploadCancelButton)).setOnClickListener(new View.OnClickListener() {
                                                                                   @Override
                                                                                   public void onClick(View view) {
                                                                                       stopUploading();
                                                                                   }
                                                                               }
        );


        mPauseRecordingButton.setOnClickListener(new View.OnClickListener() {
                                                     @Override
                                                     public void onClick(View view) {
                                                         mPauseRecordingButton.setVisibility(View.GONE);
                                                         findViewById(R.id.pausedLayout).setVisibility(View.VISIBLE);
                                                     }
                                                 }
        );

        ((Button) findViewById(R.id.tenMinutesButton)).
                setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           startPausedCountdown();
                                           mCountDownTenPaused.start();
                                       }
                                   }
                );

        ((Button) findViewById(R.id.thirtyMinutesButton)).
                setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           startPausedCountdown();
                                           mCountDownThirtyPaused.start();
                                       }
                                   }
                );

        mResumeMissionButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        resumeMission();
                                                    }
                                                }
        );
        ((Button) findViewById(R.id.pauseCancelButton)).setOnClickListener(new View.OnClickListener() {
                                                                               @Override
                                                                               public void onClick(View view) {
                                                                                   mPauseRecordingButton.setVisibility(View.VISIBLE);
                                                                                   findViewById(R.id.pausedLayout).setVisibility(View.GONE);
                                                                               }
                                                                           }
        );

        mStreamSwitch.setOnCheckedChangeListener(mStreamListener);
    }

    private void resetStatusUpload() {
        Globals.setDirectorySize(getApplicationContext(), FileUtils.getDirectorySize());

        ((ProgressBar) findViewById(R.id.progressBar)).setMax(getDirectorySize(getApplicationContext()).intValue());

        ((TextView) findViewById(R.id.uploadingLabel)).setText(getString(R.string.uploading_size, 0, formatMegaBytes(getDirectorySize(getApplicationContext()))));
        ((TextView) findViewById(R.id.uploadData)).setText(getString(R.string.upload_data_size, formatMegaBytes(getDirectorySize(getApplicationContext()))));
    }

    private void stopAlarmReceiver(){
        Intent intent = new Intent(this, AlarmHeartBeatReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    private void startAlarmLocationReceiver() {
        /**
         * AlarmManager...wakes every 15 sec.
         */
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmHeartBeatReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Globals.GPS_REPEAT_TIME, pending);



        intent = new Intent(this, BatteryReceiver.class);
        pending = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Globals.BATTERY_REPEAT_TIME, pending);

    }

    public void missionCompleted()
    {
        vibrate(200); //vibrate when touch a button
        talk("mission_completed");
        talk("wait");


        // Log.d("talk","mission completed...!!");

    }

    private void startPausedCountdown() {
        if (isStreaming()) {
            HistoryUtils.registerHistory(getApplicationContext(), State.STREAMING, State.PAUSED, Globals.getUserLogin(getApplicationContext()));
        } else {
            HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.PAUSED, Globals.getUserLogin(getApplicationContext()));
        }

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
        mStreamSwitch.setEnabled(false);


        mPauseCounter.setVisibility(View.VISIBLE);
        findViewById(R.id.recBall).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.welcomeDesc)).setText(getString(R.string.pause_desc));
        ((TextView) findViewById(R.id.welcome)).setText(getString(R.string.pause_title));
    }

    private boolean isStreaming() {
        return mStreamSwitch.isChecked();
    }
    private void resumeMission() {
        HistoryUtils.registerHistory(getApplicationContext(), State.PAUSED, State.RECORDING_ONLINE, Globals.getUserLogin(getApplicationContext()), null);

        mStreamSwitch.setEnabled(true);
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
        resetStatusUpload();

        findViewById(R.id.uploadLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.uploadingLayout).setVisibility(View.GONE);
        findViewById(R.id.streamLayout).setVisibility(View.GONE);
        Intent intent = new Intent(MainActivity.this, UploadService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        stopService(intent);
        uploadManager = null;

        HistoryUtils.registerHistory(getApplicationContext(), State.UPLOADING, State.LOGGED, Globals.getUserLogin(MainActivity.this), null);
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
        killServices();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        Globals.clear(MainActivity.this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        Log.d(TAG, "Menu inflated!");
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
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_playback) {
            List<String> videos = FileUtils.getVideoPathList(Globals.getUserLogin(MainActivity.this));

            if (videos.size() == 0) {
                Log.w(TAG, "no video available for playback");
                Toast.makeText(MainActivity.this, getResources().getString(R.string.no_video_message), Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, videos.size()+ " videos available for playback");
                Intent i = new Intent(this, PlayerActivity.class);
                startActivity(i);
            }
            return true;
        } else if (id == R.id.action_incident_form) {
            Log.d(TAG, "IncidentForm Open Menu!");
            pDialog = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.please_hold), true);

            Intent i = new Intent(this, FormIncidentReportActivity.class);
            startActivity(i);
            return true;

        } else if (id == R.id.action_logout) {
            logout(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout(String reason) {
        //TODO needs current state?
        if (isStreaming()) {
            HistoryUtils.registerHistory(getApplicationContext(), State.STREAMING, State.NOT_LOGGED, Globals.getUserLogin(MainActivity.this), null);
        } else if (isRecording()){
            HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.NOT_LOGGED, Globals.getUserLogin(MainActivity.this), null);
        } else if (isPaused()){
            HistoryUtils.registerHistory(getApplicationContext(), State.PAUSED, State.NOT_LOGGED, Globals.getUserLogin(MainActivity.this), null);
        } else {
            HistoryUtils.registerHistory(getApplicationContext(), State.LOGGED, State.NOT_LOGGED, Globals.getUserLogin(MainActivity.this), null);
        }
        Globals.clear(MainActivity.this);
        killServices();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        if (reason != null)
            intent.putExtra("reason", reason);
        startActivity(intent);
        MainActivity.this.finish();
    }

    private boolean isPaused() {
        return mResumeMissionButton.getVisibility() == View.VISIBLE;
    }

    private boolean isRecording() {
        return !mStreamSwitch.isChecked() && mPauseRecordingButton.getVisibility() == View.VISIBLE;
    }

    private void killServices() {
        stopService(new Intent(MainActivity.this, StreamService.class));
        stopService(new Intent(MainActivity.this, LocationService.class));
        stopService(new Intent(MainActivity.this, VideoRecorderService.class));
        stopService(new Intent(MainActivity.this, UploadService.class));

        stopAlarmReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(UploadManager.UPLOAD_PROGRESS_ACTION);
        filter.addAction(UploadManager.CANCEL_UPLOAD_ACTION);
        filter.addAction(UploadManager.UPLOAD_FAILED_ACTION);
        filter.addAction(UploadManager.COMPLETED_UPLOAD_ACTION);
        filter.addAction(CopcastGcmListenerService.START_STREAMING_ACTION);
        filter.addAction(CopcastGcmListenerService.STOP_STREAMING_ACTION);
        filter.addAction(BatteryReceiver.BATTERY_LOW_MESSAGE);
        filter.addAction(BatteryReceiver.BATTERY_OKAY_MESSAGE);
        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver), filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        //Log.d("state","onStop");
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Globals.setRotation(getWindowManager().getDefaultDisplay().getRotation());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Globals.getAccessToken(getApplicationContext()) == null) {
            logout(getString(R.string.invalid_token));
        }
        if (pDialog != null){
            pDialog.dismiss();
        }
        NetworkUtils.get(getApplicationContext(), "/users/me", new HttpResponseCallback() {
            @Override
            public void unauthorized() {
                logout(getString(R.string.token_expired));
            }

            @Override
            public void failure(int statusCode) {}

            @Override
            public void noConnection() {}

            @Override
            public void badConnection() {}

            @Override
            public void badRequest() {}

            @Override
            public void badResponse() {}
        });

        Globals.setRotation(getWindowManager().getDefaultDisplay().getRotation());
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

        switch(keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:

                if (first_keydown == null) {
                    first_keydown = System.currentTimeMillis(); // first keydown sets start time
                } else if (System.currentTimeMillis() - first_keydown > FLAG_TRIGGER_WAIT_TIME) {
                    if (Globals.getIncidentFlag() == IncidentFlagState.NOT_FLAGGED) {

                        //wait a bit
                        first_keydown = null; //reset state

                        if (!VideoRecorderService.serviceRunning) {

                            Globals.setIncidentFlag(IncidentFlagState.FLAG_PENDING);
                            Log.d(TAG, "Flag incident scheduled");

                            if (((TextView) findViewById(R.id.welcome)).getText().equals(getString(R.string.pause_title))) {
                                Log.d(TAG, "resuming mission by forced incident");
                                mResumeMissionButton.performClick();
                            } else {
                                Log.d(TAG, "starting mission by forced incident");
                                mStarMissionButton.performClick();
                            }
                        } else {
                            Globals.setIncidentFlag(IncidentFlagState.FLAGGED);
                            Log.d(TAG, "Flag incident immediately");
                            IncidentUtils.registerIncident(getApplicationContext(), Globals.getCurrentVideoPath());
                            Toast.makeText(this, getResources().getString(R.string.registered_incident), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d(TAG, "Incident already reported. Skipping");
                    }
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                onBackPressed();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);

        Resources res = getResources();
        alertDialog.setTitle(res.getString(R.string.confirmation_tittle));
        alertDialog.setMessage(res.getString(R.string.confirmation_msg));

        alertDialog.setPositiveButton(res.getText(R.string.confirmation_button_positive), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.this.finish();
            }
        });
        alertDialog.setNegativeButton(res.getText(R.string.confirmation_button_negative), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }


}
