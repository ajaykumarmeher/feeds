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

import com.googlecode.sqb.Aliasable;
import com.googlecode.sqb.vendor.Context;
import com.googlecode.sqb.vendor.Serializer;

import java.util.Map;


class UpdatableContext implements Context {

    private final Map<Class<?>, Serializer> serializers;
    private final Map<Aliasable, String> aliases;

    UpdatableContext(Map<Class<?>, Serializer> serializers, Map<Aliasable, String> aliases) {
        this.serializers = serializers;
        this.aliases = aliases;
    }


    @Override
    public Context clone(Map<Aliasable, String> aliases) {
        return new UpdatableContext(serializers, aliases);
    }

    /**
     * Recursive serializing using the same context
     *
     * @param object object to serialize
     * @param <T>    type of serializable object
     * @return String
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> String serialize(T object) {
        final Class clazz = object.getClass();
        final Serializer<T> serializer = serializers.get(clazz);
        if (null != serializer) {
            return serializer.serialize(object, this);
        }

        final Class<?>[] interfaces = clazz.getInterfaces();
        for (Class iface : interfaces) {
            final Serializer<T> s = serializers.get(iface);
            if (null != s) {
                serializers.put(clazz, s);
                return s.serialize(object, this);
            }
        }

        throw new UnsupportedOperationException("No serializer for " + object);
    }

    @Override
    public String getAlias(Aliasable aliasable) {
        return aliases.get(aliasable);
    }
}
