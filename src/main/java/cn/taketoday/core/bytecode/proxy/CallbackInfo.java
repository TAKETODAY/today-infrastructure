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

import cn.taketoday.core.bytecode.Type;

/**
 * @author TODAY <br>
 * 2018-11-08 15:09
 */
class CallbackInfo {

  private final Class<?> cls;
  private final Type type;
  private final CallbackGenerator generator;

  private static final CallbackInfo[] CALLBACKS = { //
          new CallbackInfo(NoOp.class, NoOpGenerator.INSTANCE), //
          new CallbackInfo(MethodInterceptor.class, MethodInterceptorGenerator.INSTANCE), //
          new CallbackInfo(InvocationHandler.class, InvocationHandlerGenerator.INSTANCE), //
          new CallbackInfo(LazyLoader.class, LazyLoaderGenerator.INSTANCE), //
          new CallbackInfo(Dispatcher.class, DispatcherGenerator.INSTANCE), //
          new CallbackInfo(FixedValue.class, FixedValueGenerator.INSTANCE), //
          new CallbackInfo(ProxyRefDispatcher.class, DispatcherGenerator.PROXY_REF_INSTANCE)//
  };

  private CallbackInfo(Class<?> cls, CallbackGenerator generator) {
    this.cls = cls;
    this.generator = generator;
    this.type = Type.fromClass(cls);
  }

  private static Type determineType(Callback callback, boolean checkAll) {
    if (callback == null) {
      throw new IllegalStateException("Callback is null");
    }
    return determineType(callback.getClass(), checkAll);
  }

  private static Type determineType(Class<?> callbackType, boolean checkAll) {
    Class<?> cur = null;
    Type type = null;

    for (final CallbackInfo info : CALLBACKS) {

      if (info.cls.isAssignableFrom(callbackType)) {
        if (cur != null) {
          throw new IllegalStateException("Callback implements both " + cur + " and " + info.cls);
        }
        cur = info.cls;
        type = info.type;
        if (!checkAll) {
          break;
        }
      }
    }
    if (cur == null) {
      throw new IllegalStateException("Unknown callback type " + callbackType);
    }
    return type;
  }

  private static CallbackGenerator getGenerator(final Type callbackType) {

    for (final CallbackInfo info : CALLBACKS) {
      if (info.type.equals(callbackType)) {
        return info.generator;
      }
    }
    throw new IllegalStateException("Unknown callback type " + callbackType);
  }

  public static Type[] determineTypes(final Class<?>[] callbackTypes) {
    return determineTypes(callbackTypes, true);
  }

  public static Type[] determineTypes(final Class<?>[] callbackTypes, final boolean checkAll) {
    final Type[] types = new Type[callbackTypes.length];

    int i = 0;
    for (final Class<?> type : callbackTypes) {
      types[i++] = determineType(type, checkAll);
    }
    return types;
  }

  public static Type[] determineTypes(final Callback[] callbacks) {
    return determineTypes(callbacks, true);
  }

  public static Type[] determineTypes(final Callback[] callbacks, final boolean checkAll) {
    final Type[] types = new Type[callbacks.length];
    int i = 0;
    for (final Callback callback : callbacks) {
      types[i++] = determineType(callback, checkAll);
    }
    return types;
  }

  public static CallbackGenerator[] getGenerators(final Type[] callbackTypes) {
    final CallbackGenerator[] generators = new CallbackGenerator[callbackTypes.length];
    int i = 0;
    for (final Type callbackType : callbackTypes) {
      generators[i++] = getGenerator(callbackType);
    }
    return generators;
  }

}
