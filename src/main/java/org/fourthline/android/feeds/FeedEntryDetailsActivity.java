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

import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import org.fourthline.android.feeds.content.FeedConfig;
import org.fourthline.android.feeds.model.FeedEntryDetail;
import org.fourthline.android.feeds.model.FeedReader;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class FeedEntryDetailsActivity extends AbstractFeedReaderActivity {

    final private static Logger log = Logger.getLogger(FeedEntryDetailsActivity.class.getName());

    //protected ShareActionProvider shareActionProvider;
    protected String currentFeedEntryLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log.fine("On create");
        setContentView(R.layout.feed_entry_details_layout);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        setCurrentState(savedInstanceState);
    }

    @Override
    protected void setCurrentState(Bundle savedInstanceState) {
        super.setCurrentState(savedInstanceState);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter adapter = new FeedEntryDetailsPagerAdapter(getFragmentManager(), feedReader.feedEntryDetails.details) {
            @Override
            protected void onFeedEntryDetailVisible(FeedEntryDetail detail) {
                currentFeedEntryLink = detail.link;
                /*
                if (shareActionProvider == null) return;
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_SUBJECT, "Article URL");
                intent.putExtra(Intent.EXTRA_TEXT, currentFeedEntryLink);
                intent.setType("text/plain");
                PackageManager packageManager = getPackageManager();
                List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : list) {
                    log.fine("----------- " + resolveInfo);
                }
                //shareActionProvider.setShareIntent(intent);
                */
            }
        };
        pager.setAdapter(adapter);
        pager.setCurrentItem(feedReader.feedEntryDetails.position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feed_entry_detail_menu, menu);

        /*
        MenuItem item = menu.findItem(R.id.menu_share_url);
        shareActionProvider = (ShareActionProvider) item.getActionProvider();
        */

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log.fine("Menu item selected: " + item);
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateHome();
                return true;
            case R.id.menu_view:
                viewFeedEntryLink();
                return true;
            case R.id.menu_copy_url:
                copyFeedEntryLink();
                return true;
            case R.id.menu_settings:
                Intent intent = new Intent(this, FeedsPreferenceActivity.class);
                startActivity(intent);
                return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        log.fine("On pause, feed config change notification (we might have marked some entries as read)");
        // Unique feed config ids
        Set<Long> feedConfigIds = new HashSet<Long>();
        for (FeedEntryDetail detail : feedReader.feedEntryDetails.details) {
            feedConfigIds.add(detail.feedConfigId);
        }
        for (Long feedConfigId : feedConfigIds) {
            Uri uri = Uri.withAppendedPath(FeedConfig.CONTENT_URI, String.valueOf(feedConfigId));
            getContentResolver().notifyChange(uri, null);
        }
    }

    protected void navigateHome() {
        log.fine("Navigating home to feed reader activity");
        Intent intent = new Intent(this, FeedEntryListActivity.class);
        intent.putExtra(FeedReader.EXTRA_FEEDREADER, feedReader);
        startActivity(intent);
        finish();
    }

    protected void viewFeedEntryLink() {
        if (currentFeedEntryLink != null) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(currentFeedEntryLink));
                startActivity(i);
            } catch (Exception ex) {
                log.warning("Invalid URI for feed entry: " + currentFeedEntryLink);
            }
        }
    }

    protected void copyFeedEntryLink() {
        if (currentFeedEntryLink != null) {
            log.fine("Copying feed entry URL to clipboard: " + currentFeedEntryLink);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText(currentFeedEntryLink);
            Toast.makeText(this, R.string.article_url_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }
}
