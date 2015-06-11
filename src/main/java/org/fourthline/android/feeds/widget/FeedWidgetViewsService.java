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

package org.fourthline.android.feeds.widget;


import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.googlecode.sqb.query.DataType;
import org.fourthline.android.feeds.R;
import org.fourthline.android.feeds.content.Feed;
import org.fourthline.android.feeds.content.FeedConfig;
import org.fourthline.android.feeds.content.FeedEntry;
import org.fourthline.android.feeds.content.FeedWidgetConfig;
import org.fourthline.android.feeds.database.PersistentEntity;
import org.fourthline.android.feeds.util.SystemDateFormat;

import java.util.logging.Logger;

public class FeedWidgetViewsService extends RemoteViewsService {

    final private static Logger log = Logger.getLogger(FeedWidgetViewsService.class.getName());

    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return new RemoteViewsFactory() {

            long feedWidgetConfigId =
               intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            FeedWidgetConfig feedWidgetConfig;
            Cursor cursor;
            RemoteViews loadingView;

            public void onCreate() {
                log.fine("On create");
                loadingView = new RemoteViews(getPackageName(), R.layout.feedwidget_item_loading_layout);
            }

            public void onDataSetChanged() {
                log.fine("Feed entry content changed, updating widget: " + feedWidgetConfigId);
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                    feedWidgetConfig = null;
                }

                Cursor configCursor = null;
                try {
                    Uri configUri = Uri.withAppendedPath(FeedWidgetConfig.CONTENT_URI, Long.toString(feedWidgetConfigId));
                    configCursor = getContentResolver().query(configUri, null, null, null, null);
                    if (configCursor.moveToFirst()) {
                        feedWidgetConfig = new FeedWidgetConfig(configCursor);
                    } else {
                        log.fine("No feed widget config found: " + feedWidgetConfigId);
                        return;
                    }
                } finally {
                    if (configCursor != null)
                        configCursor.close();
                }

                log.fine("Loading feed entries of feed widget config: " + feedWidgetConfig.getId());
                Uri uri = Uri.withAppendedPath(FeedEntry.CONTENT_URI_FOR_FEEDWIDGET, feedWidgetConfig.getIdAsString());
                // TODO: Hardcoded limit of entries for widgets, 50 should be enough?
                cursor = getContentResolver().query(
                   uri,
                   null,
                   PersistentEntity.RESTRICTION_LIMIT_OFFSET,
                   new String[]{Integer.toString(50), Long.toString(0)},
                   null
                );
            }

            public void onDestroy() {
                if (cursor != null) cursor.close();
            }

            public int getCount() {
                return cursor != null ? cursor.getCount() : 0;
            }

            public RemoteViews getViewAt(int position) {
                RemoteViews rv = new RemoteViews(getPackageName(), R.layout.feedwidget_item_layout);
                if (cursor == null || !cursor.moveToPosition(position))
                    return rv;

                FeedEntry entry = new FeedEntry(cursor);

                // Build the headline
                String headline = entry.getValue(FeedEntry.TITLE);
                headline = Html.fromHtml(headline).toString(); // Resolve HTML entities

                String prefix = null;
                Enum<FeedConfig.EntryPrefix> entryPrefix = DataType.read(cursor, FeedConfig.ENTRY_PREFIX, FeedConfig.ALIAS_PREFIX);
                if (entryPrefix == FeedConfig.EntryPrefix.AUTHOR) {
                    prefix = entry.getValue(FeedEntry.AUTHOR);
                } else if (entryPrefix == FeedConfig.EntryPrefix.FEED_TITLE) {
                    prefix = DataType.read(cursor, Feed.TITLE, Feed.ALIAS_PREFIX);
                } else if (entryPrefix == FeedConfig.EntryPrefix.TIMESTAMP) {
                    prefix = SystemDateFormat.formatDayMonthTime(getContentResolver(), entry.getValue(FeedEntry.POLLED_DATE));
                }
                if (prefix != null)
                    headline = prefix + " · " + headline;

                // TODO This breaks the list, gaps when we switch sizes
                //rv.setFloat(R.id.widget_item, "setTextSize", textSize.size);

                int itemViewId;
                FeedWidgetConfig.TextSize textSize =
                   FeedWidgetConfig.TextSize.valueOf(feedWidgetConfig.getValueAsString(FeedWidgetConfig.TEXT_SIZE));
                switch (textSize) {
                    case SMALL:
                        itemViewId = R.id.widget_item_small;
                        rv.setViewVisibility(R.id.widget_item_small, View.VISIBLE);
                        rv.setViewVisibility(R.id.widget_item_medium, View.GONE);
                        rv.setViewVisibility(R.id.widget_item_large, View.GONE);
                        break;
                    case LARGE:
                        itemViewId = R.id.widget_item_large;
                        rv.setViewVisibility(R.id.widget_item_small, View.GONE);
                        rv.setViewVisibility(R.id.widget_item_medium, View.GONE);
                        rv.setViewVisibility(R.id.widget_item_large, View.VISIBLE);
                        break;
                    default:
                        itemViewId = R.id.widget_item_medium;
                        rv.setViewVisibility(R.id.widget_item_small, View.GONE);
                        rv.setViewVisibility(R.id.widget_item_medium, View.VISIBLE);
                        rv.setViewVisibility(R.id.widget_item_large, View.GONE);
                        break;
                }

                int textColor = DataType.read(cursor, FeedConfig.TEXT_COLOR, FeedConfig.ALIAS_PREFIX);
                rv.setTextColor(itemViewId, textColor);

                boolean isRead = entry.getValue(FeedEntry.IS_READ);
                if (!isRead) {
                    SpannableString s = new SpannableString(headline);
                    s.setSpan(new StyleSpan(Typeface.BOLD), 0, Math.max(0, headline.length() - 1), 0);
                    rv.setTextViewText(itemViewId, s);
                } else {
                    rv.setTextViewText(itemViewId, headline);
                }

                // Fill in the click intent's data, the feed entry id
                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(FeedWidgetProvider.EXTRA_FEEDENTRY_ID, entry.getId());
                rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

                return rv;
            }

            public RemoteViews getLoadingView() {
                // This is only shown while getViewAt() runs, so totally useless. Let's just show an
                // empty view... if you don't, you get "loading!!11!" for every line.
                return loadingView;
            }

            public int getViewTypeCount() {
                return 1;
            }

            public long getItemId(int position) {
                return position;
            }

            public boolean hasStableIds() {
                return true;
            }

        };
    }
}

