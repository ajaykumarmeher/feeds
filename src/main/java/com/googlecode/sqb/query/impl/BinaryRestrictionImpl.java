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

import com.googlecode.sqb.query.BinaryRestriction;
import com.googlecode.sqb.query.Expression;
import com.googlecode.sqb.sql.Operator;

public class BinaryRestrictionImpl extends RestrictionImpl implements BinaryRestriction {

    private final Expression other;

    public BinaryRestrictionImpl(Operator operator, Expression expression, Expression other) {
        super(operator, expression);
        this.other = other;
    }

    @Override
    public Expression getOtherExpression() {
        return other;
    }
}
