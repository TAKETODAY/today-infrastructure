/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *   
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.cache;

import java.io.Serializable;

/**
 * 
 * @author TODAY <br>
 *         2019-11-02 00:23
 */
public abstract class AbstractCache implements Cache {

    private static enum EmptyObject implements Serializable {
        EMPTY_OBJECT
    }

    private String name;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public final Object get(final Object key) {
        return toRealValue(lookupValue(key));
    }

    protected final static Object toStoreValue(final Object userValue) {
        return userValue == null ? EmptyObject.EMPTY_OBJECT : userValue;
    }

    protected final static Object toRealValue(final Object cachedValue) {
        return cachedValue == EmptyObject.EMPTY_OBJECT ? null : cachedValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Object key, final Class<T> type) {
        final Object value = get(key);
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T get(Object key, CacheCallback<T> valueLoader) throws CacheValueRetrievalException {
        return (T) toRealValue(getInternal(key, valueLoader));
    }

    protected <T> Object getInternal(Object key, CacheCallback<T> valueLoader) throws CacheValueRetrievalException {
        Object ret = lookupValue(key);
        if (ret == null) {
            ret = lookupValue(key, valueLoader);
        }
        return ret;
    }

    protected abstract Object lookupValue(Object key);

    protected <T> Object lookupValue(final Object key, final CacheCallback<T> valueLoader)
            throws CacheValueRetrievalException //
    {
        try {
            return valueLoader.call();
        }
        catch (Throwable e) {
            throw new CacheValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public final void put(final Object key, final Object value) {
        putInternal(key, toStoreValue(value));
    }

    protected abstract void putInternal(Object key, Object value);

}
