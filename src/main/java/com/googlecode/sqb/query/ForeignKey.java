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

package com.googlecode.sqb.query;

public class ForeignKey {

    public enum Action {
        NO_ACTION("no action"),
        RESTRICT("restrict"),
        SET_NULL("set null"),
        SET_DEFAULT("set default"),
        CASCADE("cascade");

        public String sql;

        Action(String sql) {
            this.sql = sql;
        }
    }

    protected final Column references;
    protected final Action onDelete;
    protected final Action onUpdate;

    public ForeignKey(Column references) {
        this(references, null);
    }

    public ForeignKey(Column references, Action onDelete) {
        this(references, onDelete, null);
    }

    public ForeignKey(Column references, Action onDelete, Action onUpdate) {
        this.references = references;
        this.onDelete = onDelete;
        this.onUpdate = onUpdate;
    }

    public Column getReferences() {
        return references;
    }

    public Action getOnDelete() {
        return onDelete;
    }

    public Action getOnUpdate() {
        return onUpdate;
    }
}
