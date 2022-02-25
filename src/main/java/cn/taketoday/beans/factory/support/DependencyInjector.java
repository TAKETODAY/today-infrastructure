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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/27 21:06
 */
public class DependencyInjector {

  @Nullable
  private DependencyResolvingStrategies resolvingStrategies;

  private final BeanFactory beanFactory;

  public DependencyInjector(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "beanFactory is required");
    this.beanFactory = beanFactory;
  }

  //---------------------------------------------------------------------
  // DependencyResolvingStrategies supports
  //---------------------------------------------------------------------

  public boolean canInject(Field field) {
    return getResolvingStrategies().supports(field);
  }

  public boolean canInject(Executable method) {
    return getResolvingStrategies().supports(method);
  }

  //---------------------------------------------------------------------
  // Inject to target injection-point
  //---------------------------------------------------------------------

  public <T> T inject(Constructor<T> constructor, @Nullable Object... providedArgs) {
    Object[] parameter = resolveArguments(constructor, providedArgs);
    return BeanUtils.newInstance(constructor, parameter);
  }

  public Object inject(Method method, Object bean, @Nullable Object... providedArgs) {
    Object[] args = resolveArguments(method, providedArgs);
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
  public Object resolveValue(DependencyDescriptor descriptor) {
    DependencyResolvingContext context = new DependencyResolvingContext(null, beanFactory);
    return resolveValue(descriptor, context);
  }

  @Nullable
  public Object resolveValue(
          DependencyDescriptor descriptor, DependencyResolvingContext context) {
    Object resolved = resolve(descriptor, context);
    return resolved == InjectionPoint.DO_NOT_SET ? null : resolved;
  }

  @Nullable
  public Object resolveValue(DependencyDescriptor descriptor, @Nullable String beanName) {
    return resolveValue(descriptor, beanName, null);
  }

  @Nullable
  public Object resolveValue(
          DependencyDescriptor descriptor,
          @Nullable String beanName, @Nullable Set<String> autowiredBeanNames) {
    DependencyResolvingContext context = new DependencyResolvingContext(null, beanFactory, beanName);
    context.setDependentBeans(autowiredBeanNames);
    return resolveValue(descriptor, context);
  }

  @Nullable
  public Object[] resolveArguments(Executable executable, @Nullable Object... providedArgs) {
    return resolveArguments(executable, null, providedArgs);
  }

  @Nullable
  public Object[] resolveArguments(
          Executable executable, @Nullable String beanName, @Nullable Object... providedArgs) {
    int parameterLength = executable.getParameterCount();
    if (parameterLength != 0) {
      Object[] arguments = new Object[parameterLength];
      DependencyResolvingContext context = null;
      Parameter[] parameters = executable.getParameters();
      for (int i = 0; i < arguments.length; i++) {
        Object provided = findProvided(parameters[i], providedArgs);
        if (provided == null) {
          MethodParameter methodParam = MethodParameter.forExecutable(executable, i);
          DependencyDescriptor currDesc = new DependencyDescriptor(methodParam, true);
          if (context == null) {
            context = new DependencyResolvingContext(executable, beanFactory, beanName);
            if (beanName != null) {
              context.setDependentBeans(new LinkedHashSet<>());
            }
          }
          arguments[i] = resolveValue(currDesc, context);
        }
        else {
          arguments[i] = provided;
        }
      }
      if (context != null) {
        registerDependentBeans(beanName, context.getDependentBeans());
      }
      return arguments;
    }
    return null;
  }

  /**
   * @throws UnsatisfiedDependencyException No strategy supports this dependency
   * @see InjectionPoint#DO_NOT_SET
   */
  @Nullable
  public Object resolve(
          DependencyDescriptor descriptor, DependencyResolvingContext context) {
    getResolvingStrategies().resolveDependency(descriptor, context);
    if (context.isDependencyResolved()) {
      return context.getDependency();
    }
    else {
      throw new UnsatisfiedDependencyException(null, context.getBeanName(), descriptor,
              "Because: 'No strategy supports this dependency: " + descriptor + "'");
    }
  }

  /**
   * Register the specified bean as dependent on the autowired beans.
   */
  private void registerDependentBeans(@Nullable String beanName, @Nullable Set<String> autowiredBeanNames) {
    if (beanName != null && autowiredBeanNames != null
            && beanFactory instanceof ConfigurableBeanFactory configurable) {
      for (String autowiredBeanName : autowiredBeanNames) {
        if (beanFactory.containsBean(autowiredBeanName)) {
          configurable.registerDependentBean(autowiredBeanName, beanName);
        }
      }
    }
  }

  public DependencyResolvingStrategies getResolvingStrategies() {
    if (resolvingStrategies == null) {
      resolvingStrategies = new DependencyResolvingStrategies();
      initStrategies(resolvingStrategies);
    }
    return resolvingStrategies;
  }

  private void initStrategies(DependencyResolvingStrategies resolvingStrategies) {
    resolvingStrategies.initStrategies(beanFactory);
  }

  public void setResolvingStrategies(@Nullable DependencyResolvingStrategies resolvingStrategies) {
    this.resolvingStrategies = resolvingStrategies;
  }

  @Nullable
  public static Object findProvided(Parameter parameter, @Nullable Object[] providedArgs) {
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
