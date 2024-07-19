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
package cn.taketoday.bytecode.proxy;

import cn.taketoday.bytecode.Type;

/**
 * @author TODAY <br>
 * 2018-11-08 15:09
 */
class CallbackInfo {

  private final Class<?> cls;
  private final Type type;
  private final CallbackGenerator generator;

  private static final CallbackInfo[] CALLBACKS = {
          new CallbackInfo(NoOp.class, NoOpGenerator.INSTANCE),
          new CallbackInfo(MethodInterceptor.class, MethodInterceptorGenerator.INSTANCE),
          new CallbackInfo(LazyLoader.class, LazyLoaderGenerator.INSTANCE),
          new CallbackInfo(Dispatcher.class, DispatcherGenerator.INSTANCE),
          new CallbackInfo(FixedValue.class, FixedValueGenerator.INSTANCE),
          new CallbackInfo(ProxyRefDispatcher.class, DispatcherGenerator.PROXY_REF_INSTANCE)
  };

  private CallbackInfo(Class<?> cls, CallbackGenerator generator) {
    this.cls = cls;
    this.generator = generator;
    this.type = Type.forClass(cls);
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
