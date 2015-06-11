package org.fourthline.android.feeds.widget;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

/*
    http://stackoverflow.com/questions/2636098/android-spinner-selection/2649198#2649198
 */
public class OnItemSelectedListenerWrapper implements OnItemSelectedListener {

    protected OnItemSelectedListener listener;
    protected int lastPosition;

    public OnItemSelectedListenerWrapper(OnItemSelectedListener listener, int lastPosition) {
        this.listener = listener;
        this.lastPosition = lastPosition;
    }

    public OnItemSelectedListenerWrapper(OnItemSelectedListener listener) {
        this.listener = listener;
        lastPosition = 0;
    }

    @Override
    public void onItemSelected(AdapterView<?> aParentView, View aView, int aPosition, long anId) {
        if (lastPosition == aPosition) {
            Log.d(getClass().getName(), "Ignoring onItemSelected for same position: " + aPosition);
        } else {
            Log.d(getClass().getName(), "Passing on onItemSelected for different position: " + aPosition);
            listener.onItemSelected(aParentView, aView, aPosition, anId);
        }
        lastPosition = aPosition;
    }

    @Override
    public void onNothingSelected(AdapterView<?> aParentView) {
        listener.onNothingSelected(aParentView);
    }
}