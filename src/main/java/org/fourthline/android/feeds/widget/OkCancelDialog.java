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

package org.fourthline.android.feeds.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class OkCancelDialog {

    public static AlertDialog newInstance(Context context, int questionText, int okText, int cancelText,
                                          DialogInterface.OnClickListener okListener) {
        return newInstance(context, questionText, okText, cancelText, okListener,
           new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialogInterface, int i) {
               }
           }
        );
    }

    public static AlertDialog newInstance(Context context, int questionText, int okText, int cancelText,
                                          DialogInterface.OnClickListener okListener,
                                          DialogInterface.OnClickListener cancelListener) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setMessage(questionText)
           .setPositiveButton(okText, okListener)
           .setNegativeButton(cancelText, cancelListener);
        return builder.create();
    }


}
