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

package org.fourthline.android.feeds.util;

import android.content.ContentResolver;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemDateFormat {

    static protected SimpleDateFormat dateTimeFormat;

    protected static void init(ContentResolver contentResolver) {
        if (dateTimeFormat == null)
            dateTimeFormat = new SimpleDateFormat("dd. MMM " + getTimePattern(contentResolver));
    }

    public static String getTimePattern(ContentResolver contentResolver) {
        String clockType = Settings.System.getString(contentResolver, Settings.System.TIME_12_24);
        return "24".equals(clockType) ? "HH:mm" : "hh:mm a";
    }

    public static synchronized String formatDayMonthTime(ContentResolver contentResolver, long unixtime) {
        return formatDayMonthTime(contentResolver, new Date(unixtime));
    }

    public static synchronized String formatDayMonthTime(ContentResolver contentResolver, Date date) {
        init(contentResolver);
        return dateTimeFormat.format(date);
    }
}
