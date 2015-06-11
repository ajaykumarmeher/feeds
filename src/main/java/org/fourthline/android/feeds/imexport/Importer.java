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

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;
import org.fourthline.android.feeds.content.FeedConfig;
import org.seamless.util.Exceptions;
import org.seamless.util.URIUtil;
import org.seamless.util.io.IO;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Christian Bauer
 */
public class Importer {

    final private static Logger log = Logger.getLogger(Importer.class.getName());

    public boolean importOPML(Activity activity, Uri fileUri) {
        Toast.makeText(activity, "Importing feeds from OPML file...", Toast.LENGTH_SHORT).show();
        File file = new File(fileUri.getPath());

        try {
            log.info("Reading OPML XML file: " + file);
            String opml = IO.readLines(file);

            OPMLParser parser = new OPMLParser();
            List<OPML.Outline> result = parser.read(opml);

            log.info("Importing feeds: " + result.size());
            int newFeeds = 0;
            for (OPML.Outline outline : result) {

                String feedURL = outline.xmlUrl;

                Cursor c = null;
                try {
                    String encodedURL = URIUtil.encodePathSegment(feedURL);
                    log.fine("Checking existing feed config: " + encodedURL);
                    Uri uri = Uri.withAppendedPath(
                        FeedConfig.CONTENT_URI_BY_URL, encodedURL);
                    c = activity.getContentResolver().query(uri, null, null, null, null);
                    if (c.moveToFirst()) {
                        log.info("Feed with this URL exists, skipping: " + feedURL);
                        continue;
                    }
                } finally {
                    if (c != null) c.close();
                }

                log.info("Importing new feed: " + feedURL);
                newFeeds++;
                FeedConfig feedConfig = outline.toFeedConfig();
                activity.getContentResolver().insert(
                    FeedConfig.CONTENT_URI, feedConfig.INSTANCE.getEntityValues()
                );
            }

            return newFeeds > 0;

        } catch (Exception ex) {
            Throwable cause = Exceptions.unwrap(ex);
            log.log(Level.WARNING, "Error importing OPML XML file: " + file, cause);
            Toast.makeText(activity, "Error importing OPML XML file, check the log: " + file, Toast.LENGTH_LONG).show();
        }
        return false;
    }
}
