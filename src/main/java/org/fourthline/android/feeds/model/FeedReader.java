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

package org.fourthline.android.feeds.model;

import java.io.Serializable;

public class FeedReader implements Serializable {

    private static final long serialVersionUID = 3550271137185328335L;

    public interface Provider {
        FeedReader getFeedReader();
    }

    public static final String EXTRA_FEEDREADER = "FEEDREADER";

    public int feedPosition = -1;

    public long feedId = -1;

    public String feedUrl;

    public boolean haveEntries;

    public FeedEntryDetails feedEntryDetails;

    public void onFeedEntriesLoaded(boolean haveEntries) {
        this.haveEntries = haveEntries;
    }

    public void onFeedConfigSelected(int position, long id, String url) {
        this.feedPosition = position;
        this.feedId = id;
        this.feedUrl = url;
    }

    public void onFeedConfigDeselected() {
        this.feedPosition = -1;
        this.feedId = -1;
        this.feedUrl = null;
        this.haveEntries = false;
    }

    @Override
    public String toString() {
        return "Position: " + feedPosition + ", ID: " + feedId + ", Have Entries: " + haveEntries;
    }
}
