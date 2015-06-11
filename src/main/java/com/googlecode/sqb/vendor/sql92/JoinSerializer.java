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

package com.googlecode.sqb.vendor.sql92;

import com.googlecode.sqb.query.Join;
import com.googlecode.sqb.query.Restriction;
import com.googlecode.sqb.sql.JoinType;
import com.googlecode.sqb.vendor.Context;
import com.googlecode.sqb.vendor.Serializer;

class JoinSerializer implements Serializer<Join> {

    @Override
    @SuppressWarnings("unchecked")
    public String serialize(Join join, Context context) {
        final StringBuilder sb = new StringBuilder();
        final Restriction restriction = join.getRestriction();

        if (null == restriction && join.getJoinType() == JoinType.CROSS) {
            sb.append(COMMA);
        } else {
            sb.append(SPACE).append(join.getJoinType().getSqlValue());
        }

        sb.append(SPACE).append(context.serialize(join.getSource()));


        final String alias = context.getAlias(join);
        if (null != alias) {
            sb.append(SPACE).append(ALIAS).append(SPACE).append(alias);
        }

        if (null != restriction) {
            sb.append(SPACE).append(ON).append(SPACE).append(context.serialize(restriction));
        }

        return sb.toString();
    }
}
