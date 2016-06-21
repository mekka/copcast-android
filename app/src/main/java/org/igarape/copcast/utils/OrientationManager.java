package org.igarape.copcast.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.OrientationEventListener;

import org.igarape.webrecorder.enums.Orientation;

/**
 * Created by martelli on 6/21/16.

 This class is used to allow the orientation to change rapidly but only
 "stable" positions (positions without change for some defined time)
 will be actually broadcasted.

 */


public class OrientationManager extends OrientationEventListener {

    private static String TAG = OrientationManager.class.getCanonicalName();
    private Context context;
    private Pair<Long, Orientation> lastCandidate;
    private int SHORT_TIMEOUT_IN_MS = 1000;
    private int LONG_TIMEOUT_IN_MS = 3000;

    public OrientationManager(Context context, int rate) {
        super(context, rate);
        this.context = context;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        Orientation narrow_o = null;

        if (orientation >= 330 || (orientation <= 30))
            narrow_o = Orientation.TOP;
        else if (orientation > 240 && orientation < 3006)
            narrow_o = Orientation.LEFT;
        else if (orientation > 150 && orientation < 210)
            narrow_o = Orientation.BOTTOM;
        else if (orientation > 60 && orientation < 120)
            narrow_o = Orientation.RIGHT;

        if (Globals.orientation == null) {
            Globals.orientation = new Pair<>(System.currentTimeMillis(), narrow_o);
            return;
        }

        if (narrow_o != null && (lastCandidate == null || lastCandidate.second != narrow_o)) {

            long now = System.currentTimeMillis();
            Log.v(TAG, "got "+narrow_o.name());
            lastCandidate = new Pair<>(System.currentTimeMillis(), narrow_o);
            final Handler handler = new Handler();
            final int calculated_timeout = ( (now-Globals.orientation.first) > LONG_TIMEOUT_IN_MS) ? SHORT_TIMEOUT_IN_MS : LONG_TIMEOUT_IN_MS;
            Log.v(TAG, "actual timeout: "+calculated_timeout);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    Log.v(TAG, "Time to set orientation!");
                    Log.v(TAG, ":"+now+"/"+calculated_timeout);
                    if (now - lastCandidate.first > calculated_timeout && lastCandidate.second != Globals.orientation.second)
                        OrientationManager.this.broadcastOrientation(lastCandidate.second);
                }
            }, calculated_timeout+20);
        }
    }

    private void broadcastOrientation(Orientation o) {
        Log.v(TAG, "Orientation changed to " + o.name());
        Globals.orientation = new Pair(System.currentTimeMillis(), o);
        Intent i = new Intent("ROTATION");
        i.putExtra("ORIENTATION", o.name());
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }

    public void tryStart() {
        if (this.canDetectOrientation())
            this.enable();
        else {
            Log.w(TAG, "This device cannot detect rotation");
        }
    }
}
