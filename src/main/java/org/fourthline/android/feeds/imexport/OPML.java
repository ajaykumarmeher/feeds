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
package org.fourthline.android.feeds.imexport;

import android.database.Cursor;
import com.googlecode.sqb.query.DataType;
import org.fourthline.android.feeds.content.FeedConfig;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

import java.net.URL;

/**
 * @author Christian Bauer
 */
final public class OPML {

    public static final String DEFAULT_FILENAME = "Feeds-Export-OPML.xml";

    public enum ELEMENT {
        opml,
        head,
        title,
        body,
        outline;

        public static ELEMENT valueOrNullOf(String s) {
            try {
                return valueOf(s);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        public boolean equals(Node node) {
            return toString().equals(node.getLocalName());
        }
    }

    public enum ATTRIBUTE {
        version,
        title,
        text,
        type,
        xmlUrl,

        // Proprietary to Feeds
        refreshInterval,
        previewLength,
        entryPrefix,
        textColor,
        maxAgeDays,
        notifyNew
    }

    static public class Outline {

        public String xmlUrl;
        public Integer refreshInterval;
        public Enum<FeedConfig.PreviewLength> previewLength;
        public Enum<FeedConfig.EntryPrefix> entryPrefix;
        public Integer textColor;
        public Integer maxAgeDays;
        public Boolean notifyNew;

        public Outline(Attributes attributes) throws Exception {
            String value;

            if ((value = attributes.getValue(ATTRIBUTE.xmlUrl.name())) != null) {
                xmlUrl = new URL(value).toString();
            } else {
                throw new Exception("Missing attribute: " + ATTRIBUTE.xmlUrl);
            }

            refreshInterval =
                (value = attributes.getValue(ATTRIBUTE.refreshInterval.name())) != null
                    ? Integer.valueOf(value)
                    : FeedConfig.DEFAULT_REFRESH_INTERVAL;

            try {
                previewLength =
                    (value = attributes.getValue(ATTRIBUTE.previewLength.name())) != null
                        ? FeedConfig.PreviewLength.valueOf(value)
                        : FeedConfig.DEFAULT_PREVIEW_LENGTH;
            } catch (IllegalArgumentException ex) {
                previewLength = FeedConfig.DEFAULT_PREVIEW_LENGTH;
            }

            try {
                entryPrefix =
                    (value = attributes.getValue(ATTRIBUTE.entryPrefix.name())) != null
                        ? FeedConfig.EntryPrefix.valueOf(value)
                        : FeedConfig.DEFAULT_ENTRY_PREFIX;
            } catch (IllegalArgumentException ex) {
                entryPrefix = FeedConfig.DEFAULT_ENTRY_PREFIX;
            }

            textColor =
                (value = attributes.getValue(ATTRIBUTE.textColor.name())) != null
                    ? Integer.valueOf(value)
                    : FeedConfig.DEFAULT_TEXT_COLOR;

            maxAgeDays =
                (value = attributes.getValue(ATTRIBUTE.maxAgeDays.name())) != null
                    ? Integer.valueOf(value)
                    : FeedConfig.DEFAULT_MAX_AGE_DAYS;

            notifyNew =
                (value = attributes.getValue(ATTRIBUTE.notifyNew.name())) != null
                    ? Boolean.valueOf(value)
                    : FeedConfig.DEFAULT_NOTIFY_NEW;
        }
        
        public Outline(Cursor c) {
            xmlUrl = DataType.read(c, FeedConfig.URL, FeedConfig.ALIAS_PREFIX);
            refreshInterval = DataType.read(c, FeedConfig.REFRESH_INTERVAL, FeedConfig.ALIAS_PREFIX);
            previewLength = DataType.read(c, FeedConfig.PREVIEW_LENGTH, FeedConfig.ALIAS_PREFIX);
            entryPrefix = DataType.read(c, FeedConfig.ENTRY_PREFIX, FeedConfig.ALIAS_PREFIX);
            textColor = DataType.read(c, FeedConfig.TEXT_COLOR, FeedConfig.ALIAS_PREFIX);
            maxAgeDays = DataType.read(c, FeedConfig.MAX_AGE_DAYS, FeedConfig.ALIAS_PREFIX);
            notifyNew = DataType.read(c, FeedConfig.NOTIFY_NEW, FeedConfig.ALIAS_PREFIX);
        }

        public FeedConfig toFeedConfig() {
            return new FeedConfig(
                null,
                xmlUrl,
                refreshInterval,
                previewLength,
                entryPrefix,
                textColor,
                FeedConfig.DEFAULT_LAST_REFRESH,
                FeedConfig.DEFAULT_LAST_REFRESH_ETAG,
                maxAgeDays,
                notifyNew
            );
        }

        public void setAttributes(Element element) {
            element.setAttribute(ATTRIBUTE.xmlUrl.name(), xmlUrl);
            element.setAttribute(ATTRIBUTE.refreshInterval.name(), refreshInterval.toString());
            element.setAttribute(ATTRIBUTE.previewLength.name(), previewLength.toString());
            element.setAttribute(ATTRIBUTE.entryPrefix.name(), entryPrefix.toString());
            element.setAttribute(ATTRIBUTE.textColor.name(), textColor.toString());
            element.setAttribute(ATTRIBUTE.maxAgeDays.name(), maxAgeDays.toString());
            element.setAttribute(ATTRIBUTE.notifyNew.name(), notifyNew.toString());
        }

        @Override
        public String toString() {
            return xmlUrl;
        }
    }
}
