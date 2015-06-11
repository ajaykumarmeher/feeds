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

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import org.fourthline.android.feeds.model.FeedReader;
import org.fourthline.android.feeds.refresh.FeedRefreshService;

import java.util.logging.Logger;

public abstract class AbstractFeedReaderActivity extends Activity implements FeedReader.Provider {

    final private static Logger log = Logger.getLogger(AbstractFeedReaderActivity.class.getName());

    protected FeedReader feedReader;

    @Override
    public FeedReader getFeedReader() {
        return feedReader;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        setCurrentState(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(FeedReader.EXTRA_FEEDREADER, feedReader);
    }

    protected void setCurrentState(Bundle savedInstanceState) {

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
           .cancel(FeedRefreshService.NOTIFICATION_NEW_ENTRIES);

        if (savedInstanceState != null && savedInstanceState.containsKey(FeedReader.EXTRA_FEEDREADER)) {
            log.fine("Current state is saved instance state");
            feedReader = (FeedReader) savedInstanceState.getSerializable(FeedReader.EXTRA_FEEDREADER);
        } else if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(FeedReader.EXTRA_FEEDREADER)) {
            log.fine("Current state is passed intent state");
            feedReader = (FeedReader) getIntent().getExtras().getSerializable(FeedReader.EXTRA_FEEDREADER);
        } else {
            log.fine("Current state is new");
            feedReader = new FeedReader();
        }
    }


}
