/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties.bind;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import infra.aot.hint.ExecutableMode;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.context.properties.NestedConfigurationProperty;
import infra.context.properties.bind.JavaBeanBinder.BeanProperty;
import infra.core.ResolvableType;
import infra.core.annotation.MergedAnnotations;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ReflectionUtils;

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

  private static final Logger logger = LoggerFactory.getLogger(BindableRuntimeHintsRegistrar.class);

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
    for (Bindable<?> bindable : this.bindables) {
      try {
        new Processor(bindable).process(hints.reflection());
      }
      catch (Exception ex) {
        logger.debug("Skipping hints for {}", bindable, ex);
      }
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

    private final JavaBeanBinder.@Nullable BeanProperties bean;

    private final Set<Class<?>> seen;

    Processor(Bindable<?> bindable) {
      this(bindable, false, new HashSet<>());
    }

    @SuppressWarnings("NullAway")
    private Processor(Bindable<?> bindable, boolean nestedType, Set<Class<?>> seen) {
      this.type = bindable.getType().getRawClass();
      this.bindConstructor = (bindable.getBindMethod() != BindMethod.JAVA_BEAN)
              ? BindConstructorProvider.DEFAULT.getBindConstructor(bindable.getType().resolve(), nestedType)
              : null;
      this.bean = JavaBeanBinder.BeanProperties.of(bindable);
      this.seen = seen;
    }

    void process(ReflectionHints hints) {
      if (this.seen.contains(this.type)) {
        return;
      }
      this.seen.add(this.type);
      handleConstructor(hints);
      if (this.bindConstructor != null) {
        handleValueObjectProperties(hints, bindConstructor);
      }
      else if (this.bean != null && !this.bean.getProperties().isEmpty()) {
        handleJavaBeanProperties(bean, hints);
      }
    }

    private void handleConstructor(ReflectionHints hints) {
      if (this.bindConstructor != null) {
        hints.registerConstructor(this.bindConstructor, ExecutableMode.INVOKE);
        return;
      }
      Arrays.stream(this.type.getDeclaredConstructors())
              .filter(this::hasNoParameters)
              .findFirst()
              .ifPresent((constructor) -> hints.registerConstructor(constructor, ExecutableMode.INVOKE));
    }

    private boolean hasNoParameters(Constructor<?> candidate) {
      return candidate.getParameterCount() == 0;
    }

    private void handleValueObjectProperties(ReflectionHints hints, Constructor<?> bindConstructor) {
      for (int i = 0; i < bindConstructor.getParameterCount(); i++) {
        String propertyName = bindConstructor.getParameters()[i].getName();
        ResolvableType propertyType = ResolvableType.forConstructorParameter(bindConstructor, i);
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

        Field field = property.field;
        if (field != null) {
          hints.registerField(field);
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
      new Processor(Bindable.of(type), true, this.seen).process(hints);
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
