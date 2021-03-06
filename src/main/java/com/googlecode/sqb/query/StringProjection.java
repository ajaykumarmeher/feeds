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

import com.googlecode.sqb.util.AbstractHolder;

public class StringProjection extends AbstractHolder<String> implements Projection {

    protected String preferredAlias;

    public StringProjection(String value) {
        super(value);
    }

    public StringProjection(String value, String preferredAlias) {
        super(value);
        this.preferredAlias = preferredAlias;
    }

    @Override
    public String getPreferredAlias() {
        return preferredAlias != null
           ? preferredAlias
           : getValue().trim().toLowerCase().charAt(0) + "";
    }

    @Override
    public String toString() {
        return getValue();
    }
}
