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

package com.googlecode.sqb.vendor.sql92;

import com.googlecode.sqb.query.OrderBy;
import com.googlecode.sqb.vendor.Context;
import com.googlecode.sqb.vendor.Serializer;

class OrderBySerializer implements Serializer<OrderBy> {

    @Override
    public String serialize(OrderBy orderBy, Context context) {
        final StringBuilder sb = new StringBuilder();
        final String sourceAlias = context.getAlias(orderBy.getSource());
        if (null != sourceAlias) {
            sb.append(sourceAlias).append(PERIOD);
        }
        sb.append(orderBy.name()).append(SPACE).append(orderBy.getOrder().toString());

        return sb.toString();
    }
}
