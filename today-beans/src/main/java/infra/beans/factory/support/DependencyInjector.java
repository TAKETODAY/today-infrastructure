/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

import infra.beans.BeanUtils;
import infra.beans.TypeConverter;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.DependencyDescriptor;
import infra.core.MethodParameter;
import infra.util.ExceptionUtils;
import infra.util.ObjectUtils;

import static infra.beans.factory.support.StandardBeanFactory.raiseNoMatchingBeanFound;

/**
 * Dependency injector
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/27 21:06
 */
public class DependencyInjector {

  @Nullable
  private DependencyResolvingStrategies resolvingStrategies;

  @Nullable
  private final AutowireCapableBeanFactory beanFactory;

  public DependencyInjector(@Nullable AutowireCapableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  //---------------------------------------------------------------------
  // Inject to target injection-point
  //---------------------------------------------------------------------

  public <T> T inject(Constructor<T> constructor, @Nullable Object... providedArgs) {
    @Nullable Object[] parameter = resolveArguments(constructor, providedArgs);
    return BeanUtils.newInstance(constructor, parameter);
  }

  public Object inject(Method method, Object bean, @Nullable Object... providedArgs) {
    @Nullable Object[] args = resolveArguments(method, providedArgs);
    try {
      return method.invoke(bean, args);
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException("Could not access method: " + method);
    }
    catch (InvocationTargetException e) {
      throw ExceptionUtils.sneakyThrow(e.getTargetException());
    }
  }

  //---------------------------------------------------------------------
  // Resolving dependency
  //---------------------------------------------------------------------

  @Nullable
  public Object @Nullable [] resolveArguments(Executable executable, @Nullable Object... providedArgs) {
    int parameterLength = executable.getParameterCount();
    if (parameterLength != 0) {
      @Nullable Object[] arguments = new Object[parameterLength];
      Parameter[] parameters = executable.getParameters();
      for (int i = 0; i < arguments.length; i++) {
        Object provided = findProvided(parameters[i], providedArgs);
        if (provided == null) {
          MethodParameter methodParam = MethodParameter.forExecutable(executable, i);
          DependencyDescriptor descriptor = new DependencyDescriptor(methodParam, true);
          Object resolved = resolve(descriptor, null, null, null, beanFactory);
          if (resolved == null) {
            if (beanFactory instanceof StandardBeanFactory sbf) {
              if (sbf.isRequired(descriptor)) {
                sbf.raiseNoMatchingBeanFound(descriptor.getDependencyType(), descriptor);
              }
            }
            else if (descriptor.isRequired()) {
              raiseNoMatchingBeanFound(descriptor);
            }
          }
          arguments[i] = resolved;
        }
        else {
          arguments[i] = provided;
        }
      }
      return arguments;
    }
    return null;
  }

  @Nullable
  public Object resolveValue(DependencyDescriptor descriptor) {
    return resolveValue(descriptor, null, null, null);
  }

  @Nullable
  public Object resolveValue(DependencyDescriptor descriptor, @Nullable String beanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
    return resolve(descriptor, beanName, autowiredBeanNames, typeConverter, beanFactory);
  }

  @Nullable
  Object resolve(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter converter, @Nullable AutowireCapableBeanFactory beanFactory) {
    if (beanFactory != null) {
      return beanFactory.resolveDependency(descriptor, requestingBeanName, autowiredBeanNames, converter);
    }
    else {
      var context = new DependencyResolvingStrategy.Context(requestingBeanName, autowiredBeanNames, converter);
      return getResolvingStrategies().resolveDependency(descriptor, context);
    }
  }

  public DependencyResolvingStrategies getResolvingStrategies() {
    DependencyResolvingStrategies strategies = this.resolvingStrategies;
    if (strategies == null) {
      strategies = new DependencyResolvingStrategies();
      strategies.initStrategies(beanFactory instanceof ConfigurableBeanFactory cbf ? cbf.getBeanClassLoader() : null);
      this.resolvingStrategies = strategies;
    }
    return strategies;
  }

  public void setResolvingStrategies(@Nullable DependencyResolvingStrategies resolvingStrategies) {
    this.resolvingStrategies = resolvingStrategies;
  }

  @Nullable
  @SuppressWarnings("NullAway")
  public static Object findProvided(Parameter parameter, @Nullable Object @Nullable [] providedArgs) {
    if (ObjectUtils.isNotEmpty(providedArgs)) {
      Class<?> dependencyType = parameter.getType();
      for (final Object providedArg : providedArgs) {
        if (dependencyType.isInstance(providedArg)) {
          return providedArg;
        }
      }
    }
    return null;
  }

}
