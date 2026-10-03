package com.jackpf.locationhistory.client.push;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Singleton observable for the push-enabled state.
 * Acts as a source of truth for the UI; updated by {@link PushStorage#setEnabled(boolean)}.
 * Renamed from ObservableUnifiedPushState to be backend-agnostic.
 */
public class ObservablePushState {
    private static ObservablePushState INSTANCE;

    private final MutableLiveData<Boolean> enabled;

    private ObservablePushState(Context context) {
        PushStorage pushStorage = new PushStorage(context);
        this.enabled = new MutableLiveData<>(pushStorage.isEnabled());
    }

    public static synchronized ObservablePushState getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ObservablePushState(context.getApplicationContext());
        }
        return INSTANCE;
    }

    public void setEnabled(boolean enabled) {
        this.enabled.postValue(enabled);
    }

    public LiveData<Boolean> observeEnabled() {
        return enabled;
    }
}
