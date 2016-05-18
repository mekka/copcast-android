package org.igarape.copcast.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

/**
 * Created by dborkan on 5/18/16.
 */
public class EditTextUtils {
    public static void showKeyboardOnFocusAndClick(final android.app.Activity activity, final TextView tv) {
        // Show the keyboard when the user first focuses in the field.
        tv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showKeyboard(activity, tv);
                }
            }
        });
        // Show keyboard on click, in case the user has closed the keyboard.
        // Click does not fire when the user first focuses in the field.
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyboard(activity, tv);
            }
        });
    }

    public static void showKeyboard(final android.app.Activity activity, final TextView tv) {
        tv.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(tv, 0);
            }
        }, 200);
    }
}
