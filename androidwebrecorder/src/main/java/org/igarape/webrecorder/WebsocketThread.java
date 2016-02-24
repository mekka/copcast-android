package org.igarape.webrecorder;

import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

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

    public WebsocketThread(String websocket_server_url) {

        this.url = websocket_server_url;

    }

    public void push(byte[] packet) {
        this.pipe.add(packet);
    }

    @Override
    public void run() {

        WebSocketFactory factory = new WebSocketFactory();
        try {
            ws = factory.createSocket(url, 5000);
            ws.addHeader("isMobile", "true");
            ws.connect();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create websocket connection", e);
        }

        Log.d(TAG, "Service STARTED");

        long lastTenSecond, currentTenSecond=0;
        isRunning = true;

        while(isRunning) {
            try {
                packet = pipe.poll(5, TimeUnit.SECONDS);
                if (packet != null) {
                    lastTenSecond = System.currentTimeMillis()/10000;
                    if (lastTenSecond > currentTenSecond) {
                        Log.d(TAG, "bandwidth: "+traffic/10.0);
                        traffic = 0;
                        currentTenSecond = lastTenSecond;
                    } else {
                        traffic += packet.length;
                    }

//                    Log.d(TAG, "Read bytes:" + packet.length);
                    ws.sendBinary(packet);
                }
            } catch (Exception e) {
                Log.e(TAG, "error polling", e);
            }
        }
    }

    public void end() {
        ws.disconnect();
        isRunning = false;
    }
}
