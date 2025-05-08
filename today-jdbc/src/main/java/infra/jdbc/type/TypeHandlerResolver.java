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

package infra.jdbc.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;

import infra.beans.BeanProperty;
import infra.beans.BeanUtils;
import infra.core.annotation.MergedAnnotation;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * A resolver interface for determining the appropriate {@link TypeHandler} for a given
 * {@link BeanProperty}. It provides mechanisms to resolve type handlers based on annotations,
 * composite resolvers, and chaining logic.
 *
 * <p>This interface is particularly useful in scenarios where type conversion or mapping is required,
 * such as in database access frameworks. It allows for flexible and extensible resolution of type
 * handlers through various strategies.
 *
 * <h3>Usage Examples</h3>
 *
 * <p>Creating a custom resolver:
 * <pre>{@code
 * static class AnyTypeHandlerResolver implements TypeHandlerResolver {
 *   @Nullable
 *   @Override
 *   public TypeHandler<?> resolve(BeanProperty property) {
 *     return new AnyTypeHandler<>(property.getType());
 *   }
 * }
 * }</pre>
 *
 * <p>Chaining multiple resolvers:
 * <pre>{@code
 * TypeHandlerResolver resolver1 = TypeHandlerResolver.forMappedTypeHandlerAnnotation();
 * TypeHandlerResolver resolver2 = new AnyTypeHandlerResolver();
 * TypeHandlerResolver chain = resolver1.and(resolver2);
 * }</pre>
 *
 * <p>Using a composite resolver:
 * <pre>{@code
 * List<TypeHandlerResolver> resolvers = Arrays.asList(
 *   TypeHandlerResolver.forMappedTypeHandlerAnnotation(),
 *   new AnyTypeHandlerResolver()
 * );
 * TypeHandlerResolver composite = TypeHandlerResolver.composite(resolvers);
 * }</pre>
 *
 * <h3>Static Factory Methods</h3>
 * <ul>
 *   <li>{@link #forMappedTypeHandlerAnnotation()}: Creates a resolver for the {@link MappedTypeHandler} annotation.</li>
 *   <li>{@link #forAnnotation(Class)}: Creates a resolver for a custom annotation type.</li>
 *   <li>{@link #composite(TypeHandlerResolver...)}: Combines multiple resolvers into a single composite resolver.</li>
 * </ul>
 *
 * <h3>Default Methods</h3>
 * <ul>
 *   <li>{@link #and(TypeHandlerResolver)}: Chains this resolver with another resolver.</li>
 * </ul>
 *
 * <p>Note: Implementations should ensure thread safety if used in concurrent environments.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/30 23:32
 */
public interface TypeHandlerResolver {

  /**
   * Resolves a {@link TypeHandler} for the given {@link BeanProperty}.
   *
   * <p>This method attempts to find and return a suitable {@link TypeHandler}
   * based on the provided {@link BeanProperty}. If no appropriate handler
   * can be resolved, the method returns {@code null}.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * BeanProperty property = ...; // Obtain a BeanProperty instance
   * TypeHandlerResolver resolver = ...; // Obtain a TypeHandlerResolver instance
   *
   * TypeHandler<?> typeHandler = resolver.resolve(property);
   * if (typeHandler != null) {
   *   // Use the resolved TypeHandler
   *   typeHandler.setParameter(preparedStatement, 1, value);
   * }
   * else {
   *   // Handle the case where no TypeHandler is resolved
   *   System.out.println("No TypeHandler found for the given property.");
   * }
   * }</pre>
   *
   * @param property the {@link BeanProperty} for which a {@link TypeHandler}
   * needs to be resolved. It may be {@code null}, in which case
   * the behavior depends on the implementation.
   * @return a {@link TypeHandler} instance if one can be resolved for the
   * given property, or {@code null} if no suitable handler is found.
   */
  @Nullable
  TypeHandler<?> resolve(BeanProperty property);

  /**
   * Combines this {@code TypeHandlerResolver} with another resolver in a chain.
   *
   * <p>This method creates a new {@code TypeHandlerResolver} that first attempts
   * to resolve a {@link TypeHandler} using the current resolver. If no suitable
   * handler is found, it delegates the resolution to the provided {@code next}
   * resolver.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * TypeHandlerResolver resolver1 = ...; // Obtain the first resolver
   * TypeHandlerResolver resolver2 = ...; // Obtain the second resolver
   *
   * // Combine resolvers into a chain
   * TypeHandlerResolver combinedResolver = resolver1.and(resolver2);
   *
   * BeanProperty property = ...; // Obtain a BeanProperty instance
   * TypeHandler<?> typeHandler = combinedResolver.resolve(property);
   *
   * if (typeHandler != null) {
   *   // Use the resolved TypeHandler
   *   typeHandler.setParameter(preparedStatement, 1, value);
   * }
   * else {
   *   // Handle the case where no TypeHandler is resolved
   *   System.out.println("No TypeHandler found for the given property.");
   * }
   * }</pre>
   *
   * @param next the next {@code TypeHandlerResolver} in the chain. It may not be
   * {@code null}, but its behavior when {@code null} is passed depends
   * on the implementation.
   * @return a new {@code TypeHandlerResolver} that combines the current resolver
   * with the provided {@code next} resolver. The returned resolver will
   * attempt resolution in sequence, starting with the current resolver
   * and falling back to {@code next} if necessary.
   */
  default TypeHandlerResolver and(TypeHandlerResolver next) {
    return property -> {
      TypeHandler<?> resolved = resolve(property);
      if (resolved == null) {
        resolved = next.resolve(property);
      }
      return resolved;
    };
  }

  /**
   * Creates a composite {@link TypeHandlerResolver} by combining multiple resolvers.
   *
   * <p>This method takes an array of {@link TypeHandlerResolver} instances and returns
   * a new resolver that attempts to resolve a {@link TypeHandler} by delegating to each
   * resolver in the provided order. If no suitable handler is found by any resolver,
   * the composite resolver will return {@code null}.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * TypeHandlerResolver resolver1 = ...; // Obtain the first resolver
   * TypeHandlerResolver resolver2 = ...; // Obtain the second resolver
   * TypeHandlerResolver resolver3 = ...; // Obtain the third resolver
   *
   * // Create a composite resolver
   * TypeHandlerResolver compositeResolver = TypeHandlerResolver.composite(
   *   resolver1, resolver2, resolver3
   * );
   *
   * BeanProperty property = ...; // Obtain a BeanProperty instance
   * TypeHandler<?> typeHandler = compositeResolver.resolve(property);
   *
   * if (typeHandler != null) {
   *   // Use the resolved TypeHandler
   *   typeHandler.setParameter(preparedStatement, 1, value);
   * }
   * else {
   *   // Handle the case where no TypeHandler is resolved
   *   System.out.println("No TypeHandler found for the given property.");
   * }
   * }</pre>
   *
   * @param resolvers an array of {@link TypeHandlerResolver} instances to be combined.
   * Must not be {@code null}, but individual elements may be {@code null}.
   * @return a new {@link TypeHandlerResolver} that sequentially delegates to the provided
   * resolvers until a suitable {@link TypeHandler} is found or all resolvers have
   * been exhausted.
   */
  static TypeHandlerResolver composite(TypeHandlerResolver... resolvers) {
    Assert.notNull(resolvers, "TypeHandlerResolver is required");
    return composite(List.of(resolvers));
  }

  /**
   * Creates a composite {@link TypeHandlerResolver} by combining multiple resolvers.
   *
   * <p>This method takes a list of {@link TypeHandlerResolver} instances and returns
   * a new resolver that attempts to resolve a {@link TypeHandler} by delegating to each
   * resolver in the provided order. If no suitable handler is found by any resolver,
   * the composite resolver will return {@code null}.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * TypeHandlerResolver resolver1 = ...; // Obtain the first resolver
   * TypeHandlerResolver resolver2 = ...; // Obtain the second resolver
   * TypeHandlerResolver resolver3 = ...; // Obtain the third resolver
   *
   * // Create a composite resolver
   * TypeHandlerResolver compositeResolver = TypeHandlerResolver.composite(
   *   Arrays.asList(resolver1, resolver2, resolver3)
   * );
   *
   * BeanProperty property = ...; // Obtain a BeanProperty instance
   * TypeHandler<?> typeHandler = compositeResolver.resolve(property);
   *
   * if (typeHandler != null) {
   *   // Use the resolved TypeHandler
   *   typeHandler.setParameter(preparedStatement, 1, value);
   * }
   * else {
   *   // Handle the case where no TypeHandler is resolved
   *   System.out.println("No TypeHandler found for the given property.");
   * }
   * }</pre>
   *
   * @param resolvers a list of {@link TypeHandlerResolver} instances to be combined.
   * Must not be {@code null}, but individual elements may be {@code null}.
   * @return a new {@link TypeHandlerResolver} that sequentially delegates to the provided
   * resolvers until a suitable {@link TypeHandler} is found or all resolvers have
   * been exhausted.
   */
  static TypeHandlerResolver composite(List<TypeHandlerResolver> resolvers) {
    Assert.notNull(resolvers, "TypeHandlerResolver is required");
    return property -> {
      for (TypeHandlerResolver resolver : resolvers) {
        TypeHandler<?> resolved = resolver.resolve(property);
        if (resolved != null) {
          return resolved;
        }
      }

      return null;
    };
  }

  /**
   * Creates a {@link TypeHandlerResolver} that resolves {@link TypeHandler} instances
   * based on the {@link MappedTypeHandler} annotation.
   *
   * <p>This method is a convenience wrapper around {@link #forAnnotation(Class)} and
   * specifically targets the {@link MappedTypeHandler} annotation. It resolves
   * {@link TypeHandler} instances by inspecting the {@code value} attribute of the
   * {@link MappedTypeHandler} annotation.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * // Define a field with the @MappedTypeHandler annotation
   * public class User {
   *   @MappedTypeHandler(MyCustomTypeHandler.class)
   *   private String customField;
   * }
   *
   * // Use the resolver to resolve the TypeHandler for the annotated field
   * BeanProperty property = BeanProperty.valueOf(User.class, "customField");
   * TypeHandlerResolver resolver = TypeHandlerResolver.forMappedTypeHandlerAnnotation();
   *
   * TypeHandler<?> typeHandler = resolver.resolve(property);
   * if (typeHandler != null) {
   *   // Use the resolved TypeHandler
   *   typeHandler.setParameter(preparedStatement, 1, value);
   * }
   * else {
   *   // Handle the case where no TypeHandler is resolved
   *   System.out.println("No TypeHandler found for the given property.");
   * }
   * }</pre>
   *
   * @return a {@link TypeHandlerResolver} that resolves {@link TypeHandler} instances
   * based on the {@link MappedTypeHandler} annotation.
   * @see #forAnnotation(Class)
   * @see MappedTypeHandler
   */
  static TypeHandlerResolver forMappedTypeHandlerAnnotation() {
    return forAnnotation(MappedTypeHandler.class);
  }

  /**
   * Returns a {@code TypeHandlerResolver} for the specified annotation type.
   * This method is a convenience overload that uses the default attribute name
   * {@code MergedAnnotation.VALUE} to resolve the annotation.
   *
   * <p>Example usage:
   * <pre>{@code
   *   TypeHandlerResolver resolver = TypeHandlerResolver.forAnnotation(MyCustomAnnotation.class);
   *   // Use the resolver to process annotations or retrieve associated type handlers
   * }</pre>
   *
   * @param annotationType the annotation type for which the resolver is created;
   * must not be null
   * @return a {@code TypeHandlerResolver} instance configured for the given annotation type
   */
  static TypeHandlerResolver forAnnotation(Class<? extends Annotation> annotationType) {
    return forAnnotation(annotationType, MergedAnnotation.VALUE);
  }

  /**
   * Creates a {@code TypeHandlerResolver} based on the specified annotation type and attribute name.
   * This method is used to dynamically resolve a {@code TypeHandler} for a given property
   * by inspecting the presence of the specified annotation and extracting the type handler class
   * from the provided attribute name.
   *
   * <p>Example usage:
   * <pre>{@code
   *   TypeHandlerResolver resolver = forAnnotation(MyAnnotation.class, "typeHandler");
   *   BeanProperty property = ...; // Obtain the property instance
   *   TypeHandler<?> typeHandler = resolver.resolve(property);
   *   if (typeHandler != null) {
   *     // Use the resolved type handler
   *   }
   * }</pre>
   *
   * @param annotationType the annotation type to inspect for the type handler configuration;
   * must not be {@code null}
   * @param attributeName the name of the attribute in the annotation that specifies the
   * type handler class; must not be {@code null}
   * @return a {@code TypeHandlerResolver} that resolves a type handler based on the provided
   * annotation and attribute name, or {@code null} if the annotation is not present
   * or the attribute does not specify a valid type handler
   */
  static TypeHandlerResolver forAnnotation(Class<? extends Annotation> annotationType, String attributeName) {
    Assert.notNull(attributeName, "attributeName is required");
    Assert.notNull(annotationType, "annotationType is required");

    return (BeanProperty property) -> {
      var mappedTypeHandler = property.mergedAnnotations().get(annotationType);
      if (mappedTypeHandler.isPresent()) {
        // user defined TypeHandler
        Class<? extends TypeHandler<?>> typeHandlerClass = mappedTypeHandler.getClass(attributeName);
        Constructor<? extends TypeHandler<?>> constructor = BeanUtils.getConstructor(typeHandlerClass);
        if (constructor == null) {
          throw new IllegalStateException("No suitable constructor in " + typeHandlerClass);
        }

        if (constructor.getParameterCount() != 0) {
          Object[] args = new Object[constructor.getParameterCount()];
          Class<?>[] parameterTypes = constructor.getParameterTypes();
          int i = 0;
          for (Class<?> parameterType : parameterTypes) {
            args[i++] = resolveArg(property, parameterType);
          }
          return BeanUtils.newInstance(constructor, args);
        }
        else {
          return BeanUtils.newInstance(constructor);
        }
      }

      return null;
    };
  }

  private static Object resolveArg(BeanProperty beanProperty, Class<?> parameterType) {
    if (parameterType == Class.class) {
      return beanProperty.getType();
    }
    if (parameterType == BeanProperty.class) {
      return beanProperty;
    }
    throw new IllegalArgumentException(
            "TypeHandler Constructor parameterType '%s' currently not supported".formatted(parameterType.getName()));
  }

}
