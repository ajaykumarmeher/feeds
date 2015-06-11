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

import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

// TODO: Document
public abstract class PagingCursorAdapterWrapper<T extends CursorAdapter> extends AdapterWrapper<T> {

    final private static Logger log = Logger.getLogger(PagingCursorAdapterWrapper.class.getName());

    protected final Context context;
    protected final int pageSize;
    protected int pendingResource = -1;
    protected View pendingView = null;

    protected AtomicBoolean continueLoading = new AtomicBoolean(true);
    protected AtomicBoolean requiresRefresh = new AtomicBoolean(false);
    /* The following members are only ever accessed by a single (UI) thread */
    protected int currentOffset = 0;
    protected List<Cursor> cursors = new ArrayList<Cursor>();

    public PagingCursorAdapterWrapper(int pageSize, Context context, int pendingResource, T cursorAdapter) {
        super(cursorAdapter);
        this.context = context;
        this.pageSize = pageSize;
        this.pendingResource = pendingResource;
    }

    protected void createPendingView(ViewGroup parent) {
        if (context != null && pendingResource != -1) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            pendingView = inflater.inflate(pendingResource, parent, false);
        } else {
            throw new RuntimeException("You must either override getPendingView() or supply a pending View resource via the constructor");
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getWrappedAdapter().getCount()) {
            return (IGNORE_ITEM_VIEW_TYPE); // The pending view is ignored
        }
        return (super.getItemViewType(position));
    }

    @Override
    public int getViewTypeCount() {
        return (super.getViewTypeCount() + 1);
    }

    @Override
    public int getCount() {
        if (continueLoading.get()) {
            return (super.getCount() + 1); // There is always one more
        }
        return (super.getCount());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= super.getCount() && continueLoading.get()) {
            log.fine("Requested view position is last in list, need to load more data: " + position);

            if (getPendingView() == null) {
                log.fine("No pending view, creating pending view");
                createPendingView(parent);
            }

            executeQueryInBackground(false);

            return getPendingView();
        }
        return (super.getView(position, convertView, parent));
    }

    public View getPendingView() {
        return pendingView;
    }

    public void setRefreshRequired(boolean immediately) {
        log.fine("Setting refresh required, immediately: " + immediately);
        if (!requiresRefresh.get() && immediately) {
            requiresRefresh.set(true);
            refreshIfNecessary();
        } else {
            log.fine("Resetting adapter, setting refresh required");
            requiresRefresh.set(true);
        }
    }

    public void refreshIfNecessary() {
        log.fine("Refreshing if necessary: "+ requiresRefresh.get());
        if (requiresRefresh.get()) {
            executeQueryInBackground(true);
        }
    }

    public void destroy() {
        log.fine("Destroying and closing all cursors");
        if (getWrappedAdapter().getCursor() != null) {
            getWrappedAdapter().getCursor().close();
        }
        cursors.clear();
    }

    protected void executeQueryInBackground(boolean reset) {
        log.fine("Executing query task in background, reset: " + reset);
        if (reset)
            currentOffset = 0;
        PagingQueryParameters params = new PagingQueryParameters(reset, pageSize, currentOffset);
        currentOffset = currentOffset + pageSize; // Increment "after" querying
        new QueryTask().execute(params);
    }

    protected void onPreExecuteQuery(PagingQueryParameters params) {
    }

    protected void onPostExecuteQuery(PagingQueryParameters params) {
    }

    protected abstract Cursor executeQuery(PagingQueryParameters params);

    public class PagingQueryParameters {

        public boolean reset;
        public int limit;
        public int offset;

        PagingQueryParameters(boolean reset, int limit, int offset) {
            this.reset = reset;
            this.limit = limit;
            this.offset = offset;
        }
    }

    class QueryTask extends AsyncTask<PagingQueryParameters, Void, Cursor> {

        PagingQueryParameters params;

        @Override
        protected void onPreExecute() {
            onPreExecuteQuery(params);
        }

        @Override
        protected Cursor doInBackground(PagingQueryParameters... queryParams) {
            try {
                params = queryParams[0];

                log.fine("Executing query in background with offset: " + params.offset);
                Cursor cursor = executeQuery(params);

                boolean moreDataAvailable = cursor.getCount() == pageSize;
                log.fine("Obtained cursor, is more data possibly available: " + moreDataAvailable);
                continueLoading.set(moreDataAvailable);

                return cursor;
            } catch (Exception ex) {
                log.warning("Error executing query: " + ex);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            super.onPostExecute(cursor);

            // Reset as late as possible, otherwise the idiotic "empty view" is shown
            if (params.reset) {
                log.fine("Reset flag, clearing and closing existing cursors");
                cursors.clear();
                Cursor current = getWrappedAdapter().swapCursor(null);
                if (current != null) current.close();
                requiresRefresh.set(false);
            }

            if (cursor != null) {
                log.fine("Merging query result cursor with existing cursors");
                cursors.add(cursor);
                getWrappedAdapter().swapCursor(new MergeCursor(cursors.toArray(new Cursor[cursors.size()])));
            }

            onPostExecuteQuery(params);
        }
    }
}