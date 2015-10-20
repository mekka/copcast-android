package org.igarape.copcast.views;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brunosiqueira on 16/10/15.
 */
public class VideoGalleryActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_gallery);

        File dir = new File(FileUtils.getPath(Globals.getUserLogin(this)));
        File[] files = dir.listFiles(FileUtils.filter);

        ArrayList<Map<String, String>> list = buildData(files);
        String[] from = { "name" };
        int[] to = { R.id.file_name };

        SimpleAdapter adapter = new SimpleAdapter(this, list,
            R.layout.video_gallery_item, from, to);
        setListAdapter(adapter);
    }

    private ArrayList<Map<String, String>> buildData(File[] files) {
        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();
        for (File file: files){
            list.add(putData(file));
        }
        return list;
    }

    private HashMap<String, String> putData(File file) {
        HashMap<String, String> item = new HashMap<String, String>();
        item.put("name", file.getName());
        return item;
    }

}
