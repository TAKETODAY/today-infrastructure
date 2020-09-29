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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.cache;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.Constant;

/**
 * @author TODAY <br>
 * 2019-02-28 18:10
 */
public class DefaultMapCache extends AbstractCache {

  private final Map<Object, Object> store;

  public DefaultMapCache() {
    this(Constant.DEFAULT);
  }

  public DefaultMapCache(String name) {
    this(name, 256);
  }

  public DefaultMapCache(String name, int size) {
    this(name, new HashMap<>(size));
  }

  protected DefaultMapCache(String name, Map<Object, Object> store) {
    this.setName(name);
    this.store = store;
  }

  @Override
  protected Object lookupValue(Object key) {
    return this.store.get(key);
  }

  @Override
  public void evict(Object key) {
    this.store.remove(key);
  }

  @Override
  public void clear() {
    this.store.clear();
  }

  @Override
  protected void putInternal(Object key, Object value) {
    this.store.put(key, value);
  }

  @Override
  protected <T> Object getInternal(Object key, CacheCallback<T> valueLoader) {
    return this.store.computeIfAbsent(key, k -> lookupValue(k, valueLoader));
  }

}
