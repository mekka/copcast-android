package org.igarape.copcast.views;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.VideoView;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.IncidentUtils;

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

    public VideoEntryAdapter(List<VideoEntry> videoEntries) {
        this.videoEntries = videoEntries;
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
        LayoutInflater inflater =  (LayoutInflater) LayoutInflater.from(parent.getContext());
        convertView = inflater.inflate(R.layout.my_list_item, parent, false);

        VideoEntry ve = videoEntries.get(position);
        TextView nome = (TextView) convertView.findViewById(R.id.listItemText);
        ImageView box = (ImageView) convertView.findViewById(R.id.imageView1);
        nome.setText(ve.video);

        if (ve.incident)
            box.setBackgroundColor(0xFFA10000);
        else
            box.setBackgroundColor(0x00000000);

        return convertView;
    }
}


public class PlayerActivity extends Activity {

    private static final String TAG = PlayerActivity.class.getName();
    private ArrayList<String> incidentVideoList = new ArrayList<>();
    private ArrayList<VideoEntry> videoList = new ArrayList<>();
    private ListView listView;
    private VideoView videoView;
    private boolean fullscreenToggle = false;
    private long timemillis, now = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        List<String> videoPathList;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback_video);

        listView = (ListView) findViewById(R.id.videoListView);
        videoView = (VideoView) findViewById(R.id.videoView);

        String userLogin = Globals.getUserLogin(getApplicationContext());

        videoPathList = FileUtils.getVideoPathList(userLogin);

        incidentVideoList = IncidentUtils.getIncidentVideosList(userLogin);

        for (String p : videoPathList) {
            boolean flag = incidentVideoList.contains(p);
            videoList.add(new VideoEntry(p, flag));
        }

        final VideoEntryAdapter adap = new VideoEntryAdapter(videoList);

        listView.setAdapter(adap);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {

                VideoEntry chapter = adap.getItem(arg2);

                videoView.setVideoPath(chapter.path);
                videoView.start();
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

                v.setClickable(false);
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) videoView.getLayoutParams();

                if (!fullscreenToggle) {
                    params.width = metrics.widthPixels;
                    params.height = metrics.heightPixels;
                    params.leftMargin = 0;
                    Log.d(TAG, "setting FULLSCREEN");
                    fullscreenToggle = true;
                } else {
                    params.width = -1;
                    params.height = (int) (250 * metrics.density);
                    params.leftMargin = 30;
                    Log.d(TAG, "leaving FULLSCREEN");
                    fullscreenToggle = false;
                }

                videoView.setLayoutParams(params);
                v.setClickable(true);
                return true;
            }
        });
    }
}
