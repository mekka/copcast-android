package org.igarape.copcast.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.igarape.copcast.R;
import org.igarape.copcast.exceptions.StateTransitionException;
import org.igarape.copcast.state.State;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by martelli on 3/23/16.
 */

public class StateManager {

    private static String TAG = StateManager.class.getCanonicalName();
    private State currentState = State.LOGGED_OFF;
    private Context context;

    public StateManager(Context context) {
        this.context = context;
    }

    public void setState(State newState) throws StateTransitionException {

        Log.d(TAG, "From "+currentState.toString()+" to "+newState.toString());

        boolean isInvalid = false;

        if (currentState == newState)
            throw new StateTransitionException("Trying to switch to same state ["+currentState.toString()+"]");

        switch(newState) {
            case IDLE:
                break;

            case LOGGED_OFF:
                break;

            case RECORDING:
                isInvalid = (currentState != State.IDLE && currentState != State.STREAMING);
                break;

            case STREAMING:
                isInvalid = (currentState != State.RECORDING);
                break;

            case PAUSED:
                isInvalid = (currentState != State.RECORDING && currentState != State.STREAMING);
                break;

            case UPLOADING:
                isInvalid = (currentState != State.IDLE);
                break;
        }

        if (isInvalid)
            throw new StateTransitionException("Transition from ["+currentState.toString()+"] to ["+newState.toString()+"] forbidden.");

        JSONObject extras = new JSONObject();
        try {
            extras.put("sessionId", Globals.getSessionID());
        } catch (JSONException e) {
            throw new StateTransitionException("Error setting sessionId into JsonObject");
        }

        HistoryUtils.registerHistory(context, currentState, newState, extras);

        currentState = newState;
    }

    public boolean isState(State state) {
        return this.currentState == state;
    }

    public static void setStateOrDie(Activity activity, State newState) {
        try {
            Globals.getStateManager().setState(newState);
        } catch (StateTransitionException e) {
            OkDialog.displayAndTerminate(activity, activity.getString(R.string.internal_error), e.getMessage());
        }
    }
}
