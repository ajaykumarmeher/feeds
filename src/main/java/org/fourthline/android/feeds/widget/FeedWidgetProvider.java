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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import com.googlecode.sqb.query.DataType;
import org.fourthline.android.feeds.FeedEntryDetailsActivity;
import org.fourthline.android.feeds.R;
import org.fourthline.android.feeds.content.FeedConfig;
import org.fourthline.android.feeds.content.FeedEntry;
import org.fourthline.android.feeds.content.FeedWidgetConfig;
import org.fourthline.android.feeds.content.FeedWidgetFeed;
import org.fourthline.android.feeds.model.FeedEntryDetail;
import org.fourthline.android.feeds.model.FeedEntryDetails;
import org.fourthline.android.feeds.model.FeedReader;
import org.fourthline.android.feeds.refresh.FeedRefreshService;
import org.fourthline.android.feeds.database.PersistentEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FeedWidgetProvider extends AppWidgetProvider {

    final private static Logger log = Logger.getLogger(FeedWidgetProvider.class.getName());

    public static final String ACTION_FEEDWIDGET_UPDATE = "feed.FEEDWIDGET_UDPATE";
    public static final String ACTION_FEEDWIDGET_UPDATE_ALL = "feed.FEEDWIDGET_UDPATE_ALL";
    public static final String ACTION_FEEDWIDGET_MARKREAD = "feed.FEEDWIDGET_MARKREAD";
    public static final String ACTION_FEEDWIDGET_CLICK_ENTRY = "feed.FEEDWIDGET_CLICK_ENTRY";
    public static final String EXTRA_FEEDENTRY_ID = "FEEDENTRY_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        log.fine("On receive intent: " + intent.getAction());
        if (ACTION_FEEDWIDGET_UPDATE.equals(intent.getAction())) {
            onUpdate(
                context,
                AppWidgetManager.getInstance(context),
                intent.getExtras().getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            );
        } else if (ACTION_FEEDWIDGET_UPDATE_ALL.equals(intent.getAction())) {
            // TODO: ContentObservers can not be used with widgets, and the Weather examples is wrong. This sucks.
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] widgetIds = manager.getAppWidgetIds(
                new ComponentName(context, FeedWidgetProvider.class)
            );
            onUpdate(context, manager, widgetIds);
        } else if (ACTION_FEEDWIDGET_CLICK_ENTRY.equals(intent.getAction())) {
            showEntry(
               context,
               intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID),
               intent.getExtras().getLong(EXTRA_FEEDENTRY_ID)
            );
        } else if (ACTION_FEEDWIDGET_MARKREAD.equals(intent.getAction())) {
            markRead(
               context,
               intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)
            );
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] widgetIds) {
        for (int widgetId : widgetIds) {
            log.fine("On update of widget: " + widgetId);

            final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.feedwidget_layout);

            FeedWidgetConfig config = null;
            Cursor c = null;
            try {
                Uri uri = Uri.withAppendedPath(FeedWidgetConfig.CONTENT_URI, Long.toString(widgetId));
                c = context.getContentResolver().query(uri, null, null, null, null);
                if (c.moveToFirst())
                    config = new FeedWidgetConfig(c);
            } finally {
                if (c != null) c.close();
            }

            updateWidget(context, widgetId, rv, config);

            appWidgetManager.updateAppWidget(widgetId, rv);

            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.entry_list);
        }

        super.onUpdate(context, appWidgetManager, widgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        super.onDeleted(context, widgetIds);
        for (int widgetId : widgetIds) {
            log.fine("On delete of widget: " + widgetId);
            Uri uri = Uri.withAppendedPath(FeedWidgetConfig.CONTENT_URI, Long.toString(widgetId));
            context.getContentResolver().delete(uri, null, null);
        }
    }

    protected void updateWidget(Context context, int widgetId, RemoteViews rv, FeedWidgetConfig config) {
        if (config != null) {
            log.fine("Have config, updating widget: " + widgetId);

            long[] feedConfigIds = queryFeedConfigIds(context, widgetId);

            rv.setViewVisibility(R.id.initial_configure, View.GONE);
            rv.setViewVisibility(R.id.entries, View.VISIBLE);

            boolean displayToolbar = config.getValue(FeedWidgetConfig.TOOLBAR);
            if (displayToolbar) {
                log.fine("Showing toolbar");
                rv.setViewVisibility(R.id.toolbar, View.VISIBLE);
            } else {
                log.fine("Hiding toolbar");
                rv.setViewVisibility(R.id.toolbar, View.GONE);
            }

            rv.setInt(R.id.entries, "setBackgroundColor", config.getValue(FeedWidgetConfig.BACKGROUND_COLOR));

            rv.setOnClickPendingIntent(R.id.configure_button, createConfigureIntent(context, widgetId));
            rv.setOnClickPendingIntent(R.id.mark_button, createMarkReadIntent(context, widgetId));
            rv.setOnClickPendingIntent(R.id.refresh_button, createRefreshIntent(context, feedConfigIds));

            // The entry list is handled with an adapter
            final Intent viewsServiceIntent = new Intent(context, FeedWidgetViewsService.class);
            viewsServiceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            viewsServiceIntent.setData(Uri.parse(viewsServiceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            rv.setRemoteAdapter(widgetId, R.id.entry_list, viewsServiceIntent);
            rv.setEmptyView(R.id.entry_list, R.id.empty_view);

            // Clicks on the entry list item (template, see views service for fill in)
            final Intent entryTemplateIntent = new Intent(context, FeedWidgetProvider.class);
            entryTemplateIntent.setAction(ACTION_FEEDWIDGET_CLICK_ENTRY);
            entryTemplateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            entryTemplateIntent.setData(Uri.parse(entryTemplateIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent entryTemplatePendingIntent =
               PendingIntent.getBroadcast(
                  context, 0,
                  entryTemplateIntent, PendingIntent.FLAG_UPDATE_CURRENT
               );
            rv.setPendingIntentTemplate(R.id.entry_list, entryTemplatePendingIntent);

        } else {
            log.fine("No config, initial configuration view for widget: " + widgetId);
            rv.setViewVisibility(R.id.initial_configure, View.VISIBLE);
            rv.setViewVisibility(R.id.toolbar, View.GONE);
            rv.setViewVisibility(R.id.entries, View.GONE);
            rv.setOnClickPendingIntent(R.id.initial_configure_button, createConfigureIntent(context, widgetId));
        }
    }

    protected void showEntry(Context context, int widgetId, long feedEntryId) {
        log.fine("On entry click, preparing entry details for display: " + feedEntryId);
        FeedEntryDetails feedEntryDetails = null;
        Cursor c = null;
        try {
            log.fine("Loading feed entries of feed widget config: " + widgetId);
            Uri uri = Uri.withAppendedPath(FeedEntry.CONTENT_URI_FOR_FEEDWIDGET, String.valueOf(widgetId));
            // This is a best guess, we load 200 and we hope that the identifier is still in there
            c = context.getContentResolver().query(
               uri,
               null,
               PersistentEntity.RESTRICTION_LIMIT_OFFSET,
               new String[]{Integer.toString(200), Long.toString(0)},
               null
            );

            int position = -1;
            List<FeedEntryDetail> details = new ArrayList<FeedEntryDetail>();
            int i = 0;
            while (c.moveToNext()) {
                FeedEntryDetail detail = new FeedEntryDetail(
                   DataType.read(c, FeedEntry._ID),
                   DataType.read(c, FeedConfig._ID, FeedConfig.ALIAS_PREFIX),
                   DataType.read(c, FeedEntry.LINK, FeedEntry.ALIAS_PREFIX)
                );
                details.add(detail);

                if (detail.id == feedEntryId)
                    position = i;
                i++;
            }

            if (position != -1) {
                feedEntryDetails =
                   new FeedEntryDetails(
                      position, details.toArray(new FeedEntryDetail[details.size()])
                   );
            }

        } finally {
            if (c != null) c.close();
        }

        if (feedEntryDetails != null) {
            log.fine("Starting feed entry details activity");
            Intent detailsIntent = new Intent(context, FeedEntryDetailsActivity.class);
            detailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            FeedReader feedReader = new FeedReader();
            feedReader.feedEntryDetails = feedEntryDetails;
            detailsIntent.putExtra(FeedReader.EXTRA_FEEDREADER, feedReader);
            context.startActivity(detailsIntent);
        } else {
            log.fine("Feed entry disappeared between the click and the handling of the click");
            // Nothing happens, too bad
        }
    }

    protected void markRead(Context context, int widgetId) {
        long[] feedConfigIds = queryFeedConfigIds(context, widgetId);
        for (long feedConfigId : feedConfigIds) {
            log.fine("Marking read feed config: " + feedConfigId);
            ContentValues cv = new ContentValues();
            DataType.write(cv, FeedEntry.IS_READ, true);
            context.getContentResolver().update(
               FeedEntry.CONTENT_URI,
               cv,
               FeedEntry.FEED_ID.getName(),
               new String[]{String.valueOf(feedConfigId)}
            );
            Uri uri = Uri.withAppendedPath(FeedConfig.CONTENT_URI, String.valueOf(feedConfigId));
            log.fine("Notifying listeners of feed config change: " + uri);
            context.getContentResolver().notifyChange(uri, null);
        }
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
           .cancel(FeedRefreshService.NOTIFICATION_NEW_ENTRIES);
    }

    protected PendingIntent createConfigureIntent(Context context, int widgetId) {
        final Intent intent = new Intent(context, FeedWidgetPreferenceActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getActivity(context, intent.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected PendingIntent createMarkReadIntent(Context context, int widgetId) {
        final Intent intent = new Intent(context, FeedWidgetProvider.class);
        intent.setAction(ACTION_FEEDWIDGET_MARKREAD);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getBroadcast(context, intent.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected PendingIntent createRefreshIntent(Context context, long[] feedConfigIds) {
        Intent intent = new Intent(context, FeedRefreshService.class);
        intent.putExtra(FeedRefreshService.EXTRA_FORCE_REFRESH, true);
        intent.putExtra(FeedRefreshService.EXTRA_FEEDCONFIG_IDS, feedConfigIds);
        return PendingIntent.getService(context, intent.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected long[] queryFeedConfigIds(Context context, int widgetId) {
        Cursor c = null;
        try {
            Uri uri = Uri.withAppendedPath(FeedWidgetFeed.CONTENT_URI, String.valueOf(widgetId));
            c = context.getContentResolver().query(uri, null, null, null, null);
            long[] result = new long[c.getCount()];
            int i = 0;
            while (c.moveToNext()) {
                result[i] = DataType.read(c, FeedWidgetFeed.FEED_CONFIG_ID, FeedWidgetFeed.ALIAS_PREFIX);
                i++;
            }
            return result;
        } finally {
            if (c != null) c.close();
        }
    }

}