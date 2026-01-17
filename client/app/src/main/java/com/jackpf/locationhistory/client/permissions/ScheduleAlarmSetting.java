package com.jackpf.locationhistory.client.permissions;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class ScheduleAlarmSetting extends RequiredSetting {
    public ScheduleAlarmSetting(String description, String explanation) {
        super(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, description, explanation);
    }

    @Override
    public boolean isGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }
}
