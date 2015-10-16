package org.igarape.copcast.views;

import android.app.Activity;
import android.os.Bundle;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;

import java.io.File;

/**
 * Created by brunosiqueira on 16/10/15.
 */
public class VideoGalleryActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_gallery);



        File dir = new File(FileUtils.getPath(Globals.getUserLogin(this)));
        File[] files = dir.listFiles(FileUtils.filter);
    }
}
