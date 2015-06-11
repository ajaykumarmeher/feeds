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

package org.fourthline.android.feeds.model;

import java.io.Serializable;

public class FeedEntryDetail implements Serializable {

    private static final long serialVersionUID = -5440904920804985715L;

    public long id;

    public long feedConfigId;

    public String link;

    public boolean isRead;

    public FeedEntryDetail(long id, long feedConfigId, String link) {
        this.id = id;
        this.feedConfigId = feedConfigId;
        this.link = link;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    @Override
    public String toString() {
        return "ID: " + id + ", Feed ID: " + feedConfigId + ", Link: " + link + ", Read: " + isRead;
    }

}
