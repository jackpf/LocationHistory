package com.jackpf.locationhistory.client.push;

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

    private ObservableUnifiedPushState(UnifiedPushStorage pushStorage) {
        this.enabled = new MutableLiveData<>(pushStorage.isEnabled());
    }

    public static synchronized ObservableUnifiedPushState getInstance(UnifiedPushStorage pushStorage) {
        if (INSTANCE == null) {
            INSTANCE = new ObservableUnifiedPushState(pushStorage);
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
