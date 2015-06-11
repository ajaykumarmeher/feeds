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
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.googlecode.sqb.query.DataType;
import org.fourthline.android.feeds.content.Feed;
import org.fourthline.android.feeds.content.FeedConfig;
import org.fourthline.android.feeds.model.FeedReader;
import org.fourthline.android.feeds.widget.BindingCursorAdapter;

import java.util.logging.Logger;

public class FeedListFragment extends ListFragment
   implements LoaderManager.LoaderCallbacks<Cursor> {

    final private static Logger log = Logger.getLogger(FeedListFragment.class.getName());

    protected FeedReader.Provider stateProvider;
    protected OnFeedConfigSelectedListener onFeedConfigSelectedListener;
    protected BindingCursorAdapter feedConfigCursorAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            stateProvider = (FeedReader.Provider) activity;
            onFeedConfigSelectedListener = (OnFeedConfigSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(getClass().getName() + " doesn't implement required listeners");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        log.fine("On activity created");

        getLoaderManager().initLoader(Constants.FEED_LIST_LOADER_ID, null, this);
        feedConfigCursorAdapter = onCreateCursorAdapter();
        setListAdapter(feedConfigCursorAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log.fine("On create view");
        return inflater.inflate(R.layout.feed_list_layout, container, false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        log.fine("On create loader for: " + FeedConfig.CONTENT_URI);
        return new CursorLoader(getActivity(), FeedConfig.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        log.fine("Feed configs loaded, swapping cursor");
        feedConfigCursorAdapter.swapCursor(cursor);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                selectPosition(stateProvider.getFeedReader().feedPosition);
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        log.fine("Feed configs loader reset, nulling cursor");
        feedConfigCursorAdapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        log.fine("On list item click, position: " + position);
        String feedConfigUrl =
           DataType.read((Cursor)getListAdapter().getItem(position), FeedConfig.URL, FeedConfig.ALIAS_PREFIX);
        onFeedConfigSelectedListener.onFeedConfigSelected(position, id, feedConfigUrl);
    }

    protected void selectPosition(int position) {
        log.fine("Selected position argument: " + position);
        ListView lv = getListView();
        int count = getListAdapter().getCount();
        log.fine("List adapter item count: " + count);
        if (count == 0)
            position = -1;
        else if (position >= count)
            position = count - 1;
        log.fine("Selecting position: " + position);
        if (position >= 0) {
            lv.setItemChecked(position, true);
            lv.setSelectionFromTop(position, 20);
        } else {
            lv.clearChoices();
        }
    }

    protected BindingCursorAdapter onCreateCursorAdapter() {
        return new BindingCursorAdapter(
           getActivity(),
           R.layout.feed_list_item_layout,
           new BindingCursorAdapter.Binding<TextView>(
              R.id.feed_title, Feed.TITLE, Feed.ALIAS_PREFIX
           ) {
               @Override
               public void setViewValue(View parent, TextView view, Cursor cursor, int index) {
                   setViewText(view, Feed.getFeedTitleOrUrl(cursor));
               }
           }
        );
    }

}
