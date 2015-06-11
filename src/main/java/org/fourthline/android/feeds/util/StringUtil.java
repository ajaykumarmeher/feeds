/*
 * Copyright (C) 2011 Teleal GmbH, Switzerland
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

package org.fourthline.android.feeds.util;

import java.util.Arrays;

public class StringUtil {

    public static String truncate(String string, int length, String appendString) {
        if (string.length() <= length) return string;
        return string.substring(0, length - 1) + appendString;
    }

    public static String truncateOnWordBoundary(String string, int length, String appendString) {
        if (string.length() <= length) return string;

        char[] chars = string.toCharArray();
        StringBuffer buffer = new StringBuffer();
        String result = "";
        int lastWhitespace = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ' ') lastWhitespace = i;
            buffer.append(chars[i]);

            if (i >= length) {
                result = buffer.substring(0, lastWhitespace);
                break;
            }
        }
        return result + appendString;
    }

    public static String replaceControlChars(String original) {
        return original.replaceAll("[\\a\\e\\f\\n\\r\\t]", " ");
    }

    public static String replaceMultipleWhitespace(String original) {
        return original.replaceAll("(\\s)+", " ");
    }

    public static String removeHtml(String original) {
        if (original == null) return null;
        return android.text.Html.fromHtml(original).toString();
        //return original.replaceAll("\\<([a-zA-Z]|/){1}?.*?\\>", "");
    }

    public static String[] concatAll(String[] first, String... rest) {
        int totalLength = first.length + rest.length;
        String[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        int x = 0;
        for (int i = offset; i < totalLength; i++) {
            result[i] = rest[x++];
        }
        return result;
    }

    public static String[] concatAll(String[] first, String[]... rest) {
        int totalLength = first.length;
        for (String[] array : rest) {
            totalLength += array.length;
        }
        String[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (String[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

}
