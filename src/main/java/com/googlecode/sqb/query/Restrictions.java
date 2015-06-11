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

package com.googlecode.sqb.query;

import com.googlecode.sqb.query.impl.BinaryRestrictionImpl;
import com.googlecode.sqb.query.impl.MultiRestrictionImpl;
import com.googlecode.sqb.query.impl.StringExpression;
import com.googlecode.sqb.sql.BinaryOperator;

import java.util.HashSet;
import java.util.Set;

public class Restrictions {

    private Restrictions() {
        // private
    }


    public static Restriction and(Restriction left, Restriction right) {
        return new BinaryRestrictionImpl(BinaryOperator.AND, left, right);
    }

    public static Restriction or(Restriction left, Restriction right) {
        return new BinaryRestrictionImpl(BinaryOperator.OR, left, right);
    }

    public static Restriction eq(Projection left, String right) {
        return new BinaryRestrictionImpl(BinaryOperator.EQUAL, left, new StringExpression(right));
    }

    public static Restriction eq(Projection left, Expression right) {
        return new BinaryRestrictionImpl(BinaryOperator.EQUAL, left, right);
    }

    public static Restriction in(Projection left, Set<String> right) {
        final Set<Expression> expressions = new HashSet<Expression>();
        for (String s : right) {
            expressions.add(new StringExpression(s));
        }
        return new MultiRestrictionImpl(BinaryOperator.IN, left, expressions);
    }
}
