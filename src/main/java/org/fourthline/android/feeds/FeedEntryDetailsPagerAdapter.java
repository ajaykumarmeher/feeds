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
import android.app.Fragment;
import android.app.FragmentManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;
import org.fourthline.android.feeds.content.FeedEntry;
import org.fourthline.android.feeds.model.FeedEntryDetail;

import java.util.logging.Logger;

public class FeedEntryDetailsPagerAdapter extends FragmentPagerAdapter {

    final private static Logger log = Logger.getLogger(FeedEntryDetailsPagerAdapter.class.getName());

    protected FeedEntryDetail[] feedEntryDetails;

    public FeedEntryDetailsPagerAdapter(FragmentManager fm, FeedEntryDetail[] feedEntryDetails) {
        super(fm);
        log.fine("Creating adapter with entry details: " + feedEntryDetails.length);
        this.feedEntryDetails = feedEntryDetails;
    }

    @Override
    public Fragment getItem(int i) {
        long id = feedEntryDetails[i].id;
        log.fine("Creating new feed entry detail fragment for id: " + id);
        return FeedEntryDetailFragment.newInstance(id);
    }

    @Override
    public int getCount() {
        return feedEntryDetails.length;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, final int position, final Object object) {
        super.setPrimaryItem(container, position, object);

        final Fragment fragment = (Fragment)object;
        final FeedEntryDetail detail = feedEntryDetails[position];
        onFeedEntryDetailVisible(detail);
        if (!detail.isRead()) {
            log.fine("First time visible, marking feed entry read: " + detail);
            detail.setRead(true);

            // Update in database asynchronously
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    // Sanity check for concurrency issues, stupid Android
                    Activity activity = fragment.getActivity();
                    if (activity == null)
                        return null;

                    FeedEntry updatedEntry = new FeedEntry(detail.id, detail.isRead());

                    activity.getContentResolver().update(
                       Uri.withAppendedPath(FeedEntry.CONTENT_URI, Long.toString(detail.id)),
                       updatedEntry.INSTANCE.getEntityValues(),
                       null,
                       null
                    );
                    return null;
                }
            }.execute();
        }
    }

    protected void onFeedEntryDetailVisible(FeedEntryDetail detail) {

    }



}
