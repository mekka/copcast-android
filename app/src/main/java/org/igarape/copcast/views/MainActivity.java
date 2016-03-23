package org.igarape.copcast.views;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
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
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.igarape.copcast.BuildConfig;
import org.igarape.copcast.R;
import org.igarape.copcast.receiver.BatteryReceiver;
import org.igarape.copcast.service.CopcastGcmListenerService;
import org.igarape.copcast.service.LocationService;
import org.igarape.copcast.service.VideoRecorderService;
import org.igarape.copcast.service.upload.UploadService;
import org.igarape.copcast.state.IncidentFlagState;
import org.igarape.copcast.state.NetworkState;
import org.igarape.copcast.state.State;
import org.igarape.copcast.state.UploadServiceEvent;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HistoryUtils;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.IncidentUtils;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.util.Promise;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.igarape.copcast.utils.FileUtils.formatMegaBytes;
import static org.igarape.copcast.utils.Globals.getDirectorySize;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();
    private static int MINUTES_30 = 1800000;
    private static int MINUTES_10 = 600000;
    private BroadcastReceiver broadcastReceiver;
    private BroadcastReceiver uploadFeedbackReceiver;
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
    //private UploadManager uploadManager;
    private Long first_keydown;
    private final int FLAG_TRIGGER_WAIT_TIME = 1000;
    private ProgressDialog pDialog;
    private VideoRecorderService videoRecorderService;
    boolean mBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            VideoRecorderService.LocalBinder binder = (VideoRecorderService.LocalBinder) service;
            videoRecorderService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar ab = getActionBar(); //needs  import android.app.ActionBar;
        ab.setTitle(Globals.getUserName(getApplicationContext()));
        ab.setSubtitle(Globals.getUserLogin(this));
        FileUtils.init(getApplicationContext());

        mStreamSwitch = (Switch) findViewById(R.id.streamSwitch);
        mStarMissionButton = (Button) findViewById(R.id.startMissionButton);
        mEndMissionButton = (Button) findViewById(R.id.endMissionButton);
        mPauseRecordingButton = (Button) findViewById(R.id.pauseRecordingButton);
        mResumeMissionButton = (Button) findViewById(R.id.resumeMissionButton);
        mPauseCounter = (TextView) findViewById(R.id.pauseCounter);

        mCountDownThirtyPaused = new CountDownPausedTimer(MINUTES_30, 1000);
        mCountDownTenPaused = new CountDownPausedTimer(MINUTES_10, 1000);

        mStreamListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Globals.setToggling(true);
                //When toogling, the stopped service will start the other one
                if (isChecked) {
                    HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.STREAMING);

                    videoRecorderService.startStreaming();
                } else {
                    HistoryUtils.registerHistory(getApplicationContext(), State.STREAMING, State.RECORDING_ONLINE);
                    videoRecorderService.stopStreaming();


                }
            }
        };

        broadcastReceiver
                = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ILog.d(TAG, "received generic");
                if (intent.getAction().equals(BatteryReceiver.BATTERY_LOW_MESSAGE)) {
                    stopUploading();
                    stopAlarmReceiver();
                } else if (intent.getAction().equals(BatteryReceiver.BATTERY_OKAY_MESSAGE)) {
                } else if (intent.getAction().equals(VideoRecorderService.STARTED_STREAMING)) {
                    Log.e(TAG, "EVENTOO!!");
                    mStreamSwitch.setChecked(true);
                } else if (intent.getAction().equals(VideoRecorderService.STOPPED_STREAMING)) {
                    mStreamSwitch.setChecked(false);
                }
            }
        };

        uploadFeedbackReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(UploadService.UPLOAD_FEEDBACK_ACTION)) {
                    if (intent.getExtras() != null && intent.getExtras().get("event")!=null) {
                        UploadServiceEvent use = (UploadServiceEvent) intent.getExtras().get("event");

                        if (!use.getRunning())
                            resetStatusUpload();
                        else
                            displayUploadBar();

                        switch (use) {
                            case RUNNING:
                                ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
                                int prog = (int) intent.getExtras().getLong("uploadedBytes");
                                p.setProgress(prog);
                                ((TextView) findViewById(R.id.uploadingLabel)).setText(getString(R.string.uploading_size, formatMegaBytes((long) prog), formatMegaBytes((long)p.getMax())));
                                break;
                            case STARTED:
                                HistoryUtils.registerHistory(getApplicationContext(), State.LOGGED, State.UPLOADING);
                                break;
                            case ABORTED_NO_NETWORK:
                                Toast.makeText(getApplicationContext(), getString(R.string.network_state_no_network), Toast.LENGTH_LONG).show();
                                ILog.d(TAG, "No network available");
                                break;
                            case FAILED:
                                Toast.makeText(getApplicationContext(), getString(R.string.upload_error), Toast.LENGTH_LONG).show();
                                break;
                            case FINISHED:
                                Toast.makeText(getApplicationContext(), getString(R.string.upload_completed), Toast.LENGTH_LONG).show();
                                break;
                            case ABORTED_USER:
                                ILog.d(TAG, "user aborted upload");
                                break;
                            case NO_DATA:
                                Toast.makeText(getApplicationContext(), getString(R.string.upload_no_data), Toast.LENGTH_LONG).show();
                                break;
                            default:
                                ILog.e(TAG, "Unexpected feedback status: "+use);
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(CopcastGcmListenerService.START_STREAMING_ACTION);
        filter.addAction(CopcastGcmListenerService.STOP_STREAMING_ACTION);
        filter.addAction(BatteryReceiver.BATTERY_LOW_MESSAGE);
        filter.addAction(BatteryReceiver.BATTERY_OKAY_MESSAGE);
        filter.addAction(VideoRecorderService.STARTED_STREAMING);
        filter.addAction(VideoRecorderService.STOPPED_STREAMING);
        Log.d(TAG, broadcastReceiver.toString());
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
        IntentFilter filterUploadFeedback = new IntentFilter(UploadService.UPLOAD_FEEDBACK_ACTION);
        Log.d(TAG, uploadFeedbackReceiver.toString());
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadFeedbackReceiver, filterUploadFeedback);


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
            public void forbidden() {

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
                        ActionBar actionBar = getActionBar();
                        if (actionBar != null) {
                            actionBar.setIcon(new BitmapDrawable(MainActivity.this.getResources(), bm));
                        }
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

                vibrate(200);
                talk("mission_started");

                findViewById(R.id.settingsLayout).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.welcome)).setText(getString(R.string.mission_start));
                ((TextView) findViewById(R.id.welcomeDesc)).setText(getString(R.string.mission_start_desc));
                findViewById(R.id.uploadLayout).setVisibility(View.GONE);
                findViewById(R.id.uploadingLayout).setVisibility(View.GONE);
                findViewById(R.id.streamLayout).setVisibility(View.VISIBLE);
                findViewById(R.id.recBall).setVisibility(View.VISIBLE);

                Intent intent = new Intent(MainActivity.this, VideoRecorderService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startService(intent);

                intent = new Intent(MainActivity.this, LocationService.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startService(intent);

                HistoryUtils.registerHistory(getApplicationContext(), State.LOGGED, State.RECORDING_ONLINE);

                startAlarmBatteryReceiver();
            }


        });

        mEndMissionButton.setOnClickListener(new View.OnClickListener()

                                             {
                                                 @Override
                                                 public void onClick(View view) {


                                                     final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                                                     progressDialog.setTitle(getString(R.string.please_hold));
                                                     progressDialog.setMessage(getString(R.string.storing_video));
                                                     progressDialog.show();
                                                     vibrate(100);

                                                     videoRecorderService.stop(new Promise() {
                                                         @Override
                                                         public void success(Object payload) {

                                                             runOnUiThread(new Runnable() {
                                                                 @Override
                                                                 public void run() {

                                                                     if (isStreaming()) {
                                                                         HistoryUtils.registerHistory(getApplicationContext(), State.STREAMING, State.LOGGED);
                                                                     } else if (isRecording()) {
                                                                         HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.LOGGED);
                                                                     } else if (isPaused()) {
                                                                         HistoryUtils.registerHistory(getApplicationContext(), State.PAUSED, State.LOGGED);
                                                                     }

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

                                                                     Intent intent = new Intent(MainActivity.this, VideoRecorderService.class);
                                                                     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                                                     intent = new Intent(MainActivity.this, LocationService.class);
                                                                     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                                     stopService(intent);

                                                                     mCountDownTenPaused.cancel();
                                                                     mCountDownThirtyPaused.cancel();
                                                                     mPauseCounter.setText("");

                                                                     //reset upload values
                                                                     resetStatusUpload();
                                                                     stopAlarmReceiver();
                                                                     missionCompleted();
                                                                     progressDialog.dismiss();
                                                                 }
                                                             });
                                                         }

                                                         @Override
                                                         public void failure(Exception exception) {
                                                             Log.e(TAG, "webrecorder stop error", exception);
                                                             this.success();
                                                         }
                                                     });
                                                 }
                                             }


        );


        findViewById(R.id.uploadButton).setOnClickListener(new View.OnClickListener() {

                                                               @Override
                                                               public void onClick(View view) {
                                                                   NetworkState networkState = NetworkUtils.checkUploadState(getApplicationContext());
                                                                   if (networkState == NetworkState.NETWORK_OK) {
                                                                       resetStatusUpload(); // prevent ghost information from appearing
                                                                       UploadService.doUpload(getApplicationContext());
                                                                   } else {
                                                                       int msgid = -1;
                                                                       switch (networkState) {
                                                                           case NO_NETWORK:
                                                                               msgid = R.string.network_state_no_network;
                                                                               break;
                                                                           case WIFI_REQUIRED:
                                                                               msgid = R.string.network_state_wifi_required;
                                                                               break;
                                                                           case NOT_CHARGING:
                                                                               msgid = R.string.network_state_not_charging;
                                                                               break;
                                                                       }
                                                                       if (msgid == -1)
                                                                           Log.e(TAG, "Unexpected network state: " + networkState.name());
                                                                       else
                                                                           Toast.makeText(getApplicationContext(), getString(msgid), Toast.LENGTH_LONG).show();
                                                                   }
                                                               }
                                                           }
        );

        findViewById(R.id.uploadCancelButton).setOnClickListener(new View.OnClickListener() {
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

        findViewById(R.id.tenMinutesButton).
                setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           startPausedCountdown();
                                           mCountDownTenPaused.start();
                                       }
                                   }
                );

        findViewById(R.id.thirtyMinutesButton).
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
        findViewById(R.id.pauseCancelButton).setOnClickListener(new View.OnClickListener() {

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
        ((ProgressBar) findViewById(R.id.progressBar)).setProgress(0);

        ((TextView) findViewById(R.id.uploadingLabel)).setText(getString(R.string.uploading_size, formatMegaBytes(0L), formatMegaBytes(getDirectorySize(getApplicationContext()))));
        ((TextView) findViewById(R.id.uploadData)).setText(getString(R.string.upload_data_size, formatMegaBytes(getDirectorySize(getApplicationContext()))));
        displayUploadButton();
    }

    private void startAlarmBatteryReceiver() {

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, BatteryReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Globals.BATTERY_REPEAT_TIME, pending);

    }

    public void missionCompleted()
    {
        vibrate(200); //vibrate when touch a button
        talk("mission_completed");
        talk("wait");
    }

    private void startPausedCountdown() {
        if (isStreaming()) {
            HistoryUtils.registerHistory(getApplicationContext(), State.STREAMING, State.PAUSED);
        } else {
            HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.PAUSED);
        }

        findViewById(R.id.pausedLayout).setVisibility(View.GONE);
        findViewById(R.id.resumeMissionButton).setVisibility(View.VISIBLE);

        unbindService(mConnection);
        Intent intent = new Intent(this, VideoRecorderService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        stopService(intent);

//
//        intent = new Intent(this, StreamService.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        stopService(intent);

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
        HistoryUtils.registerHistory(getApplicationContext(), State.PAUSED, State.RECORDING_ONLINE);

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
        mPauseCounter.setText("");

        Intent intent = new Intent(MainActivity.this, VideoRecorderService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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

    private void stopUploading() {
        findViewById(R.id.uploadCancelButton).setVisibility(View.INVISIBLE);
        UploadService.stop(getApplicationContext());

        HistoryUtils.registerHistory(getApplicationContext(), State.UPLOADING, State.LOGGED);
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
        v.vibrate(mili);

    }
    private void msgBox(int textId)
    {
        Toast.makeText(getApplicationContext(), textId, Toast.LENGTH_LONG).show();
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
            msgBox(R.string.not_supported);
        }

    }

    @Override
    protected void onDestroy() {
        killServices();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadFeedbackReceiver);
        Globals.clear(MainActivity.this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem item_incident_form = menu.findItem(R.id.action_incident_form);
        if (item_incident_form != null)
            item_incident_form.setVisible (BuildConfig.HAS_INCIDENT_FORM);

        MenuItem item_playback = menu.findItem(R.id.action_playback);
        if (item_playback != null)
            item_playback.setVisible (BuildConfig.HAS_VIDEO_PLAYBACK);

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
            HistoryUtils.registerHistory(getApplicationContext(), State.STREAMING, State.NOT_LOGGED);
        } else if (isRecording()){
            HistoryUtils.registerHistory(getApplicationContext(), State.RECORDING_ONLINE, State.NOT_LOGGED);
        } else if (isPaused()){
            HistoryUtils.registerHistory(getApplicationContext(), State.PAUSED, State.NOT_LOGGED);
        } else {
            HistoryUtils.registerHistory(getApplicationContext(), State.LOGGED, State.NOT_LOGGED);
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
        stopService(new Intent(MainActivity.this, LocationService.class));
        if (mBound)
            unbindService(mConnection);
        stopService(new Intent(MainActivity.this, VideoRecorderService.class));
        stopService(new Intent(MainActivity.this, UploadService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        NetworkUtils.get(getApplicationContext(), "/users/me", new HttpResponseCallback() {
            @Override
            public void unauthorized() {
                logout(getString(R.string.token_expired));
            }

            @Override
            public void failure(int statusCode) {
            }

            @Override
            public void noConnection() {
            }

            @Override
            public void forbidden() {}

            @Override
            public void badRequest() {}

            @Override
            public void badRequest() {
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BatteryReceiver.BATTERY_LOW_MESSAGE)) {
                    stopUploading();
                } else if (intent.getAction().equals(BatteryReceiver.BATTERY_OKAY_MESSAGE)) {
                    //TODO check if it's already running. if not, start startAlarmBatteryReceiver()
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
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
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
            pDialog = null;
        }

        Globals.setRotation(getWindowManager().getDefaultDisplay().getRotation());
        WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()){
            showSettingsAlert();
        }
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
            mPauseCounter.setText("");
            resumeMission();
        }
    }

    private class CountDownStreamTimer extends CountDownTimer {

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public CountDownStreamTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            mStreamSwitch.setChecked(false);
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

    private void displayUploadButton() {
        findViewById(R.id.uploadLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.uploadingLayout).setVisibility(View.GONE);
        findViewById(R.id.streamLayout).setVisibility(View.GONE);
    }

    private void displayUploadBar() {
        findViewById(R.id.uploadCancelButton).setVisibility(View.VISIBLE);
        findViewById(R.id.uploadLayout).setVisibility(View.GONE);
        findViewById(R.id.uploadingLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.streamLayout).setVisibility(View.GONE);
    }

}
