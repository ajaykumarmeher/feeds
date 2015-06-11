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

package com.googlecode.sqb.vendor;

import com.googlecode.sqb.query.Query;

public interface QuerySerializer<T extends Query> extends Serializer<T> {

    public static final String SELECT = "SELECT";

    public static final String FROM = "FROM";

    public static final String WHERE = "WHERE";

    public static final String HAVING = "HAVING";

    public static final String GROUP_BY = "GROUP BY";

    public static final String ORDER_BY = "ORDER BY";

}
