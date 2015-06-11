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

import com.googlecode.sqb.query.FromClause;
import com.googlecode.sqb.query.Join;
import com.googlecode.sqb.query.Source;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FromClauseImpl implements FromClause {

    private Source source;

    private final List<Join> Joins = new LinkedList<Join>();

    private final List<Join> joinedTablesView = Collections.unmodifiableList(Joins);


    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public List<Join> getJoins() {
        return joinedTablesView;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public void addJoin(Join Join) {
        Joins.add(Join);
    }

}
