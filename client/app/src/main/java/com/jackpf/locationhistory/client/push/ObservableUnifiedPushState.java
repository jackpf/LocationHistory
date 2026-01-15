package com.jackpf.locationhistory.client.push;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Creates a static observable "enabled" flag
 * This acts as a source of truth for the front end, and is updated
 * in conjunction with the stored state in the UnifiedPushStorage
 */
public class ObservableUnifiedPushState {
    private static ObservableUnifiedPushState INSTANCE;

    private final MutableLiveData<Boolean> enabled;

    private ObservableUnifiedPushState(Context context) {
        UnifiedPushStorage unifiedPushStorage = new UnifiedPushStorage(context);
        this.enabled = new MutableLiveData<>(unifiedPushStorage.isEnabled());
    }

    public static synchronized ObservableUnifiedPushState getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ObservableUnifiedPushState(context);
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
