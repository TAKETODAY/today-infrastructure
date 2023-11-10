/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.context.properties.NestedConfigurationProperty;
import cn.taketoday.context.properties.bind.JavaBeanBinder.BeanProperty;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} that can be used to register {@link ReflectionHints} for
 * {@link Bindable} types, discovering any nested type it may expose through a property.
 * <p>
 * This class can be used as a base-class, or instantiated using the {@code forTypes} and
 * {@code forBindables} factory methods.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Sebastien Deleuze
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BindableRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

  private final Bindable<?>[] bindables;

  /**
   * Create a new {@link BindableRuntimeHintsRegistrar} for the specified types.
   *
   * @param types the types to process
   */
  protected BindableRuntimeHintsRegistrar(Class<?>... types) {
    this(Stream.of(types).map(Bindable::of).toArray(Bindable[]::new));
  }

  /**
   * Create a new {@link BindableRuntimeHintsRegistrar} for the specified bindables.
   *
   * @param bindables the bindables to process
   */
  protected BindableRuntimeHintsRegistrar(Bindable<?>... bindables) {
    this.bindables = bindables;
  }

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    registerHints(hints);
  }

  /**
   * Contribute hints to the given {@link RuntimeHints} instance.
   *
   * @param hints the hints contributed so far for the deployment unit
   */
  public void registerHints(RuntimeHints hints) {
    Set<Class<?>> compiledWithoutParameters = new HashSet<>();
    for (Bindable<?> bindable : this.bindables) {
      new Processor(bindable, compiledWithoutParameters).process(hints.reflection());
    }
    if (!compiledWithoutParameters.isEmpty()) {
      throw new MissingParametersCompilerArgumentException(compiledWithoutParameters);
    }
  }

  /**
   * Create a new {@link BindableRuntimeHintsRegistrar} for the specified types.
   *
   * @param types the types to process
   * @return a new {@link BindableRuntimeHintsRegistrar} instance
   */
  public static BindableRuntimeHintsRegistrar forTypes(Iterable<Class<?>> types) {
    Assert.notNull(types, "Types is required");
    return forTypes(StreamSupport.stream(types.spliterator(), false).toArray(Class<?>[]::new));
  }

  /**
   * Create a new {@link BindableRuntimeHintsRegistrar} for the specified types.
   *
   * @param types the types to process
   * @return a new {@link BindableRuntimeHintsRegistrar} instance
   */
  public static BindableRuntimeHintsRegistrar forTypes(Class<?>... types) {
    return new BindableRuntimeHintsRegistrar(types);
  }

  /**
   * Create a new {@link BindableRuntimeHintsRegistrar} for the specified bindables.
   *
   * @param bindables the bindables to process
   * @return a new {@link BindableRuntimeHintsRegistrar} instance
   */
  public static BindableRuntimeHintsRegistrar forBindables(Iterable<Bindable<?>> bindables) {
    Assert.notNull(bindables, "Bindables is required");
    return forBindables(StreamSupport.stream(bindables.spliterator(), false).toArray(Bindable[]::new));
  }

  /**
   * Create a new {@link BindableRuntimeHintsRegistrar} for the specified bindables.
   *
   * @param bindables the bindables to process
   * @return a new {@link BindableRuntimeHintsRegistrar} instance
   */
  public static BindableRuntimeHintsRegistrar forBindables(Bindable<?>... bindables) {
    return new BindableRuntimeHintsRegistrar(bindables);
  }

  /**
   * Processor used to register the hints.
   */
  private static final class Processor {

    private final Class<?> type;

    @Nullable
    private final Constructor<?> bindConstructor;

    @Nullable
    private final JavaBeanBinder.BeanProperties bean;

    private final Set<Class<?>> seen;

    private final Set<Class<?>> compiledWithoutParameters;

    Processor(Bindable<?> bindable, Set<Class<?>> compiledWithoutParameters) {
      this(bindable, false, new HashSet<>(), compiledWithoutParameters);
    }

    private Processor(Bindable<?> bindable, boolean nestedType, Set<Class<?>> seen,
            Set<Class<?>> compiledWithoutParameters) {
      this.type = bindable.getType().getRawClass();
      this.bindConstructor = (bindable.getBindMethod() != BindMethod.JAVA_BEAN)
                             ? BindConstructorProvider.DEFAULT.getBindConstructor(bindable.getType().resolve(), nestedType)
                             : null;
      this.bean = JavaBeanBinder.BeanProperties.of(bindable);
      this.seen = seen;
      this.compiledWithoutParameters = compiledWithoutParameters;
    }

    void process(ReflectionHints hints) {
      if (this.seen.contains(this.type)) {
        return;
      }
      this.seen.add(this.type);
      handleConstructor(hints);
      if (this.bindConstructor != null) {
        handleValueObjectProperties(bindConstructor, hints);
      }
      else if (this.bean != null && !this.bean.getProperties().isEmpty()) {
        handleJavaBeanProperties(bean, hints);
      }
    }

    private void handleConstructor(ReflectionHints hints) {
      if (this.bindConstructor != null) {
        verifyParameterNamesAreAvailable(bindConstructor);
        hints.registerConstructor(bindConstructor, ExecutableMode.INVOKE);
        return;
      }
      Arrays.stream(this.type.getDeclaredConstructors())
              .filter(this::hasNoParameters)
              .findFirst()
              .ifPresent((constructor) -> hints.registerConstructor(constructor, ExecutableMode.INVOKE));
    }

    private void verifyParameterNamesAreAvailable(Constructor<?> bindConstructor) {
      String[] parameterNames = ParameterNameDiscoverer.findParameterNames(bindConstructor);
      if (parameterNames == null) {
        this.compiledWithoutParameters.add(bindConstructor.getDeclaringClass());
      }
    }

    private boolean hasNoParameters(Constructor<?> candidate) {
      return candidate.getParameterCount() == 0;
    }

    private void handleValueObjectProperties(Constructor<?> bindConstructor, ReflectionHints hints) {
      int i = 0;
      for (Parameter parameter : bindConstructor.getParameters()) {
        String propertyName = parameter.getName();
        ResolvableType propertyType = ResolvableType.forConstructorParameter(bindConstructor, i++);
        handleProperty(hints, propertyName, propertyType);
      }
    }

    private void handleJavaBeanProperties(JavaBeanBinder.BeanProperties bean, ReflectionHints hints) {
      Map<String, BeanProperty> properties = bean.getProperties();
      properties.forEach((name, property) -> {
        Method getter = property.getter;
        if (getter != null) {
          hints.registerMethod(getter, ExecutableMode.INVOKE);
        }
        Method setter = property.setter;
        if (setter != null) {
          hints.registerMethod(setter, ExecutableMode.INVOKE);
        }
        handleProperty(hints, name, property.getType());
      });
    }

    private void handleProperty(ReflectionHints hints, String propertyName, ResolvableType propertyType) {
      Class<?> propertyClass = propertyType.resolve();
      if (propertyClass == null) {
        return;
      }
      if (propertyClass.equals(this.type)) {
        return; // Prevent infinite recursion
      }
      Class<?> componentType = getComponentClass(propertyType);
      if (componentType != null) {
        // Can be a list of simple types
        if (!isJavaType(componentType)) {
          processNested(componentType, hints);
        }
      }
      else if (isNestedType(propertyName, propertyClass)) {
        processNested(propertyClass, hints);
      }
    }

    private void processNested(Class<?> type, ReflectionHints hints) {
      new Processor(Bindable.of(type), true, this.seen, this.compiledWithoutParameters).process(hints);
    }

    @Nullable
    private Class<?> getComponentClass(ResolvableType type) {
      ResolvableType componentType = getComponentType(type);
      if (componentType == null) {
        return null;
      }
      if (isContainer(componentType)) {
        // Resolve nested generics like Map<String, List<SomeType>>
        return getComponentClass(componentType);
      }
      return componentType.toClass();
    }

    @Nullable
    private ResolvableType getComponentType(ResolvableType type) {
      if (type.isArray()) {
        return type.getComponentType();
      }
      if (isCollection(type)) {
        return type.asCollection().getGeneric();
      }
      if (isMap(type)) {
        return type.asMap().getGeneric(1);
      }
      return null;
    }

    private boolean isContainer(ResolvableType type) {
      return type.isArray() || isCollection(type) || isMap(type);
    }

    private boolean isCollection(ResolvableType type) {
      return Collection.class.isAssignableFrom(type.toClass());
    }

    private boolean isMap(ResolvableType type) {
      return Map.class.isAssignableFrom(type.toClass());
    }

    /**
     * Specify whether the specified property refer to a nested type. A nested type
     * represents a sub-namespace that need to be fully resolved. Nested types are
     * either inner classes or annotated with {@link NestedConfigurationProperty}.
     *
     * @param propertyName the name of the property
     * @param propertyType the type of the property
     * @return whether the specified {@code propertyType} is a nested type
     */
    private boolean isNestedType(String propertyName, Class<?> propertyType) {
      Class<?> declaringClass = propertyType.getDeclaringClass();
      if (declaringClass != null && isNested(declaringClass, this.type)) {
        return true;
      }
      Field field = ReflectionUtils.findField(this.type, propertyName);
      return (field != null) && MergedAnnotations.from(field).isPresent(Nested.class);
    }

    private static boolean isNested(Class<?> type, Class<?> candidate) {
      if (type.isAssignableFrom(candidate)) {
        return true;
      }
      return (candidate.getDeclaringClass() != null && isNested(type, candidate.getDeclaringClass()));
    }

    private boolean isJavaType(Class<?> candidate) {
      return candidate.getPackageName().startsWith("java.");
    }

  }

}
