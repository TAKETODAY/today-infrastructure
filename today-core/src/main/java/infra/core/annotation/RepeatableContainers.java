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

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

import infra.lang.Assert;
import infra.lang.NullValue;
import infra.lang.Nullable;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.ObjectUtils;

/**
 * Strategy used to determine annotations that act as containers for other
 * annotations. The {@link #standard()} method provides a default
 * strategy that respects Java's {@link Repeatable @Repeatable} support and
 * should be suitable for most situations.
 *
 * <p>The {@link #valueOf} method can be used to register relationships for
 * annotations that do not wish to use {@link Repeatable @Repeatable}.
 *
 * <p>To completely disable repeatable support use {@link #none()}.
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
   * Add an additional explicit relationship between a container and
   * repeatable annotation.
   * <p>WARNING: the arguments supplied to this method are in the reverse order
   * of those supplied to {@link #valueOf(Class, Class)}
   *
   * @param container the container annotation type
   * @param repeatable the repeatable annotation type
   * @return a new {@link RepeatableContainers} instance
   */
  public RepeatableContainers and(Class<? extends Annotation> container, Class<? extends Annotation> repeatable) {
    return new ExplicitRepeatableContainer(this, repeatable, container);
  }

  @Nullable
  Annotation[] findRepeatedAnnotations(Annotation annotation) {
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
   * Create a {@link RepeatableContainers} instance that searches using Java's
   * {@link Repeatable @Repeatable} annotation.
   *
   * @return a {@link RepeatableContainers} instance
   */
  public static RepeatableContainers standard() {
    return StandardRepeatableContainers.INSTANCE;
  }

  /**
   * Create a {@link RepeatableContainers} instance that uses predefined
   * repeatable and container types.
   *
   * @param repeatable the contained repeatable annotation type
   * @param container the container annotation type or {@code null}. If specified,
   * this annotation must declare a {@code value} attribute returning an array
   * of repeatable annotations. If not specified, the container will be
   * deduced by inspecting the {@code @Repeatable} annotation on
   * {@code repeatable}.
   * @return a {@link RepeatableContainers} instance
   * @throws IllegalArgumentException if the supplied container type is
   * {@code null} and the annotation type is not a repeatable annotation
   * @throws AnnotationConfigurationException if the supplied container type
   * is not a properly configured container for a repeatable annotation
   */
  public static RepeatableContainers valueOf(Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {
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
