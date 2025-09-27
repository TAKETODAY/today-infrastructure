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

import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;

import infra.lang.Constant;
import infra.util.ObjectUtils;

/**
 * Adapter for exposing a set of annotations as an {@link AnnotatedElement}, in
 * particular as input for various methods in {@link AnnotatedElementUtils}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AnnotatedElementUtils#isAnnotated(AnnotatedElement, Class)
 * @see AnnotatedElementUtils#getMergedAnnotation(AnnotatedElement, Class)
 * @since 5.0 2025/3/21 16:19
 */
public final class AnnotatedElementAdapter implements AnnotatedElement, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final AnnotatedElementAdapter EMPTY = new AnnotatedElementAdapter(Constant.EMPTY_ANNOTATIONS, null);

  private volatile Annotation @Nullable [] annotations;

  @Nullable
  private final AnnotatedElement annotated;

  public AnnotatedElementAdapter(Annotation @Nullable [] annotations, @Nullable AnnotatedElement annotated) {
    this.annotations = annotations;
    this.annotated = annotated;
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    for (Annotation annotation : getAnnotations(false)) {
      if (annotation.annotationType() == annotationClass) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Nullable
  @SuppressWarnings("unchecked")
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    for (Annotation annotation : getAnnotations(false)) {
      if (annotation.annotationType() == annotationClass) {
        return (T) annotation;
      }
    }
    return null;
  }

  @Override
  public Annotation[] getAnnotations() {
    return getAnnotations(true);
  }

  Annotation[] getAnnotations(boolean cloneArray) {
    Annotation[] annotations = this.annotations;
    if (annotations == null) {
      synchronized(this) {
        annotations = this.annotations;
        if (annotations == null) {
          if (annotated != null) {
            annotations = annotated.getAnnotations();
          }
          else {
            annotations = Constant.EMPTY_ANNOTATIONS;
          }
          this.annotations = annotations;
        }
      }
    }
    if (cloneArray && annotations.length > 0) {
      return annotations.clone();
    }
    return annotations;
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return getAnnotations(true);
  }

  /**
   * Determine if this {@code AnnotatedElementAdapter} is empty.
   *
   * @return {@code true} if this adapter contains no annotations
   */
  public boolean isEmpty() {
    return ObjectUtils.isEmpty(getAnnotations(false));
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return (this == obj || (obj instanceof AnnotatedElementAdapter that
            && Arrays.equals(getAnnotations(false), that.getAnnotations(false))));
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(getAnnotations(false));
  }

  @Override
  public String toString() {
    return "AnnotatedElementAdapter annotations=" + Arrays.toString(getAnnotations(false));
  }

  // Static

  /**
   * Create an {@code AnnotatedElementAdapter} from the supplied annotations.
   * <p>The supplied annotations will be considered to be both <em>present</em>
   * and <em>directly present</em> with regard to the results returned from
   * methods such as {@link #getAnnotation(Class)},
   * {@link #getDeclaredAnnotation(Class)}, etc.
   * <p>If the supplied annotations array is either {@code null} or empty, this
   * factory method will return an {@linkplain #isEmpty() empty} adapter.
   *
   * @param annotations the annotations to expose via the {@link AnnotatedElement}
   * API
   * @return a new {@code AnnotatedElementAdapter}
   */
  public static AnnotatedElementAdapter forAnnotations(Annotation @Nullable [] annotations) {
    if (annotations == null || annotations.length == 0) {
      return EMPTY;
    }
    return new AnnotatedElementAdapter(annotations, null);
  }

}
