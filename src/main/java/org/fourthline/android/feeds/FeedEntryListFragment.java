/*
 * Copyright (C) 2011 4th Line GmbH, Switzerland
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
import android.app.ListFragment;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Adapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.googlecode.sqb.query.DataType;
import org.fourthline.android.feeds.content.Feed;
import org.fourthline.android.feeds.content.FeedConfig;
import org.fourthline.android.feeds.content.FeedEntry;
import org.fourthline.android.feeds.model.FeedEntryDetail;
import org.fourthline.android.feeds.model.FeedReader;
import org.fourthline.android.feeds.database.PersistentEntity;
import org.fourthline.android.feeds.util.SystemDateFormat;
import org.fourthline.android.feeds.widget.BindingCursorAdapter;
import org.fourthline.android.feeds.widget.PagingCursorAdapterWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FeedEntryListFragment extends ListFragment {

    final private static Logger log = Logger.getLogger(FeedEntryListFragment.class.getName());

    protected FeedReader.Provider stateProvider;
    protected OnFeedEntrySelectionListener onFeedEntrySelectionListener;
    protected PagingCursorAdapterWrapper adapter;
    protected ContentObserver refreshContentObserver;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            stateProvider = (FeedReader.Provider) activity;
            onFeedEntrySelectionListener = (OnFeedEntrySelectionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(getClass().getName() + " doesn't implement required listeners");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log.fine("On create view");
        return inflater.inflate(R.layout.feed_entry_list_layout, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        log.fine("On activity created");

        adapter = createListAdapter();
        setListAdapter(adapter);

        refreshContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                adapter.setRefreshRequired(isResumed());
            }
        };

        long id = stateProvider.getFeedReader().feedId;
        Uri uri = id == -1
           ? FeedConfig.CONTENT_URI
           : Uri.withAppendedPath(FeedConfig.CONTENT_URI, Long.toString(id));
        log.fine("On create, registering content observer: " + uri);
        getActivity().getContentResolver().registerContentObserver(
           uri, true, refreshContentObserver
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.refreshIfNecessary();
    }

    @Override
    public void onDestroy() {
        super.onStop();
        log.fine("On destroy, removing content observer");
        getActivity().getContentResolver().unregisterContentObserver(refreshContentObserver);
        adapter.destroy();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        log.fine("On list item click, id: " + id);
        onFeedEntrySelectionListener.onFeedEntrySelected(position, createFeedEntryDetails());
    }

    protected FeedEntryDetail[] createFeedEntryDetails() {
        // Collect all the information we need to know while paging through entry detail
        // views (previous id, next id, link comparison, etc.) but skip the last one
        List<FeedEntryDetail> details = new ArrayList<FeedEntryDetail>();
        for (int i = 0; i < getListAdapter().getCount(); i++) {
            if (getListAdapter().getItemViewType(i) == Adapter.IGNORE_ITEM_VIEW_TYPE)
                continue;
            Cursor cursor = (Cursor) getListAdapter().getItem(i);
            FeedEntryDetail detail = new FeedEntryDetail(
               getListAdapter().getItemId(i),
               DataType.read(cursor, FeedConfig._ID, FeedConfig.ALIAS_PREFIX),
               DataType.read(cursor, FeedEntry.LINK, FeedEntry.ALIAS_PREFIX)
            );
            details.add(detail);
        }
        return details.toArray(new FeedEntryDetail[details.size()]);
    }

    protected void setTitle(String title) {
        log.fine("Setting title of entry list: " + title);
        getView().findViewById(R.id.header).setVisibility(
           title != null ? View.VISIBLE : View.GONE
        );
        TextView titleTextView = (TextView) getView().findViewById(R.id.title);
        titleTextView.setText(title);
    }

    protected PagingCursorAdapterWrapper createListAdapter() {

        CursorAdapter cursorAdapter =
           new BindingCursorAdapter(
              getActivity(),
              R.layout.feed_entry_list_item_layout,
              bindings
           );

        return new PagingCursorAdapterWrapper(
           10,
           getActivity(),
           R.layout.feed_entry_list_loading_layout,
           cursorAdapter
        ) {

            @Override
            protected Cursor executeQuery(PagingQueryParameters params) {
                long id = stateProvider.getFeedReader().feedId;
                Uri uri = id == -1
                   ? FeedEntry.CONTENT_URI
                   : Uri.withAppendedPath(FeedEntry.CONTENT_URI_FOR_FEEDCONFIG, Long.toString(id));
                log.fine("Execute query for: " + uri);

                return getActivity().getContentResolver().query(
                   uri,
                   null,
                   PersistentEntity.RESTRICTION_LIMIT_OFFSET,
                   new String[]{Integer.toString(params.limit), Long.toString(params.offset)},
                   null
                );
            }

            @Override
            protected void onPreExecuteQuery(PagingQueryParameters params) {
                getPendingView().findViewById(R.id.spinner)
                   .startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.rotate360indefinitely));
            }

            @Override
            protected void onPostExecuteQuery(PagingCursorAdapterWrapper.PagingQueryParameters params) {
                if (!isVisible()) return; // We never know... the magic of Android life cycle.
                getPendingView().findViewById(R.id.spinner).clearAnimation();

                if (params.offset != 0) return; // Only do this once, for the initial query

                // TODO we don't have the feed name if the result is empty
                // Set or hide the title
                if (stateProvider.getFeedReader().feedId == -1 || getWrappedAdapter().getCount() == 0) {
                    setTitle(null);
                } else {
                    Cursor c = (Cursor) getWrappedAdapter().getItem(0);
                    setTitle(Feed.getFeedTitleOrUrl(c));
                }
                onFeedEntrySelectionListener.onFeedEntriesLoaded(getWrappedAdapter().getCount() > 0);
            }
        };
    }

    /* ############################################################################### */

    BindingCursorAdapter.Binding[] bindings = new BindingCursorAdapter.Binding[]{
       new BindingCursorAdapter.Binding<TextView>(
          R.id.feed_entry_title, FeedEntry.TITLE, FeedEntry.ALIAS_PREFIX
       ) {
           @Override
           public void setViewValue(View parent, TextView view, Cursor cursor) {
               super.setViewValue(parent, view, cursor);
               if (!isFeedEntryRead(cursor)) {
                   view.setTypeface(null, Typeface.BOLD);
                   Spannable txt = new SpannableString(view.getText());
                   txt.setSpan(new UnderlineSpan(), 0, txt.length(), 0);
                   view.setText(txt);
               } else {
                   view.setTypeface(null, Typeface.NORMAL);
               }
           }

           @Override
           public void setViewText(TextView v, String text) {
               // Decode HTML entities
               super.setViewText(v, Html.fromHtml(text).toString());
           }
       },
       new BindingCursorAdapter.Binding<TextView>(
          R.id.feed_title, Feed.TITLE, Feed.ALIAS_PREFIX
       ) {
           @Override
           public void setViewValue(View parent, TextView view, Cursor cursor) {
               super.setViewValue(parent, view, cursor);
               view.setVisibility(
                  stateProvider.getFeedReader().feedId == -1 ? View.VISIBLE : View.GONE
               );
               if (!isFeedEntryRead(cursor)) {
                   view.setBackgroundResource(R.color.light_background);
               } else {
                   view.setBackgroundResource(R.color.medium_background);
               }
           }

           @Override
           public void setViewText(TextView v, String text) {
               // Decode HTML entities
               super.setViewText(v, Html.fromHtml(text).toString());
           }
       },
       new BindingCursorAdapter.Binding<TextView>(
          R.id.feed_entry_date, FeedEntry.POLLED_DATE, FeedEntry.ALIAS_PREFIX
       ) {

           @Override
           public void setViewValue(View parent, TextView view, Cursor cursor, int index) {
               long polledDate = DataType.read(cursor, FeedEntry.POLLED_DATE, index);
               setViewText(view, SystemDateFormat.formatDayMonthTime(getActivity().getContentResolver(), polledDate));
           }
       },
       new BindingCursorAdapter.Binding<TextView>(
          R.id.feed_entry_description, FeedEntry.DESCRIPTION_VALUE, FeedEntry.ALIAS_PREFIX
       ) {
           @Override
           public void setViewValue(View parent, TextView view, Cursor cursor, int index) {

               Enum<FeedConfig.PreviewLength> previewLength =
                  DataType.read(cursor, FeedConfig.PREVIEW_LENGTH, FeedConfig.ALIAS_PREFIX);

               String description;
               if (previewLength == FeedConfig.PreviewLength.LESS) {
                   description = FeedEntry.getDescriptionAsText(cursor, 50);
               } else if (previewLength == FeedConfig.PreviewLength.REGULAR) {
                   description = FeedEntry.getDescriptionAsText(cursor, 150);
               } else if (previewLength == FeedConfig.PreviewLength.MORE) {
                   description = FeedEntry.getDescriptionAsText(cursor, 300);
               } else if (previewLength == FeedConfig.PreviewLength.ALL) {
                   description = FeedEntry.getDescriptionAsText(cursor, -1);
               } else {
                   view.setVisibility(View.GONE);
                   return;
               }

               setViewText(view, description);
               if (!isFeedEntryRead(cursor)) {
                   view.setTypeface(null, Typeface.BOLD);
               } else {
                   view.setTypeface(null, Typeface.NORMAL);
               }
           }
       }
    };

    protected boolean isFeedEntryRead(Cursor cursor) {
        return DataType.read(cursor, FeedEntry.IS_READ, FeedEntry.ALIAS_PREFIX);
    }

}
