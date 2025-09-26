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

package infra.core.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

import infra.lang.Assert;
import infra.lang.NullValue;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.ObjectUtils;

/**
 * Strategy used to find repeatable annotations within container annotations.
 *
 * <p>{@link #standard() RepeatableContainers.standard()}
 * provides a default strategy that respects Java's {@link Repeatable @Repeatable}
 * support and is suitable for most situations.
 *
 * <p>If you need to register repeatable annotation types that do not make use of
 * {@code @Repeatable}, you should typically use {@code standard()}
 * combined with {@link #plus(Class, Class)}. Note that multiple invocations of
 * {@code plus()} can be chained together to register multiple repeatable/container
 * type pairs. For example:
 *
 * <pre class="code">
 * RepeatableContainers repeatableContainers =
 *     RepeatableContainers.standard()
 *         .plus(MyRepeatable1.class, MyContainer1.class)
 *         .plus(MyRepeatable2.class, MyContainer2.class);</pre>
 *
 * <p>For special use cases where you are certain that you do not need Java's
 * {@code @Repeatable} support, you can use {@link #explicit(Class, Class)
 * RepeatableContainers.explicit()} to create an instance of
 * {@code RepeatableContainers} that only supports explicit repeatable/container
 * type pairs. As with {@code standard()}, {@code plus()} can be used
 * to register additional repeatable/container type pairs. For example:
 *
 * <pre class="code">
 * RepeatableContainers repeatableContainers =
 *     RepeatableContainers.explicit(MyRepeatable1.class, MyContainer1.class)
 *         .plus(MyRepeatable2.class, MyContainer2.class);</pre>
 *
 * <p>To completely disable repeatable annotation support use
 * {@link #none() RepeatableContainers.none()}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RepeatableContainers {

  static final ConcurrentReferenceHashMap<Class<? extends Annotation>, Object>
          cache = new ConcurrentReferenceHashMap<>();

  /**
   * No repeatable containers.
   */
  public static final RepeatableContainers NONE = new RepeatableContainers(null);

  @Nullable
  private final RepeatableContainers parent;

  private RepeatableContainers(@Nullable RepeatableContainers parent) {
    this.parent = parent;
  }

  /**
   * Register a pair of repeatable and container annotation types.
   * <p>See the {@linkplain RepeatableContainers class-level javadoc} for examples.
   *
   * @param repeatable the repeatable annotation type
   * @param container the container annotation type
   * @return a new {@code RepeatableContainers} instance that is chained to
   * the current instance
   */
  public RepeatableContainers plus(Class<? extends Annotation> container, Class<? extends Annotation> repeatable) {
    return new ExplicitRepeatableContainer(this, repeatable, container);
  }

  /**
   * Find repeated annotations contained in the supplied {@code annotation}.
   *
   * @param annotation the candidate container annotation
   * @return the repeated annotations found in the supplied container annotation
   * (potentially an empty array), or {@code null} if the supplied annotation is
   * not a supported container annotation
   */
  Annotation @Nullable [] findRepeatedAnnotations(Annotation annotation) {
    if (this.parent == null) {
      return null;
    }
    return this.parent.findRepeatedAnnotations(annotation);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == this) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    return Objects.equals(this.parent, ((RepeatableContainers) other).parent);
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(this.parent);
  }

  // static

  /**
   * Create a {@link RepeatableContainers} instance that does not expand any
   * repeatable annotations.
   *
   * @return a {@link RepeatableContainers} instance
   */
  public static RepeatableContainers none() {
    return NONE;
  }

  /**
   * Create a {@link RepeatableContainers} instance that searches for repeated
   * annotations according to the semantics of Java's {@link Repeatable @Repeatable}
   * annotation.
   * <p>See the {@linkplain RepeatableContainers class-level javadoc} for examples.
   *
   * @return a {@code RepeatableContainers} instance that supports {@code @Repeatable}
   * @see #plus(Class, Class)
   */
  public static RepeatableContainers standard() {
    return StandardRepeatableContainers.INSTANCE;
  }

  /**
   * Create a {@link RepeatableContainers} instance that searches for repeated
   * annotations by taking into account the supplied repeatable and container
   * annotation types.
   * <p><strong>WARNING</strong>: The {@code RepeatableContainers} instance
   * returned by this factory method does <strong>not</strong> respect Java's
   * {@link Repeatable @Repeatable} support. Use {@link #standard()}
   * for standard {@code @Repeatable} support, optionally combined with
   * {@link #plus(Class, Class)}.
   * <p>If the supplied container annotation type is not {@code null}, it must
   * declare a {@code value} attribute returning an array of repeatable
   * annotations. If the supplied container annotation type is {@code null}, the
   * container will be deduced by inspecting the {@code @Repeatable} annotation
   * on the {@code repeatable} annotation type.
   * <p>See the {@linkplain RepeatableContainers class-level javadoc} for examples.
   *
   * @param repeatable the repeatable annotation type
   * @param container the container annotation type or {@code null}
   * @return a {@code RepeatableContainers} instance that does not support
   * {@link Repeatable @Repeatable}
   * @throws IllegalArgumentException if the supplied container type is
   * {@code null} and the annotation type is not a repeatable annotation
   * @throws AnnotationConfigurationException if the supplied container type
   * is not a properly configured container for a repeatable annotation
   * @see #standard()
   * @see #plus(Class, Class)
   */
  public static RepeatableContainers explicit(Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {
    return new ExplicitRepeatableContainer(null, repeatable, container);
  }

  /**
   * Standard {@link RepeatableContainers} implementation that searches using
   * Java's {@link Repeatable @Repeatable} annotation.
   */
  private static class StandardRepeatableContainers extends RepeatableContainers
          implements Function<Class<? extends Annotation>, Object> {

    private static final Object NONE = NullValue.INSTANCE;

    private static final StandardRepeatableContainers INSTANCE = new StandardRepeatableContainers();

    StandardRepeatableContainers() {
      super(null);
    }

    @Override
    @Nullable
    Annotation[] findRepeatedAnnotations(Annotation annotation) {
      Method method = getRepeatedAnnotationsMethod(annotation.annotationType());
      if (method != null) {
        return (Annotation[]) AnnotationUtils.invokeAnnotationMethod(method, annotation);
      }
      return super.findRepeatedAnnotations(annotation);
    }

    @Nullable
    private Method getRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
      Object result = cache.computeIfAbsent(annotationType, this);
      return result != NONE ? (Method) result : null;
    }

    @Override
    public Object apply(Class<? extends Annotation> annotationType) {
      return computeRepeatedAnnotationsMethod(annotationType);
    }

    private static Object computeRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
      AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
      Method method = methods.get(MergedAnnotation.VALUE);
      if (method != null) {
        Class<?> returnType = method.getReturnType();
        if (returnType.isArray()) {
          Class<?> componentType = returnType.getComponentType();
          if (Annotation.class.isAssignableFrom(componentType)
                  && componentType.isAnnotationPresent(Repeatable.class)) {
            return method;
          }
        }
      }
      return NONE;
    }
  }

  /**
   * A single explicit mapping.
   */
  private static class ExplicitRepeatableContainer extends RepeatableContainers {

    private final Class<? extends Annotation> repeatable;

    private final Class<? extends Annotation> container;

    private final Method valueMethod;

    ExplicitRepeatableContainer(@Nullable RepeatableContainers parent,
            Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {
      super(parent);
      Assert.notNull(repeatable, "Repeatable is required");
      if (container == null) {
        container = deduceContainer(repeatable);
      }
      Method valueMethod = AttributeMethods.forAnnotationType(container).get(MergedAnnotation.VALUE);
      try {
        if (valueMethod == null) {
          throw new NoSuchMethodException("No value method found");
        }
        Class<?> returnType = valueMethod.getReturnType();
        if (returnType.componentType() != repeatable) {
          throw new AnnotationConfigurationException(
                  "Container type [%s] must declare a 'value' attribute for an array of type [%s]"
                          .formatted(container.getName(), repeatable.getName()));
        }
      }
      catch (AnnotationConfigurationException ex) {
        throw ex;
      }
      catch (Throwable ex) {
        throw new AnnotationConfigurationException(
                "Invalid declaration of container type [%s] for repeatable annotation [%s]"
                        .formatted(container.getName(), repeatable.getName()), ex);
      }
      this.container = container;
      this.repeatable = repeatable;
      this.valueMethod = valueMethod;
    }

    private Class<? extends Annotation> deduceContainer(Class<? extends Annotation> repeatable) {
      Repeatable annotation = repeatable.getAnnotation(Repeatable.class);
      if (annotation == null) {
        throw new IllegalArgumentException(
                "Annotation type must be a repeatable annotation: failed to resolve container type for "
                        + repeatable.getName());
      }
      return annotation.value();
    }

    @Override
    @Nullable
    Annotation[] findRepeatedAnnotations(Annotation annotation) {
      if (this.container.isAssignableFrom(annotation.annotationType())) {
        return (Annotation[]) AnnotationUtils.invokeAnnotationMethod(valueMethod, annotation);
      }
      return super.findRepeatedAnnotations(annotation);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (!super.equals(other)) {
        return false;
      }
      ExplicitRepeatableContainer otherErc = (ExplicitRepeatableContainer) other;
      return (this.container.equals(otherErc.container) && this.repeatable.equals(otherErc.repeatable));
    }

    @Override
    public int hashCode() {
      int hashCode = super.hashCode();
      hashCode = 31 * hashCode + this.container.hashCode();
      hashCode = 31 * hashCode + this.repeatable.hashCode();
      return hashCode;
    }
  }

}
