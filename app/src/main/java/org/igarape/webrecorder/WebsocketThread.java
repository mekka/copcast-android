package org.igarape.webrecorder;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

//import com.neovisionaries.ws.client.WebSocket;
//import com.neovisionaries.ws.client.WebSocketFactory;

/**
 * Created by martelli on 2/6/16.
 */

class WebsocketThread extends Thread {

    private int traffic = 0;
    private boolean isRunning = false;
    private static String TAG = WebsocketThread.class.getCanonicalName();
    Socket ws;
    private final ArrayBlockingQueue<byte[]> pipe = new ArrayBlockingQueue<>(100);
    byte[] packet;
    private String url;
    private byte[] sps;
    private final int fps;

    public WebsocketThread(Socket websocket, final int fps) {
        this.ws = websocket;
        Log.d(TAG, "Created.");
        this.fps = fps;
    }

    public void push(byte[] packet) {
        this.pipe.add(packet);
    }

    @Override
    public void run() {

        int counter = 0;

        Log.d(TAG, "Thread started.");

        try {
//            Log.d(TAG, "Websocket server: "+url);
//            ws = factory.createSocket(url, 5000);
//            String query = "";
//            for (String k : websocketHeaders.keySet()) {
//                if (query.length() > 0)
//                    query += "&";
//                query += k+"="+websocketHeaders.get(k);
//            }
//            Log.d(TAG, "Websocket query: "+query);
//
//            try {
//                IO.Options opts = new IO.Options();
//                opts.forceNew = true;
//                opts.query = query;
//                opts.reconnection = true;
//                ws = IO.socket(url, opts);
//            } catch (URISyntaxException e) {
//                Log.e(TAG, "error connecting socket", e);
//            }
//            ws.connect();
            ws.on("startStreaming", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "STREAM WANTED");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to create websocket connection", e);
        }

        long lastTenSecond, currentTenSecond=0;
        isRunning = true;

        Log.d(TAG, "Loop started.");
        while(isRunning) {
            try {
                packet = pipe.poll(20, TimeUnit.MILLISECONDS);
                if (packet != null) {

                    lastTenSecond = System.currentTimeMillis()/10000;
                    if (lastTenSecond > currentTenSecond) {
                        Log.d(TAG, "bandwidth: "+traffic/10.0);
                        traffic = 0;
                        currentTenSecond = lastTenSecond;
                    } else {
                        traffic += packet.length;
                    }

                    if (counter++ % fps == 0) {
                        ws.emit("frame", sps);
//                        Log.d(TAG, "Sending SPS");
                    }

                    ws.emit("frame", packet);
                }
            } catch (Exception e) {
                Log.e(TAG, "error polling", e);
            }
        }
        //ws.disconnect();
        Log.d(TAG, "Loop and thread finished.");
    }

    public void setSps(byte[] sps) {
        this.sps = sps;
        Log.d(TAG, "SPS set with "+sps.length+" bytes");
    }

    public void end() {
        Log.d(TAG, "Stop requested.");
        isRunning = false;
        Log.d(TAG, "Waiting for loop to finish.");
    }

    public void setStreaming(boolean isStreaming) {
        try {
            if (isStreaming)
                ws.emit("streamStarted");
            else
                ws.emit("streamStopped");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error sending stream command", e);
        }
    }
}
