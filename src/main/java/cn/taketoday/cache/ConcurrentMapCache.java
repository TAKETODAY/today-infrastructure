/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.util.function.UnaryOperator;

import cn.taketoday.core.Constant;
import cn.taketoday.core.utils.ConcurrentCache;

/**
 * @author TODAY <br>
 * 2019-12-17 12:29
 */
public class ConcurrentMapCache extends AbstractMappingFunctionCache {

  private final ConcurrentCache<Object, Object> store;

  public ConcurrentMapCache() {
    this(Constant.DEFAULT);
  }

  public ConcurrentMapCache(String name) {
    this(name, 256);
  }

  public ConcurrentMapCache(String name, int size) {
    this(name, new ConcurrentCache<>(size));
  }

  protected ConcurrentMapCache(String name, ConcurrentCache<Object, Object> store) {
    this.setName(name);
    this.store = store;
  }

  @Override
  protected Object getInternal(Object key, UnaryOperator<Object> mappingFunction) {
    return store.get(key, mappingFunction);
  }

  @Override
  public void evict(Object key) {
    store.remove(key);
  }

  @Override
  public void clear() {
    store.clear();
  }

  @Override
  protected Object lookupValue(Object key) {
    return store.get(key);
  }

  @Override
  protected void putInternal(Object key, Object value) {
    store.put(key, value);
  }

}
