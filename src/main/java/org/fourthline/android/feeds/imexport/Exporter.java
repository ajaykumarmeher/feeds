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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;
import org.fourthline.android.feeds.content.FeedConfig;
import org.seamless.util.Exceptions;
import org.seamless.util.io.IO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Christian Bauer
 */
public class Exporter {

    final private static Logger log = Logger.getLogger(Exporter.class.getName());

    public void exportOPML(final Activity activity, Uri fileUri) {
        Toast.makeText(activity, "Exporting feeds to OPML file...", Toast.LENGTH_SHORT).show();

        final File file = new File(fileUri.getPath(), OPML.DEFAULT_FILENAME);

        try {
            List<OPML.Outline> outlines = new ArrayList<OPML.Outline>();
            Cursor c = null;
            try {
                log.fine("Loading feed configs");
                c = activity.getContentResolver().query(FeedConfig.CONTENT_URI, null, null, null, null);
                while (c.moveToNext()) {
                    OPML.Outline outline = new OPML.Outline(c);
                    outlines.add(outline);
                }
            } finally {
                if (c != null) c.close();
            }

            log.fine("Generating XML for feed configs: " + outlines.size());

            OPMLParser parser = new OPMLParser();
            final String result = parser.write(outlines);

            if (file.exists()) {
                log.fine("File exists, confirming overwrite: " + file);
                new AlertDialog.Builder(activity)
                    .setTitle("Overwrite file?")
                    .setMessage("OMPL export file exists in this directory, are you sure you want to overwrite it?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            writeFile(activity, file, result);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            } else {
                log.fine("File doesn't exist, creating: " + file);
                file.createNewFile();
                writeFile(activity, file, result);
            }

        } catch (Exception ex) {
            Throwable cause = Exceptions.unwrap(ex);
            log.log(Level.WARNING, "Error writing OPML XML file: " + file, cause);
            Toast.makeText(activity, "Error writing OPML XML file, check the log: " + file, Toast.LENGTH_LONG).show();
        }
    }

    protected void writeFile(Activity activity, File file, String xml) {
        try {
            log.info("Writing OPML XML to file: " + file);
            IO.writeUTF8(file, xml);
        } catch (Exception ex) {
            Throwable cause = Exceptions.unwrap(ex);
            log.log(Level.WARNING, "Error writing OPML XML file: " + file, cause);
            Toast.makeText(activity, "Error writing OPML XML file, check the log: " + file, Toast.LENGTH_LONG).show();
        }
    }
}
