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

import com.googlecode.sqb.query.Restriction;
import com.googlecode.sqb.vendor.Context;
import com.googlecode.sqb.vendor.Serializer;

class RestrictionSerializer implements Serializer<Restriction> {

    @Override
    public String serialize(Restriction restriction, Context context) {
        final StringBuilder sb = new StringBuilder();
        sb.append(restriction.getOperator().getSqlValue()).append(SPACE)
          .append(context.serialize(restriction.getExpression()));
        return sb.toString();
    }
}