/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Performs dependency injection for constructors and methods.
 * <p>Resolves method/constructor parameters by matching provided arguments first,
 * then delegating to the bean factory or resolving strategies for remaining dependencies.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/27 21:06
 */
public class DependencyInjector {

  private final @Nullable AutowireCapableBeanFactory beanFactory;

  private @Nullable DependencyResolvingStrategies resolvingStrategies;

  public DependencyInjector(@Nullable AutowireCapableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  //---------------------------------------------------------------------
  // Inject to target injection-point
  //---------------------------------------------------------------------

  /**
   * Resolves dependencies for the given constructor and creates a new instance.
   * <p>Matches provided arguments by type first, then resolves remaining parameters
   * via the bean factory or configured resolving strategies.</p>
   *
   * @param <T> the type of the instance to create
   * @param constructor the constructor to invoke
   * @param providedArgs optional arguments that may match some parameters by type;
   * these take precedence over resolved beans
   * @return a new instance created with resolved dependencies
   */
  public <T> T inject(Constructor<T> constructor, @Nullable Object @Nullable ... providedArgs) {
    @Nullable Object[] parameter = resolveArguments(constructor, providedArgs);
    return BeanUtils.newInstance(constructor, parameter);
  }

  /**
   * Resolves dependencies for the given method and invokes it on the specified bean instance.
   * <p>Matches provided arguments by type first, then resolves remaining parameters
   * via the bean factory or configured resolving strategies.</p>
   *
   * @param method the method to invoke
   * @param bean the target bean instance on which to invoke the method
   * @param providedArgs optional arguments that may match some parameters by type;
   * these take precedence over resolved beans
   * @return the result of the method invocation
   * @throws IllegalStateException if the method is not accessible
   * @throws RuntimeException if the method invocation throws an exception (wrapped as unchecked)
   */
  public Object inject(Method method, Object bean, @Nullable Object @Nullable ... providedArgs) {
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

  /**
   * Resolves arguments for the given executable (constructor or method) by matching
   * provided arguments first, then delegating to the bean factory or resolving strategies
   * for remaining dependencies.
   *
   * @param executable the constructor or method to resolve arguments for
   * @param providedArgs optional arguments that may match some parameters by type;
   * these take precedence over resolved beans
   * @return an array of resolved arguments if the executable has parameters; {@code null} otherwise
   */
  public @Nullable Object @Nullable [] resolveArguments(Executable executable, @Nullable Object @Nullable ... providedArgs) {
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

  /**
   * Resolves a dependency value based on the given descriptor.
   * <p>This is a convenience method that delegates to {@link #resolveValue(DependencyDescriptor, String, Set, TypeConverter)}
   * with default {@code null} values for optional parameters.</p>
   *
   * @param descriptor the dependency descriptor containing metadata about the injection point
   * @return the resolved bean instance or {@code null} if no matching bean is found and the dependency is optional
   */
  public @Nullable Object resolveValue(DependencyDescriptor descriptor) {
    return resolveValue(descriptor, null, null, null);
  }

  /**
   * Resolves a dependency value based on the given descriptor and contextual parameters.
   * <p>Delegates to the underlying {@link AutowireCapableBeanFactory} if available,
   * otherwise uses configured {@link DependencyResolvingStrategies} to resolve the dependency.</p>
   *
   * @param descriptor the dependency descriptor containing metadata about the injection point
   * @param beanName the name of the bean requesting the dependency (may be {@code null})
   * @param autowiredBeanNames a set to collect names of autowired beans (may be {@code null})
   * @param typeConverter the type converter to use for type conversion during resolution (may be {@code null})
   * @return the resolved bean instance or {@code null} if no matching bean is found and the dependency is optional
   */
  public @Nullable Object resolveValue(DependencyDescriptor descriptor, @Nullable String beanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
    return resolve(descriptor, beanName, autowiredBeanNames, typeConverter, beanFactory);
  }

  protected @Nullable Object resolve(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
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

  /**
   * Sets the dependency resolving strategies to use when no bean factory is available.
   *
   * @param resolvingStrategies the strategies for resolving dependencies,
   * or {@code null} to use defaults
   */
  public void setResolvingStrategies(@Nullable DependencyResolvingStrategies resolvingStrategies) {
    this.resolvingStrategies = resolvingStrategies;
  }

  /**
   * Finds a provided argument that matches the type of the given parameter.
   * <p>Iterates through the provided arguments and returns the first one that is an instance
   * of the parameter's type.</p>
   *
   * @param parameter the method or constructor parameter to match against
   * @param providedArgs optional arguments that may match the parameter by type
   * @return the matching provided argument, or {@code null} if no match is found
   */
  public static @Nullable Object findProvided(Parameter parameter, @Nullable Object @Nullable [] providedArgs) {
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
