/*
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.core.bytecode.proxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.bytecode.core.CglibReflectUtils;
import cn.taketoday.core.Constant;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY <br>
 * 2018-11-08 15:09
 */
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

  public Class<?>[] getCallbackTypes() {
    final List callbacks = this.callbacks;
    if (CollectionUtils.isEmpty(callbacks)) {
      return Constant.EMPTY_CLASS_ARRAY;
    }
    if (callbacks.get(0) instanceof Callback) {
      return CglibReflectUtils.getClasses(getCallbacks());
    }
    return ClassUtils.toClassArray(callbacks);
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
