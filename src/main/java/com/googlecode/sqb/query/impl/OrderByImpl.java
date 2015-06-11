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

package com.googlecode.sqb.query.impl;

import com.googlecode.sqb.query.Column;
import com.googlecode.sqb.query.OrderBy;
import com.googlecode.sqb.query.Source;
import com.googlecode.sqb.sql.Order;

// TODO: This is actually a projection/expression with additional order fragment?
public class OrderByImpl implements OrderBy {

    private final Column column;
    private final Order order;

    public OrderByImpl(Column column, Order order) {
        this.column = column;
        this.order = order;
    }

    @Override
    public Source getSource() {
        return column.getSource();
    }

    @Override
    public String name() {
        return column.getName();
    }

    @Override
    public String getPreferredAlias() {
        return column.getPreferredAlias();
    }

    @Override
    public Order getOrder() {
        return order;
    }
}
