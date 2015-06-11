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
import java.util.Arrays;

public class FeedEntryDetails implements Serializable {

    private static final long serialVersionUID = -3285632845230086612L;

    public int position = -1;

    public FeedEntryDetail[] details;

    public FeedEntryDetails(int position, FeedEntryDetail[] details) {
        this.position = position;
        this.details = details;
    }

    @Override
    public String toString() {
        return "Position: " + position + ", Details: " + Arrays.toString(details);
    }
}
