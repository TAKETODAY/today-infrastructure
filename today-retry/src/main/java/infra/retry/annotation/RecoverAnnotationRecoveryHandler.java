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

package infra.retry.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import infra.classify.SubclassClassifier;
import infra.core.annotation.AnnotatedElementUtils;
import infra.lang.Nullable;
import infra.retry.ExhaustedRetryException;
import infra.retry.RetryContext;
import infra.retry.interceptor.MethodInvocationRecoverer;
import infra.retry.support.RetrySynchronizationManager;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

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
 * @author Artem Bilan
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RecoverAnnotationRecoveryHandler<T> implements MethodInvocationRecoverer<T> {

  private final SubclassClassifier<Throwable, Method> classifier = new SubclassClassifier<>();

  private final HashMap<Method, RecoverMetadata> recoverMethods = new HashMap<>();

  private final Object target;

  /**
   * Retryable method
   *
   * @since 5.0
   */
  private final Method retryable;

  @Nullable
  private String recoverMethodName;

  public RecoverAnnotationRecoveryHandler(Object target, Method method) {
    this.target = target;
    this.retryable = method;
    var types = new HashMap<Class<? extends Throwable>, Method>();
    final Method failingMethod = method;
    Retryable retryable = AnnotatedElementUtils.findMergedAnnotation(method, Retryable.class);
    if (retryable != null) {
      String recoverMethodName = retryable.recover();
      if (StringUtils.hasText(recoverMethodName)) {
        this.recoverMethodName = recoverMethodName;
      }
    }
    ReflectionUtils.doWithMethods(target.getClass(), candidate -> {
      Recover recover = AnnotatedElementUtils.findMergedAnnotation(candidate, Recover.class);
      if (recover == null) {
        recover = findAnnotationOnTarget(target, candidate);
      }

      if (recover != null) {
        if (failingMethod.getGenericReturnType() instanceof ParameterizedType failingPtype
                && candidate.getGenericReturnType() instanceof ParameterizedType parameterizedType) {
          if (isParameterizedTypeAssignable(parameterizedType, failingPtype)) {
            putToMethodsMap(candidate, types);
          }
        }
        else if (candidate.getReturnType().isAssignableFrom(failingMethod.getReturnType())) {
          putToMethodsMap(candidate, types);
        }
      }
    });
    this.classifier.setTypeMap(types);
    optionallyFilterMethodsBy(failingMethod.getReturnType());
  }

  @Override
  @SuppressWarnings("unchecked")
  public T recover(Object[] args, Throwable cause) {
    Method method = findClosestMatch(args, cause.getClass());
    if (method == null) {
      throw new ExhaustedRetryException("Cannot locate recovery method", cause);
    }
    RecoverMetadata meta = this.recoverMethods.get(method);
    Object[] argsToUse = meta.getArgs(cause, args);
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
    return (T) ReflectionUtils.invokeMethod(method, proxy, argsToUse);
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
    if (recoverMethodName == null) {
      int min = Integer.MAX_VALUE;
      for (Map.Entry<Method, RecoverMetadata> entry : recoverMethods.entrySet()) {
        Method current = entry.getKey();
        RecoverMetadata meta = entry.getValue();
        if (meta.isAssignable(cause)) {
          int distance = calculateDistance(cause, meta.throwableType);
          if (distance < min) {
            min = distance;
            result = current;
          }
          else if (distance == min) {
            // parametersMatch
            if (compareParameters(args, meta.argCount, current.getParameterTypes(), false)) {
              result = current;
            }
          }
        }
      }
    }
    else {
      for (Map.Entry<Method, RecoverMetadata> entry : this.recoverMethods.entrySet()) {
        Method method = entry.getKey();
        if (method.getName().equals(this.recoverMethodName)) {
          RecoverMetadata meta = entry.getValue();
          if (meta.isAssignable(cause) && compareParameters(
                  args, meta.argCount, method.getParameterTypes(), true)) {
            result = method;
            break;
          }
        }
      }
    }
    return result;
  }

  private int calculateDistance(Class<? extends Throwable> cause, @Nullable Class<? extends Throwable> type) {
    int result = 0;
    Class<?> current = cause;
    while (current != type && current != Throwable.class) {
      result++;
      current = current.getSuperclass();
    }
    return result;
  }

  private boolean compareParameters(Object[] args, int argCount, Class<?>[] inputTypes, boolean withRecoverMethodName) {
    if ((withRecoverMethodName && argCount == args.length) || argCount == (args.length + 1)) {
      int startingIndex = 0;
      if (inputTypes.length > 0 && Throwable.class.isAssignableFrom(inputTypes[0])) {
        startingIndex = 1;
      }
      for (int i = startingIndex; i < inputTypes.length; i++) {
        if (i - startingIndex < args.length) {
          final Object argument = args[i - startingIndex];
          if (argument == null) {
            // null args type match
            Class<?> targetType = retryable.getParameterTypes()[i - startingIndex];
            if (!inputTypes[i].isAssignableFrom(targetType)) {
              return false;
            }
            continue;
          }

          Class<?> parameterType = ClassUtils.resolvePrimitiveIfNecessary(inputTypes[i]);
          if (!parameterType.isAssignableFrom(argument.getClass())) {
            return false;
          }
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
   * @param methodReturnType the method return type
   * @param failingMethodReturnType the failing method return type
   * @return true if the parameterized return types match.
   */
  private static boolean isParameterizedTypeAssignable(
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
      if (methodArgType instanceof ParameterizedType pType
              && failingMethodArgType instanceof ParameterizedType fPtype) {
        if (!isParameterizedTypeAssignable(pType, fPtype)) {
          return false;
        }
      }
      else if (methodArgType instanceof Class && failingMethodArgType instanceof Class) {
        if (!failingMethodArgType.equals(methodArgType)) {
          return false;
        }
      }
      else if (!methodArgType.equals(failingMethodArgType)) {
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
      recoverMethods.put(method, new RecoverMetadata(parameterTypes.length, type));
    }
    else {
      classifier.setDefaultValue(method);
      recoverMethods.put(method, new RecoverMetadata(parameterTypes.length, null));
    }
  }

  private Recover findAnnotationOnTarget(Object target, Method method) {
    try {
      Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
      return AnnotatedElementUtils.findMergedAnnotation(targetMethod, Recover.class);
    }
    catch (Exception e) {
      return null;
    }
  }

  private void optionallyFilterMethodsBy(Class<?> returnClass) {
    var filteredMethods = new HashMap<Method, RecoverMetadata>();
    for (Map.Entry<Method, RecoverMetadata> entry : recoverMethods.entrySet()) {
      Method key = entry.getKey();
      if (key.getReturnType() == returnClass) {
        filteredMethods.put(key, entry.getValue());
      }
    }

    if (!filteredMethods.isEmpty()) {
      this.recoverMethods.clear();
      this.recoverMethods.putAll(filteredMethods);
    }
  }

  private static class RecoverMetadata {

    public final int argCount;

    @Nullable
    public final Class<? extends Throwable> throwableType;

    public RecoverMetadata(int argCount, @Nullable Class<? extends Throwable> throwableType) {
      this.argCount = argCount;
      this.throwableType = throwableType;
    }

    public Object[] getArgs(Throwable t, Object[] args) {
      Object[] result = new Object[argCount];
      int startArgs = 0;
      if (this.throwableType != null) {
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

    public boolean isAssignable(Class<? extends Throwable> cls) {
      return throwableType == null || throwableType.isAssignableFrom(cls);
    }

  }

}
