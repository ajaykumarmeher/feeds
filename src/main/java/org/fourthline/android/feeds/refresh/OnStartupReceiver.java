/*
 * Copyright (C) 2012 4th Line GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fourthline.android.feeds.refresh;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.fourthline.android.feeds.R;

import java.util.Calendar;
import java.util.logging.Logger;

public class OnStartupReceiver extends BroadcastReceiver {

    final private static Logger log = Logger.getLogger(OnStartupReceiver.class.getName());

    public static final String ACTION_STARTUP = OnStartupReceiver.class.getName() + ".STARTUP";

    public static final int PERIOD_SECONDS = 300; // Check every five minutes

    @Override
    public void onReceive(Context context, Intent intent) {
        log.fine("On receive, starting alarm manager for periodic feed refresh checks");

        Intent onRefreshIntent = new Intent(context, FeedRefreshService.class);

        PendingIntent pendingIntent =
           PendingIntent.getService(context, 1, onRefreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 30); // Delayed start after boot, avoiding congestion

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean wakeForRefresh = preferences.getBoolean(context.getString(R.string.key_feedreader_wakeup), false);

        alarmManager.setRepeating(
           wakeForRefresh ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC,
           calendar.getTimeInMillis(),
           PERIOD_SECONDS * 1000,
           pendingIntent
        );
    }
}
