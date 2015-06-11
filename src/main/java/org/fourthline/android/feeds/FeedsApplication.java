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
package org.fourthline.android.feeds;

import android.app.Application;
import org.fourthline.android.feeds.util.FixedAndroidHandler;
import org.seamless.util.logging.LoggingUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FeedsApplication extends Application {

    @Override
    public void onCreate() {
        // Fix the logging integration between java.util.logging and Android internal logging
        //LoggingUtil.resetRootHandler(new FixedAndroidHandler());
        //Logger.getLogger("org.fourthline.android.feeds").setLevel(Level.FINEST);
        super.onCreate();
    }
}
