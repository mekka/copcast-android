package org.igarape.copcast.ws;

import android.content.Context;
import android.util.Log;
import org.igarape.copcast.utils.Globals;
import java.net.URISyntaxException;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

/**
 * Created by martelli on 7/14/16.
 */
public class SocketSingleton {

    private String TAG = SocketSingleton.class.getCanonicalName();
    private String query;
    private Socket ws;
    private static SocketSingleton socketSingleton;

    private SocketSingleton(Context context) {

        query = "token="+ Globals.getPlainToken(context);
        query += "&userId="+Globals.getUserId(context);
        query += "&clientType=android";

        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.query = query;
            opts.reconnection = true;
            opts.reconnectionDelay=5000;
            opts.reconnectionDelayMax=10000;
            opts.timeout = 20000;
            opts.upgrade = true;
            opts.transports = new String[] {"websocket"};
            ws = IO.socket(Globals.getServerUrl(context), opts);

        } catch (URISyntaxException e) {
            Log.e(TAG, "error connecting socket", e);
        }
    }

    public static SocketSingleton getInstance(Context context) {

        if (socketSingleton == null) {
            socketSingleton = new SocketSingleton(context);
        }

        return socketSingleton;
    }

    public void on(String event, Emitter.Listener listener) {
        ws.on(event, listener);
    }

    public void off(String event) {
        ws.off(event);
    }

    public void off(String event, Emitter.Listener listener) {
        ws.off(event, listener);
    }

    public void emit(String event) {
        ws.emit(event);
    }

    public void emit(String event, Object... args) {
        ws.emit(event, args);
    }

    public Socket connect() {
        return ws.connect();
    }

    public Socket disconnect() {
        return ws.disconnect();
    }

    public Socket getWebsocket() {
        return ws;
    }

    public static void dismiss() {
        socketSingleton = null;
    }
}
