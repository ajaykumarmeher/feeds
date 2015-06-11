/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.sqb.query.impl;

import com.googlecode.sqb.query.Projection;
import com.googlecode.sqb.query.SelectClause;
import com.googlecode.sqb.sql.SelectQuantifier;

import java.util.Collection;
import java.util.HashSet;

public class SelectClauseImpl implements SelectClause {

    private SelectQuantifier selectQuantifier = SelectQuantifier.NONE;

    private final Collection<Projection> projections = new HashSet<Projection>();

    @Override
    public SelectQuantifier getQuantifier() {
        return selectQuantifier;
    }

    @Override
    public Collection<Projection> getProjections() {
        return projections;
    }

    public void addProjection(Projection projection) {
        projections.add(projection);
    }

    public void setQuantifier(SelectQuantifier selectQuantifier) {
        this.selectQuantifier = selectQuantifier;
    }

}
