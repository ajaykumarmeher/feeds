/*
 * Copyright (C) 2012 Paul Burke
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
package org.fourthline.android.feeds.filechooser;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.fourthline.android.feeds.R;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * List adapter for Files.
 *
 * @author paulburke (ipaulpro)
 * @author Christian Bauer
 * 
 */
public class FileListAdapter extends BaseAdapter {

    protected List<File> files = new ArrayList<File>();
	protected LayoutInflater inflater;

	public FileListAdapter(Context context) {
		inflater = LayoutInflater.from(context);
	}

	public ArrayList<File> getListItems() {
		return (ArrayList<File>) files;
	}

	public void setListItems(List<File> files) {
		this.files = files;
		notifyDataSetChanged();
	}

	public int getCount() {
		return files.size();
	}

	public void add(File file) {
		files.add(file);
	}

	public void clear() {
		files.clear();
	}

	public Object getItem(int position) {
		return files.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ViewHolder holder;

		if (row == null) {
			row = inflater.inflate(R.layout.file, parent, false);
			holder = new ViewHolder(row);
			row.setTag(holder);
		} else {
			// Reduce, reuse, recycle!
			holder = (ViewHolder) row.getTag();
		}

		// Get the file at the current position
		final File file = (File) getItem(position);

		// Set the TextView as the file name
		holder.nameView.setText(file.getName());

		// If the item is not a directory, use the file icon
		holder.iconView.setImageResource(file.isDirectory()
            ? R.drawable.folder_closed
            : R.drawable.file);

		return row;
	}

	static class ViewHolder {
		TextView nameView;
		ImageView iconView;

		ViewHolder(View row) {
			nameView = (TextView) row.findViewById(R.id.file_name);
			iconView = (ImageView) row.findViewById(R.id.file_icon);
		}
	}
}