/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.retry.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.classify.SubclassClassifier;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.retry.ExhaustedRetryException;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.interceptor.MethodInvocationRecoverer;
import cn.taketoday.retry.support.RetrySynchronizationManager;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * A recoverer for method invocations based on the <code>@Recover</code> annotation. A
 * suitable recovery method is one with a Throwable type as the first parameter and the
 * same return type and arguments as the method that failed. The Throwable first argument
 * is optional and if omitted the method is treated as a default (called when there are no
 * other matches). Generally the best matching method is chosen based on the type of the
 * first parameter and the type of the exception being handled. The closest match in the
 * class hierarchy is chosen, so for instance if an IllegalArgumentException is being
 * handled and there is a method whose first argument is RuntimeException, then it will be
 * preferred over a method whose first argument is Throwable.
 *
 * @param <T> the type of the return value from the recovery
 * @author Dave Syer
 * @author Josh Long
 * @author Aldo Sinanaj
 * @author Randell Callahan
 * @author Nathanaël Roberts
 * @author Maksim Kita
 * @author Gary Russell
 * @since 4.0
 */
public class RecoverAnnotationRecoveryHandler<T> implements MethodInvocationRecoverer<T> {

  private final SubclassClassifier<Throwable, Method> classifier = new SubclassClassifier<>();

  private HashMap<Method, SimpleMetadata> methods = new HashMap<>();

  private final Object target;

  private String recoverMethodName;

  public RecoverAnnotationRecoveryHandler(Object target, Method method) {
    this.target = target;
    var types = new HashMap<Class<? extends Throwable>, Method>();
    final Method failingMethod = method;
    Retryable retryable = AnnotationUtils.findAnnotation(method, Retryable.class);
    if (retryable != null) {
      this.recoverMethodName = retryable.recover();
    }
    ReflectionUtils.doWithMethods(target.getClass(), currentMethod -> {
      Recover recover = AnnotationUtils.findAnnotation(currentMethod, Recover.class);
      if (recover == null) {
        recover = findAnnotationOnTarget(target, currentMethod);
      }
      if (recover != null && failingMethod.getGenericReturnType() instanceof ParameterizedType
              && currentMethod.getGenericReturnType() instanceof ParameterizedType) {
        if (isParameterizedTypeAssignable((ParameterizedType) currentMethod.getGenericReturnType(),
                (ParameterizedType) failingMethod.getGenericReturnType())) {
          putToMethodsMap(currentMethod, types);
        }
      }
      else if (recover != null && currentMethod.getReturnType().isAssignableFrom(failingMethod.getReturnType())) {
        putToMethodsMap(currentMethod, types);
      }
    });
    this.classifier.setTypeMap(types);
    optionallyFilterMethodsBy(failingMethod.getReturnType());
  }

  @Override
  @SuppressWarnings("deprecation")
  public T recover(Object[] args, Throwable cause) {
    Method method = findClosestMatch(args, cause.getClass());
    if (method == null) {
      throw new ExhaustedRetryException("Cannot locate recovery method", cause);
    }
    SimpleMetadata meta = this.methods.get(method);
    Object[] argsToUse = meta.getArgs(cause, args);
    boolean methodAccessible = method.isAccessible();
    try {
      ReflectionUtils.makeAccessible(method);
      RetryContext context = RetrySynchronizationManager.getContext();
      Object proxy = null;
      if (context != null) {
        proxy = context.getAttribute("___proxy___");
        if (proxy != null) {
          Method proxyMethod = findMethodOnProxy(method, proxy);
          if (proxyMethod == null) {
            proxy = null;
          }
          else {
            method = proxyMethod;
          }
        }
      }
      if (proxy == null) {
        proxy = this.target;
      }
      @SuppressWarnings("unchecked")
      T result = (T) ReflectionUtils.invokeMethod(method, proxy, argsToUse);
      return result;
    }
    finally {
      if (methodAccessible != method.isAccessible()) {
        method.setAccessible(methodAccessible);
      }
    }
  }

  private Method findMethodOnProxy(Method method, Object proxy) {
    try {
      return proxy.getClass().getMethod(method.getName(), method.getParameterTypes());
    }
    catch (NoSuchMethodException | SecurityException e) {
      return null;
    }
  }

  private Method findClosestMatch(Object[] args, Class<? extends Throwable> cause) {
    Method result = null;

    if (StringUtils.isEmpty(this.recoverMethodName)) {
      int min = Integer.MAX_VALUE;
      for (Map.Entry<Method, SimpleMetadata> entry : this.methods.entrySet()) {
        Method method = entry.getKey();
        SimpleMetadata meta = entry.getValue();
        Class<? extends Throwable> type = meta.getType();
        if (type == null) {
          type = Throwable.class;
        }
        if (type.isAssignableFrom(cause)) {
          int distance = calculateDistance(cause, type);
          if (distance < min) {
            min = distance;
            result = method;
          }
          else if (distance == min) {
            boolean parametersMatch = compareParameters(args, meta.getArgCount(),
                    method.getParameterTypes());
            if (parametersMatch) {
              result = method;
            }
          }
        }
      }
    }
    else {
      for (Map.Entry<Method, SimpleMetadata> entry : this.methods.entrySet()) {
        Method method = entry.getKey();
        if (method.getName().equals(this.recoverMethodName)) {
          SimpleMetadata meta = entry.getValue();
          if (meta.type.isAssignableFrom(cause)
                  && compareParameters(args, meta.getArgCount(), method.getParameterTypes())) {
            result = method;
            break;
          }
        }
      }
    }
    return result;
  }

  private int calculateDistance(Class<? extends Throwable> cause, Class<? extends Throwable> type) {
    int result = 0;
    Class<?> current = cause;
    while (current != type && current != Throwable.class) {
      result++;
      current = current.getSuperclass();
    }
    return result;
  }

  private boolean compareParameters(Object[] args, int argCount, Class<?>[] parameterTypes) {
    if (argCount == (args.length + 1)) {
      int startingIndex = 0;
      if (parameterTypes.length > 0 && Throwable.class.isAssignableFrom(parameterTypes[0])) {
        startingIndex = 1;
      }
      for (int i = startingIndex; i < parameterTypes.length; i++) {
        final Object argument = i - startingIndex < args.length ? args[i - startingIndex] : null;
        if (argument == null) {
          continue;
        }
        if (!parameterTypes[i].isAssignableFrom(argument.getClass())) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Returns {@code true} if the input methodReturnType is a direct match of the
   * failingMethodReturnType. Takes nested generics into consideration as well, while
   * deciding a match.
   *
   * @return true if the parameterized return types match.
   */
  private boolean isParameterizedTypeAssignable(
          ParameterizedType methodReturnType, ParameterizedType failingMethodReturnType) {

    Type[] methodActualArgs = methodReturnType.getActualTypeArguments();
    Type[] failingMethodActualArgs = failingMethodReturnType.getActualTypeArguments();
    if (methodActualArgs.length != failingMethodActualArgs.length) {
      return false;
    }
    int startingIndex = 0;
    for (int i = startingIndex; i < methodActualArgs.length; i++) {
      Type methodArgType = methodActualArgs[i];
      Type failingMethodArgType = failingMethodActualArgs[i];
      if (methodArgType instanceof ParameterizedType
              && failingMethodArgType instanceof ParameterizedType) {
        return isParameterizedTypeAssignable((ParameterizedType) methodArgType,
                (ParameterizedType) failingMethodArgType);
      }
      if (methodArgType instanceof Class && failingMethodArgType instanceof Class
              && !failingMethodArgType.equals(methodArgType)) {
        return false;
      }
    }
    return true;
  }

  private void putToMethodsMap(Method method, Map<Class<? extends Throwable>, Method> types) {
    Class<?>[] parameterTypes = method.getParameterTypes();
    if (parameterTypes.length > 0 && Throwable.class.isAssignableFrom(parameterTypes[0])) {
      @SuppressWarnings("unchecked")
      Class<? extends Throwable> type = (Class<? extends Throwable>) parameterTypes[0];
      types.put(type, method);
      methods.put(method, new SimpleMetadata(parameterTypes.length, type));
    }
    else {
      classifier.setDefaultValue(method);
      methods.put(method, new SimpleMetadata(parameterTypes.length, null));
    }
  }

  private Recover findAnnotationOnTarget(Object target, Method method) {
    try {
      Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
      return AnnotationUtils.findAnnotation(targetMethod, Recover.class);
    }
    catch (Exception e) {
      return null;
    }
  }

  private void optionallyFilterMethodsBy(Class<?> returnClass) {
    HashMap<Method, SimpleMetadata> filteredMethods = new HashMap<>();
    for (Method method : this.methods.keySet()) {
      if (method.getReturnType() == returnClass) {
        filteredMethods.put(method, this.methods.get(method));
      }
    }
    if (filteredMethods.size() > 0) {
      this.methods = filteredMethods;
    }
  }

  private record SimpleMetadata(int argCount, Class<? extends Throwable> type) {

    public int getArgCount() {
      return this.argCount;
    }

    public Class<? extends Throwable> getType() {
      return this.type;
    }

    public Object[] getArgs(Throwable t, Object[] args) {
      Object[] result = new Object[getArgCount()];
      int startArgs = 0;
      if (this.type != null) {
        result[0] = t;
        startArgs = 1;
      }
      int length = Math.min(result.length - startArgs, args.length);
      if (length == 0) {
        return result;
      }
      System.arraycopy(args, 0, result, startArgs, length);
      return result;
    }

  }

}
