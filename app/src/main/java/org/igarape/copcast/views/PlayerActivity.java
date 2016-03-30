package org.igarape.copcast.views;

import android.app.Activity;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import org.igarape.copcast.R;
import org.igarape.copcast.state.State;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HistoryUtils;
import org.igarape.copcast.utils.IncidentUtils;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;


class VideoEntry {

    public String path;
    public String video;
    public boolean incident;

    public VideoEntry(String path, boolean incident) {
        this.path = path;
        this.video = pathToTimestamp(path);
        this.incident = incident;
    }

    private String pathToTimestamp(String path) {
        String[] tokens = path.split("/");
        tokens = tokens[tokens.length-1].replace(".mp4", "").split("_");
        return tokens[0].replace("-","/")+" "+tokens[1].replace("-",":");

    }

    @Override
    public String toString() {
        return this.video;
    }

}

class VideoEntryAdapter extends BaseAdapter {

    private List<VideoEntry> videoEntries;
    private int pos = -1;

    public VideoEntryAdapter(List<VideoEntry> videoEntries) {
        this.videoEntries = videoEntries;
    }

    public void setCurrentVideo(int pos) {
        this.pos = pos;
    }

    @Override
    public int getCount() {
        return videoEntries.size();
    }

    @Override
    public VideoEntry getItem(int position) {
        return videoEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater =  LayoutInflater.from(parent.getContext());
        convertView = inflater.inflate(R.layout.my_list_item, parent, false);

        VideoEntry ve = videoEntries.get(position);
        TextView hora = (TextView) convertView.findViewById(R.id.listItemText);
        TextView dia = (TextView) convertView.findViewById(R.id.listItemText2);
        ImageView issue = (ImageView) convertView.findViewById(R.id.issue_icon);
        String[] tokens = ve.video.split(" ");
        dia.setText(tokens[0]);
        hora.setText(tokens[1]);

        Log.d(">>", "Pos:"+position+" / selected:"+pos );

        if (pos == position) {
            ImageView img = (ImageView) convertView.findViewById(R.id.imageView1);
            img.setImageResource(R.drawable.pause);
        }

        if (ve.incident)
            issue.setVisibility(View.VISIBLE);
        else
            issue.setVisibility(View.INVISIBLE);

        return convertView;
    }
}


public class PlayerActivity extends Activity {

    private static final String TAG = PlayerActivity.class.getName();
    private List<String> incidentVideoList = new ArrayList<>();
    private ArrayList<VideoEntry> videoList = new ArrayList<>();
    private ListView listView;
    private VideoView videoView;
    private ProgressBar mProgressBar;
    private long timemillis, now = 0;
    private int mainWidth, mainHeight = 0;
    private AsyncTask task;
    private boolean paused = false;
    private int videoPosition = -1;
    private ImageButton fullScreen;
    private ImageButton restoreScreen;
    private VideoEntryAdapter adap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        List<String> videoPathList;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback_video);

        listView = (ListView) findViewById(R.id.videoListView);
        videoView = (VideoView) findViewById(R.id.videoView);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar2);
        fullScreen = (ImageButton) findViewById(R.id.fullScreenBtn);
        restoreScreen = (ImageButton) findViewById(R.id.restoreScreenBtn);

        final String userLogin = Globals.getUserLogin(getApplicationContext());

        final android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) videoView.getLayoutParams();

        //try Globals default, otherwise set to LOW
        CamcorderProfile camcorderProfile;
        try {
            camcorderProfile = CamcorderProfile.get(Globals.appCamcoderProfile);
        } catch (RuntimeException ex) {
            camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        }
        mainWidth = params.height * camcorderProfile.videoFrameHeight / camcorderProfile.videoFrameWidth;
        params.width = mainWidth;
        mainHeight = params.height;
        Log.d(TAG, params.width + " x " + params.height);
        videoView.setLayoutParams(params);
        android.widget.LinearLayout.LayoutParams params2 = (android.widget.LinearLayout.LayoutParams) mProgressBar.getLayoutParams();
        params2.width = mainWidth;
        mProgressBar.setLayoutParams(params2);
        videoPathList = FileUtils.getVideoPathList(userLogin);

        fullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "Duração: "+videoView.getDuration());

                if (videoView.getDuration() < 0)
                    return;

                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) videoView.getLayoutParams();
                android.widget.LinearLayout.LayoutParams params2 = (android.widget.LinearLayout.LayoutParams) mProgressBar.getLayoutParams();
                params.width = metrics.widthPixels;
                params.height = metrics.heightPixels;
                params2.width = metrics.widthPixels;
                params2.height = 20;
                params.leftMargin = 0;
                Log.d(TAG, "setting FULLSCREEN");
                listView.setVisibility(View.INVISIBLE);
                videoView.setLayoutParams(params);
                mProgressBar.setLayoutParams(params2);
                fullScreen.setVisibility(View.GONE);
                restoreScreen.setVisibility(View.VISIBLE);
            }
        });


        restoreScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) videoView.getLayoutParams();
                android.widget.LinearLayout.LayoutParams params2 = (android.widget.LinearLayout.LayoutParams) mProgressBar.getLayoutParams();
                params.width = mainWidth;
                params.height = mainHeight;
                params.leftMargin = 0;
                params2.width = mainWidth;
                Log.d(TAG, "leaving FULLSCREEN");
                listView.setVisibility(View.VISIBLE);
                videoView.setLayoutParams(params);
                mProgressBar.setLayoutParams(params2);
                restoreScreen.setVisibility(View.GONE);
                fullScreen.setVisibility(View.VISIBLE);
            }
        });

        try {
            incidentVideoList = IncidentUtils.getFlaggedVideosList(getApplicationContext());
        } catch (JSONException e) {
            Log.e(TAG, "Could not retrieve flagged videos list");
            Log.d(TAG, e.toString());
        }

        for (String p : videoPathList) {
            boolean flag = incidentVideoList.contains(p);
            videoList.add(new VideoEntry(p, flag));
        }

        adap = new VideoEntryAdapter(videoList);

        listView.setAdapter(adap);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, final View arg1, int arg2,
                                    long arg3) {

                if (task != null)
                    task.cancel(true);

                unsetPlay();
                adap.setCurrentVideo(arg2);
                adap.notifyDataSetChanged();

                VideoEntry chapter = adap.getItem(arg2);
                HistoryUtils.registerHistoryEvent(getApplicationContext(), State.SEEN_VIDEO, chapter.video);
                videoView.setVideoPath(chapter.path);
                videoView.start();
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                    public void onPrepared(MediaPlayer mp) {
                        task = new MyAsync();
                        task.execute(arg1);
                    }
                });
            }
        });

//        videoView.setClickable(true);
        videoView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                now = System.currentTimeMillis();

                if ( (now-timemillis) < 300) {
                    return true;
                }

                timemillis = System.currentTimeMillis();
//                v.setClickable(false);
//
//                v.setClickable(true);

                Log.d(TAG, "Paused: "+paused);

                if (paused) {
                    videoView.seekTo(videoPosition);
                    videoView.start();
                    paused = false;
                } else {
                    videoView.pause();
                    videoPosition = videoView.getCurrentPosition();
                    paused = true;
                }

                return true;
            }
        });
    }

    private void unsetPlay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adap.setCurrentVideo(-1);
                adap.notifyDataSetChanged();
            }
        });
    }

    public void onDestroy() {
        if (task != null)
            task.cancel(true);
        videoView.stopPlayback();
        super.onDestroy();
    }

    private class MyAsync extends AsyncTask<Object, Integer, Void>
    {
        int duration = 0;
        int current = 0;
        @Override
        protected Void doInBackground(Object... params) {

            now = System.currentTimeMillis();

            if ( (now-timemillis) < 200) {
                return null;
            }

            timemillis = System.currentTimeMillis();
            Log.d(TAG, "CLICCCCKKK");

            duration = videoView.getDuration();
            mProgressBar.setProgress(0);

            do {
                if (isCancelled() || !videoView.isPlaying()) {
                    break;
                }

                current = videoView.getCurrentPosition();
//                System.out.println("duration - " + duration + " current- "
//                        + current);
                try {
                    publishProgress((int) (current * 100 / duration));
                    if(mProgressBar.getProgress() >= 100){
                        unsetPlay();
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (mProgressBar.getProgress() <= 100 && videoView.isPlaying());

            Log.d(TAG, "exited task");
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
//            System.out.println(values[0]);
            mProgressBar.setProgress(values[0]);
        }
    }
}
