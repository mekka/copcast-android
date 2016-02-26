package org.igarape.webrecorder;

import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by martelli on 2/6/16.
 */

class WebsocketThread extends Thread {

    private int traffic = 0;
    private boolean isRunning = false;
    private static String TAG = WebsocketThread.class.getCanonicalName();
    WebSocket ws;
    private final ArrayBlockingQueue<byte[]> pipe = new ArrayBlockingQueue<>(100);
    byte[] packet;
    private String url;
    private byte[] sps;
    private final int fps;
    Map<String, String> websocketHeaders;

    public WebsocketThread(String websocket_server_url, Map<String, String> websocketHeaders, final int fps) {
        this.websocketHeaders = websocketHeaders;
        this.url = websocket_server_url;
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

        WebSocketFactory factory = new WebSocketFactory();
        try {
            Log.d(TAG, "Websocket server: "+url);
            ws = factory.createSocket(url, 5000);
            ws.addHeader("isMobile", "true");
            for (String k : websocketHeaders.keySet()) {
                ws.addHeader(k, websocketHeaders.get(k));
            }
            ws.connect();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create websocket connection", e);
        }

        long lastTenSecond, currentTenSecond=0;
        isRunning = true;

        Log.d(TAG, "Loop started.");
        while(isRunning) {
            try {
                packet = pipe.poll(1, TimeUnit.SECONDS);
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
                        ws.sendBinary(sps);
                        Log.d(TAG, "Sending SPS");
                    }
                    ws.sendBinary(packet);
                }
            } catch (Exception e) {
                Log.e(TAG, "error polling", e);
            }
        }
        Log.d(TAG, "Loop and thread finished.");
    }

    public void setSps(byte[] sps) {
        this.sps = sps;
        Log.d(TAG, "SPS set with "+sps.length+" bytes");
    }

    public void end() {
        Log.d(TAG, "Stop requested.");
        ws.disconnect();
        isRunning = false;
        Log.d(TAG, "Waiting for loop to finish.");
    }
}
