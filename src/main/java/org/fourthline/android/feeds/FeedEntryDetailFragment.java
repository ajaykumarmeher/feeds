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

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import org.fourthline.android.feeds.content.Feed;
import org.fourthline.android.feeds.content.FeedEntry;
import org.fourthline.android.feeds.util.StringUtil;
import org.fourthline.android.feeds.util.SystemDateFormat;

import java.util.logging.Logger;

public class FeedEntryDetailFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

    final private static Logger log = Logger.getLogger(FeedEntryDetailFragment.class.getName());

    public static final String EXTRA_FEEDENTRY_ID = "FEEDENTRY_ID";

    public static FeedEntryDetailFragment newInstance(long id) {
        log.fine("Constructing new instance with feed entry: " + id);
        FeedEntryDetailFragment f = new FeedEntryDetailFragment();
        Bundle args = new Bundle();
        args.putLong(EXTRA_FEEDENTRY_ID, id);
        f.setArguments(args);
        return f;
    }


    protected long feedEntryId;
    protected WebView descriptionView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log.fine("On create view");
        return inflater.inflate(R.layout.feed_entry_detail_layout, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        log.fine("On activity created");

        if (getArguments().containsKey(EXTRA_FEEDENTRY_ID)) {
            feedEntryId = getArguments().getLong(EXTRA_FEEDENTRY_ID);
            log.fine("Creating with feed entry argument: " + feedEntryId);
        } else {
            throw new IllegalArgumentException("Missing extra " + EXTRA_FEEDENTRY_ID);
        }

        descriptionView = (WebView) getView().findViewById(R.id.feed_entry_description);

        descriptionView.getSettings().setFixedFontFamily(descriptionView.getSettings().getStandardFontFamily());

        descriptionView.getSettings().setBuiltInZoomControls(true);
        descriptionView.getSettings().setSupportZoom(true);

        /// Load links in external browser
        descriptionView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                Uri uri = Uri.parse(url);
                Context context = webView.getContext();
                Intent intent = createBrowserViewIntent(uri, context);
                addActivityFlags(intent);
                boolean overridingUrlLoading = false;
                try {
                    context.startActivity(intent);
                    overridingUrlLoading = true;
                } catch (ActivityNotFoundException ex) {
                    // If no application can handle the URL, assume that the WebView can handle it.
                }
                return overridingUrlLoading;
            }

            private Intent createBrowserViewIntent(Uri uri, Context context) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                intent.putExtra(Browser.EXTRA_CREATE_NEW_TAB, true);
                return intent;
            }

            protected void addActivityFlags(Intent intent) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            }
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String textSize = preferences.getString(getString(R.string.key_feedreader_text_size), "MEDIUM");
        if (textSize.equals("SMALL")) {
            descriptionView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        } else if (textSize.equals("MEDIUM")) {
            descriptionView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
        } else {
            descriptionView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.CLOSE);
        }

        getLoaderManager().initLoader(Constants.FEED_ENTRY_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri = Uri.withAppendedPath(FeedEntry.CONTENT_URI, Long.toString(feedEntryId));
        log.fine("On create loader for: " + uri);
        return new CursorLoader(getActivity(), uri, null, null, null, null) {
            @Override
            public void onContentChanged() {
                // Ignoring content change! We don't want the detail view to jump around when the database
                // is updated, lets just show outdated information instead.
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, final Cursor cursor) {
        log.fine("Feed entry load finished");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!cursor.moveToFirst() || cursor.getCount() == 0) {
                    log.warning("Feed entry has been removed, closing this activity: " + feedEntryId);
                    getActivity().finish();
                    return;
                }
                updateView(cursor);
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        log.fine("Feed entry loader reset");
    }

    public void updateView(Cursor cursor) {
        log.fine("Updating feed entry view...");

        FeedEntry entry = new FeedEntry(cursor);

        TextView titleView = (TextView) getView().findViewById(R.id.title);
        String title = Html.fromHtml(Feed.getFeedTitleOrUrl(cursor)).toString();
        titleView.setText(title);

        String author = entry.getValue(FeedEntry.AUTHOR);
        TextView authorView = (TextView) getView().findViewById(R.id.feed_entry_author);
        if (!author.equals(FeedEntry.DEFAULT_AUTHOR)) {
            authorView.setText(author);
            authorView.setVisibility(View.VISIBLE);
        } else {
            authorView.setVisibility(View.GONE);
        }

        TextView receivedDateView = (TextView) getView().findViewById(R.id.feed_entry_date_received);
        TextView publishedDateView = (TextView) getView().findViewById(R.id.feed_entry_date_published);
        TextView updatedDateView = (TextView) getView().findViewById(R.id.feed_entry_date_updated);

        long publishedDate = entry.getValue(FeedEntry.PUBLISHED_DATE);
        if (publishedDate != FeedEntry.DEFAULT_DATE) {
            receivedDateView.setText(
                getString(R.string.received) + ": " +
                    SystemDateFormat.formatDayMonthTime(getActivity().getContentResolver(), entry.getValue(FeedEntry.POLLED_DATE))
            );
            publishedDateView.setText(
                getString(R.string.published) + ": " +
                    SystemDateFormat.formatDayMonthTime(getActivity().getContentResolver(), publishedDate)
            );
            publishedDateView.setVisibility(View.VISIBLE);
        } else {
            receivedDateView.setText(
                SystemDateFormat.formatDayMonthTime(getActivity().getContentResolver(), entry.getValue(FeedEntry.POLLED_DATE))
            );
            publishedDateView.setVisibility(View.GONE);
        }

        long updatedDate = entry.getValue(FeedEntry.UPDATED_DATE);
        if (updatedDate != FeedEntry.DEFAULT_DATE) {
            updatedDateView.setText(
                getString(R.string.updated) + ": " +
                    SystemDateFormat.formatDayMonthTime(getActivity().getContentResolver(), updatedDate)
            );
            updatedDateView.setVisibility(View.VISIBLE);
        } else {
            updatedDateView.setVisibility(View.GONE);
        }

        TextView feedEntryTitleView = (TextView) getView().findViewById(R.id.feed_entry_title);
        String entryTitle = Html.fromHtml(entry.getValue(FeedEntry.TITLE)).toString();
        feedEntryTitleView.setText(entryTitle);

        descriptionView.loadDataWithBaseURL(
            "fake://android.developers.are.morons.and.this.is.needed.for.umlauts",
            FeedEntry.getDescriptionAsHTML(cursor),
            "text/html",
            "utf-8",
            null
        );
    }


}
