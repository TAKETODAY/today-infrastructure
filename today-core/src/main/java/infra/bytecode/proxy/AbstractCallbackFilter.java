/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package infra.bytecode.proxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.util.CollectionUtils;

/**
 * @author TODAY <br>
 * 2018-11-08 15:09
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class AbstractCallbackFilter implements CallbackFilter {

  private final ArrayList<Object> callbacks = new ArrayList<>();
  private final HashMap<Method, Integer> methodMap = new HashMap<>();

  public AbstractCallbackFilter(Class<?> superclass, Class<?>[] interfaces) {

    List<Method> methods = new ArrayList<>();
    Enhancer.getMethods(superclass, interfaces, methods);
    Map<Object, Integer> indexes = new HashMap<>();

    final List<Object> callbacks = this.callbacks;

    for (int i = 0, size = methods.size(); i < size; i++) {
      Method method = methods.get(i);
      Object callback = getCallback(method);
      if (callback == null) {
        throw new IllegalStateException("getCallback cannot return null");
      }
      boolean isCallback = callback instanceof Callback;

      if (!(isCallback || (callback instanceof Class))) {
        throw new IllegalStateException("getCallback must return a Callback or a Class");
      }
      if (i > 0 && ((callbacks.get(i - 1) instanceof Callback) ^ isCallback)) {
        throw new IllegalStateException("getCallback must return a Callback or a Class consistently for every Method");
      }
      Integer index = indexes.get(callback);
      if (index == null) {
        index = callbacks.size(); // box
        indexes.put(callback, index);
      }
      methodMap.put(method, index);
      callbacks.add(callback);
    }
  }

  protected abstract Object getCallback(Method method);

  public Callback[] getCallbacks() {
    final List<Object> callbacks = this.callbacks;
    if (CollectionUtils.isEmpty(callbacks)) {
      return new Callback[0];
    }
    if (callbacks.get(0) instanceof Callback) {
      return callbacks.toArray(new Callback[callbacks.size()]);
    }
    throw new IllegalStateException("getCallback returned classes, not callbacks; call getCallbackTypes instead");
  }

  public int accept(Method method) {
    return methodMap.get(method);
  }

  public int hashCode() {
    return methodMap.hashCode();
  }

  public boolean equals(Object o) {
    return o == this || (o instanceof AbstractCallbackFilter && methodMap.equals(((AbstractCallbackFilter) o).methodMap));
  }
}
