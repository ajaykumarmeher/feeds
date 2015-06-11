/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fourthline.android.feeds.widget;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import com.googlecode.sqb.query.Projection;
import org.fourthline.android.feeds.database.ValueAdapter;

import java.util.logging.Logger;

// TODO: Document
public class BindingCursorAdapter extends ResourceCursorAdapter {

    final private static Logger log = Logger.getLogger(BindingCursorAdapter.class.getName());

    protected Binding[] bindings;

    // TODO Constructors
    public BindingCursorAdapter(Context context, int layout, Binding... bindings) {
        super(context, layout, null, false);
        this.bindings = bindings;
    }

    public Binding[] getBindings() {
        return bindings;
    }

    public void setBindings(Binding[] bindings) {
        this.bindings = bindings;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        for (Binding binding : getBindings()) {
            View targetView = view.findViewById(binding.getViewResId());
            if (targetView == null) {
                throw new IllegalArgumentException("Target view not found: " + binding.getViewResId());
            }
            binding.setViewValue(view, targetView, cursor);
        }
    }

    @Override
    public boolean hasStableIds() {
        return true; // I can't find the default value for this. Of course we use a real DBMS with stable PKs.
    }

    public static class Binding<V extends View> {

        protected int viewResId;
        protected Projection projection;
        protected String prefix;

        public Binding(int viewResId, Projection projection) {
            this.viewResId = viewResId;
            this.projection = projection;
        }

        public Binding(int viewResId, Projection projection, String prefix) {
            this.viewResId = viewResId;
            this.projection = projection;
            this.prefix = prefix;
        }

        public int getViewResId() {
            return viewResId;
        }

        public Projection getProjection() {
            return projection;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getProjectionAlias() {
            return
               (getPrefix() != null && getPrefix().length() > 0
                  ? getPrefix() + ValueAdapter.PREFIX_SEPARATOR : "")
                  + projection.getPreferredAlias();
        }

        public void setViewValue(View parent, V view, Cursor cursor) {
            int index = cursor.getColumnIndex(getProjectionAlias());
            if (index == -1) {
                throw new IllegalArgumentException("No column in cursor for projection alias: " + getProjectionAlias());
            }
            setViewValue(parent, view, cursor, index);
        }

        public void setViewValue(View parent, V view, Cursor cursor, int index) {
            String text = cursor.getString(index);
            if (text == null) {
                text = "";
            }
            if (view instanceof TextView) {
                setViewText((TextView) view, text);
            } else if (view instanceof ImageView) {
                setViewImage((ImageView) view, text);
            } else {
                throw new IllegalArgumentException(
                   "Unsupported view type for binding, override setViewValue(): " + view.getClass().getName()
                );
            }
        }

        public void setViewImage(ImageView v, String value) {
            try {
                v.setImageResource(Integer.parseInt(value));
            } catch (NumberFormatException nfe) {
                v.setImageURI(Uri.parse(value));
            }
        }

        public void setViewText(TextView v, String text) {
            v.setText(text);
        }
    }

}