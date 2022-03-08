/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.annotation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.config.Scope;
import cn.taketoday.lang.Nullable;

/**
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class SimpleMapScope implements Scope, Serializable {

  private final Map<String, Object> map = new HashMap<>();

  private final List<Runnable> callbacks = new ArrayList<>();

  public SimpleMapScope() {
  }

  public final Map<String, Object> getMap() {
    return this.map;
  }

  @Override
  public Object get(String name, Supplier<?> objectFactory) {
    synchronized(this.map) {
      Object scopedObject = this.map.get(name);
      if (scopedObject == null) {
        scopedObject = objectFactory.get();
        this.map.put(name, scopedObject);
      }
      return scopedObject;
    }
  }

  @Override
  public Object remove(String name) {
    synchronized(this.map) {
      return this.map.remove(name);
    }
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {
    this.callbacks.add(callback);
  }

  @Nullable
  @Override
  public Object resolveContextualObject(String key) {
    return null;
  }

  public void close() {
    for (Iterator<Runnable> it = this.callbacks.iterator(); it.hasNext(); ) {
      Runnable runnable = it.next();
      runnable.run();
    }
  }

  @Override
  public String getConversationId() {
    return null;
  }

}
