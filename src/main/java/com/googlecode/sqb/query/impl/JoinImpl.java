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

import com.googlecode.sqb.query.Join;
import com.googlecode.sqb.query.Restriction;
import com.googlecode.sqb.query.Source;
import com.googlecode.sqb.sql.JoinType;

public class JoinImpl implements Join {

    private final Source source;
    private final JoinType joinType;
    private final Restriction restriction;

    public JoinImpl(Source source, JoinType joinType, Restriction restriction) {
        this.source = source;
        this.joinType = joinType;
        this.restriction = restriction;
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public Restriction getRestriction() {
        return restriction;
    }

    @Override
    public String getPreferredAlias() {
        return source.getPreferredAlias();
    }
}
