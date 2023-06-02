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

package cn.taketoday.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Simple factory used to instantiate objects by injecting available parameters.
 *
 * @param <T> the type to instantiate
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/18 10:53
 */
public class Instantiator<T> {

  private static final Comparator<Constructor<?>> CONSTRUCTOR_COMPARATOR =
          Comparator.<Constructor<?>>comparingInt(Constructor::getParameterCount).reversed();

  private static final FailureHandler throwingFailureHandler = (type, implementationName, failure) -> {
    throw new IllegalArgumentException(
            "Unable to instantiate " + implementationName + " [" + type.getName() + "]", failure);
  };

  private final Class<?> type;

  private final Map<Class<?>, Function<Class<?>, Object>> availableParameters;

  private final FailureHandler failureHandler;

  /**
   * Create a new {@link Instantiator} instance for the given type.
   *
   * @param type the type to instantiate
   * @param availableParameters consumer used to register available parameters
   */
  public Instantiator(Class<?> type, Consumer<AvailableParameters> availableParameters) {
    this(type, availableParameters, throwingFailureHandler);
  }

  /**
   * Create a new {@link Instantiator} instance for the given type.
   *
   * @param type the type to instantiate
   * @param availableParameters consumer used to register available parameters
   * @param failureHandler a {@link FailureHandler} that will be called in case of
   * failure when instantiating objects
   */
  public Instantiator(Class<?> type, Consumer<AvailableParameters> availableParameters,
          FailureHandler failureHandler) {
    this.type = type;
    this.failureHandler = failureHandler;
    this.availableParameters = getAvailableParameters(availableParameters);
  }

  private Map<Class<?>, Function<Class<?>, Object>> getAvailableParameters(
          Consumer<AvailableParameters> availableParameters) {
    var result = new LinkedHashMap<Class<?>, Function<Class<?>, Object>>();
    availableParameters.accept(new AvailableParameters() {

      @Override
      public void add(Class<?> type, Object instance) {
        result.put(type, (factoryType) -> instance);
      }

      @Override
      public void add(Class<?> type, Function<Class<?>, Object> factory) {
        result.put(type, factory);
      }

    });
    return Collections.unmodifiableMap(result);
  }

  /**
   * Instantiate the given set of class name, injecting constructor arguments as
   * necessary.
   *
   * @param names the class names to instantiate
   * @return a list of instantiated instances, can be modified
   */
  public List<T> instantiate(Collection<String> names) {
    return instantiate(null, names);
  }

  /**
   * Instantiate the given set of class name, injecting constructor arguments as
   * necessary.
   *
   * @param classLoader the source classloader
   * @param names the class names to instantiate
   * @return a list of instantiated instances, can be modified
   */
  public List<T> instantiate(@Nullable ClassLoader classLoader, Collection<String> names) {
    Assert.notNull(names, "Names must not be null");
    return instantiate(names.stream().map((name) -> TypeSupplier.forName(classLoader, name)));
  }

  /**
   * Instantiate the given set of classes, injecting constructor arguments as necessary.
   *
   * @param types the types to instantiate
   * @return a list of instantiated instances, can be modified
   */
  public List<T> instantiateTypes(Collection<Class<?>> types) {
    Assert.notNull(types, "Types must not be null");
    return instantiate(types.stream().map(TypeSupplier::forType));
  }

  private List<T> instantiate(Stream<TypeSupplier> typeSuppliers) {
    ArrayList<T> instances = new ArrayList<>();
    typeSuppliers.map(this::instantiate)
            .filter(Objects::nonNull)
            .forEach(instances::add);
    AnnotationAwareOrderComparator.sort(instances);
    return instances;
  }

  private T instantiate(TypeSupplier typeSupplier) {
    try {
      Class<?> type = typeSupplier.get();
      Assert.isAssignable(this.type, type);
      return instantiate(type);
    }
    catch (Throwable ex) {
      this.failureHandler.handleFailure(this.type, typeSupplier.getName(), ex);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private T instantiate(Class<?> type) throws Exception {
    Constructor<?>[] constructors = type.getDeclaredConstructors();
    Arrays.sort(constructors, CONSTRUCTOR_COMPARATOR);
    for (Constructor<?> constructor : constructors) {
      Object[] args = getArgs(constructor.getParameterTypes());
      if (args != null) {
        ReflectionUtils.makeAccessible(constructor);
        return (T) constructor.newInstance(args);
      }
    }
    throw new IllegalAccessException("Class [" + type.getName() + "] has no suitable constructor");
  }

  private Object[] getArgs(Class<?>[] parameterTypes) {
    Object[] args = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      Function<Class<?>, Object> parameter = getAvailableParameter(parameterTypes[i]);
      if (parameter == null) {
        return null;
      }
      args[i] = parameter.apply(this.type);
    }
    return args;
  }

  private Function<Class<?>, Object> getAvailableParameter(Class<?> parameterType) {
    for (Map.Entry<Class<?>, Function<Class<?>, Object>> entry : this.availableParameters.entrySet()) {
      if (entry.getKey().isAssignableFrom(parameterType)) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * Callback used to register available parameters.
   */
  public interface AvailableParameters {

    /**
     * Add a parameter with an instance value.
     *
     * @param type the parameter type
     * @param instance the instance that should be injected
     */
    void add(Class<?> type, Object instance);

    /**
     * Add a parameter with an instance factory.
     *
     * @param type the parameter type
     * @param factory the factory used to create the instance that should be injected
     */
    void add(Class<?> type, Function<Class<?>, Object> factory);

  }

  /**
   * {@link Supplier} that provides a class type.
   */
  private abstract static class TypeSupplier {

    abstract String getName();

    abstract Class<?> get() throws ClassNotFoundException;

    static TypeSupplier forName(@Nullable ClassLoader classLoader, String name) {
      return new TypeSupplier() {

        @Override
        public String getName() {
          return name;
        }

        @Override
        public Class<?> get() throws ClassNotFoundException {
          return ClassUtils.forName(name, classLoader);
        }

      };
    }

    static TypeSupplier forType(Class<?> type) {
      return new TypeSupplier() {

        @Override
        public String getName() {
          return type.getName();
        }

        @Override
        public Class<?> get() {
          return type;
        }

      };
    }

  }

  /**
   * Strategy for handling a failure that occurs when instantiating a type.
   */
  public interface FailureHandler {

    /**
     * Handle the {@code failure} that occurred when instantiating the {@code type}
     * that was expected to be of the given {@code typeSupplier}.
     *
     * @param type the type
     * @param implementationName the name of the implementation type
     * @param failure the failure that occurred
     */
    void handleFailure(Class<?> type, String implementationName, Throwable failure);

  }

}
