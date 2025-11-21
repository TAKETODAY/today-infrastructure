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

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/21 16:34
 */
class AnnotatedElementAdapterTests {

  @Test
  void emptyAdapterHasNoAnnotations() {
    var adapter = AnnotatedElementAdapter.EMPTY;

    assertThat(adapter.getAnnotations()).isEmpty();
    assertThat(adapter.isEmpty()).isTrue();
  }

  @Test
  void adapterExposesProvidedAnnotations() {
    var annotations = new Annotation[] { createAnnotation() };
    var adapter = AnnotatedElementAdapter.forAnnotations(annotations);

    assertThat(adapter.getAnnotations()).containsExactly(annotations);
    assertThat(adapter.isEmpty()).isFalse();
  }

  @Test
  void adapterWithNullAnnotationsIsEmpty() {
    var adapter = AnnotatedElementAdapter.forAnnotations(null);

    assertThat(adapter.isEmpty()).isTrue();
    assertThat(adapter.getAnnotations()).isEmpty();
  }

  @Test
  void getAnnotationReturnsMatchingAnnotation() {
    var annotation = createAnnotation();
    var adapter = AnnotatedElementAdapter.forAnnotations(new Annotation[] { annotation });

    assertThat(adapter.getAnnotation(annotation.annotationType())).isSameAs(annotation);
  }

  @Test
  void getAnnotationReturnsNullWhenNoMatch() {
    var adapter = AnnotatedElementAdapter.forAnnotations(new Annotation[] { createAnnotation() });

    assertThat(adapter.getAnnotation(Deprecated.class)).isNull();
  }

  @Test
  void isAnnotationPresentReturnsTrueForMatchingAnnotation() {
    var annotation = createAnnotation();
    var adapter = AnnotatedElementAdapter.forAnnotations(new Annotation[] { annotation });

    assertThat(adapter.isAnnotationPresent(annotation.annotationType())).isTrue();
  }

  @Test
  void equalAdaptersHaveSameHashCode() {
    var annotations = new Annotation[] { createAnnotation() };
    var adapter1 = AnnotatedElementAdapter.forAnnotations(annotations);
    var adapter2 = AnnotatedElementAdapter.forAnnotations(annotations.clone());

    assertThat(adapter1).isEqualTo(adapter2);
    assertThat(adapter1.hashCode()).isEqualTo(adapter2.hashCode());
  }

  @Test
  void declaredAnnotationsReturnsSameAsGetAnnotations() {
    var annotations = new Annotation[] { createAnnotation() };
    var adapter = new AnnotatedElementAdapter(annotations, null);

    assertThat(adapter.getDeclaredAnnotations()).containsExactly(adapter.getAnnotations());
  }

  @Test
  void getAnnotationsReturnsCopyOfArray() {
    var annotations = new Annotation[] { createAnnotation() };
    var adapter = new AnnotatedElementAdapter(annotations, null);
    var returned = adapter.getAnnotations();

    assertThat(returned).isNotSameAs(annotations);
    assertThat(returned).containsExactly(annotations);
  }

  @Test
  void annotationsAreLazyInitializedFromAnnotatedElement() {
    var annotation = createAnnotation();
    var annotated = mock(AnnotatedElement.class);
    when(annotated.getAnnotations()).thenReturn(new Annotation[] { annotation });

    var adapter = new AnnotatedElementAdapter(null, annotated);
    assertThat(adapter.getAnnotations()).containsExactly(annotation);
  }

  @Test
  void toStringContainsAnnotations() {
    var annotation = createAnnotation();
    var adapter = new AnnotatedElementAdapter(new Annotation[] { annotation }, null);

    assertThat(adapter.toString()).contains("annotations=[" + annotation + "]");
  }

  @Test
  void differentAdaptersWithSameAnnotationsAreEqual() {
    var annotations = new Annotation[] { createAnnotation() };
    var adapter1 = new AnnotatedElementAdapter(annotations, null);
    var adapter2 = new AnnotatedElementAdapter(annotations.clone(), null);

    assertThat(adapter1).isEqualTo(adapter2);
  }

  @Test
  void adapterIsNotEqualToOtherTypes() {
    var adapter = new AnnotatedElementAdapter(new Annotation[0], null);
    assertThat(adapter).isNotEqualTo("not an adapter");
  }

  @Test
  void getAnnotationWithNullElement() {
    var adapter = new AnnotatedElementAdapter(null, null);
    assertThat(adapter.getAnnotation(Override.class)).isNull();
  }

  @Test
  void isAnnotationPresentWithNullElement() {
    var adapter = new AnnotatedElementAdapter(null, null);
    assertThat(adapter.isAnnotationPresent(Override.class)).isFalse();
  }

  @Test
  void multipleCallsToGetAnnotationsReturnSameInstance() {
    var annotated = mock(AnnotatedElement.class);
    when(annotated.getAnnotations()).thenReturn(new Annotation[] { createAnnotation() });

    var adapter = new AnnotatedElementAdapter(null, annotated);
    var first = adapter.getAnnotations(false);
    var second = adapter.getAnnotations(false);

    assertThat(first).isSameAs(second);
    verify(annotated, times(1)).getAnnotations();
  }

  @Test
  void forAnnotationsReturnsEmptyAdapterWhenArrayIsEmpty() {
    var adapter = AnnotatedElementAdapter.forAnnotations(new Annotation[0]);
    assertThat(adapter).isSameAs(AnnotatedElementAdapter.EMPTY);
  }

  @Test
  void forAnnotationsReturnsEmptyAdapterWhenArrayIsNull() {
    var adapter = AnnotatedElementAdapter.forAnnotations(null);
    assertThat(adapter).isSameAs(AnnotatedElementAdapter.EMPTY);
  }

  @Test
  void forAnnotationsReturnsNewAdapterWhenArrayHasElements() {
    var annotations = new Annotation[] { createAnnotation() };
    var adapter = AnnotatedElementAdapter.forAnnotations(annotations);
    assertThat(adapter).isNotSameAs(AnnotatedElementAdapter.EMPTY);
    assertThat(adapter.getAnnotations()).containsExactly(annotations);
  }

  @Test
  void getDeclaredAnnotationsReturnsCopyOfArray() {
    var annotations = new Annotation[] { createAnnotation() };
    var adapter = new AnnotatedElementAdapter(annotations, null);
    var returned = adapter.getDeclaredAnnotations();

    assertThat(returned).isNotSameAs(annotations);
    assertThat(returned).containsExactly(annotations);
  }

  @Test
  void getDeclaredAnnotationsReturnsSameAsGetAnnotations() {
    var annotations = new Annotation[] { createAnnotation() };
    var adapter = new AnnotatedElementAdapter(annotations, null);

    assertThat(adapter.getDeclaredAnnotations()).containsExactly(adapter.getAnnotations());
  }

  @Test
  void isAnnotationPresentReturnsFalseWhenNoMatch() {
    var adapter = AnnotatedElementAdapter.forAnnotations(new Annotation[] { createAnnotation() });
    assertThat(adapter.isAnnotationPresent(Deprecated.class)).isFalse();
  }

  @Test
  void isAnnotationPresentReturnsFalseWhenAdapterIsEmpty() {
    var adapter = AnnotatedElementAdapter.EMPTY;
    assertThat(adapter.isAnnotationPresent(Override.class)).isFalse();
  }

  @Test
  void getAnnotationReturnsNullWhenAdapterIsEmpty() {
    var adapter = AnnotatedElementAdapter.EMPTY;
    assertThat(adapter.getAnnotation(Override.class)).isNull();
  }

  @Test
  void isEmptyReturnsTrueWhenNoAnnotations() {
    var adapter = new AnnotatedElementAdapter(new Annotation[0], null);
    assertThat(adapter.isEmpty()).isTrue();
  }

  @Test
  void isEmptyReturnsFalseWhenHasAnnotations() {
    var adapter = new AnnotatedElementAdapter(new Annotation[] { createAnnotation() }, null);
    assertThat(adapter.isEmpty()).isFalse();
  }

  @Test
  void annotationsAreNotClonedWhenCloneArrayIsFalse() {
    var annotations = new Annotation[] { createAnnotation() };
    var adapter = new AnnotatedElementAdapter(annotations, null);
    var returned = adapter.getAnnotations(false);

    assertThat(returned).isSameAs(annotations);
  }

  @Test
  void annotationsFromAnnotatedElementAreCached() {
    var annotation = createAnnotation();
    var annotated = mock(AnnotatedElement.class);
    when(annotated.getAnnotations()).thenReturn(new Annotation[] { annotation });

    var adapter = new AnnotatedElementAdapter(null, annotated);
    var first = adapter.getAnnotations();
    var second = adapter.getAnnotations();

    assertThat(first).isEqualTo(second);
    verify(annotated, times(1)).getAnnotations();
  }

  @Test
  void toStringReturnsExpectedFormat() {
    var annotation = createAnnotation();
    var adapter = new AnnotatedElementAdapter(new Annotation[] { annotation }, null);
    var toString = adapter.toString();

    assertThat(toString).startsWith("AnnotatedElementAdapter annotations=[");
    assertThat(toString).contains(annotation.toString());
  }

  @Test
  void equalsReturnsTrueForSameInstance() {
    var adapter = new AnnotatedElementAdapter(new Annotation[0], null);
    assertThat(adapter).isEqualTo(adapter);
  }

  @Test
  void equalsReturnsFalseForNull() {
    var adapter = new AnnotatedElementAdapter(new Annotation[0], null);
    assertThat(adapter).isNotEqualTo(null);
  }

  @Test
  void hashCodeIsConsistent() {
    var annotations = new Annotation[] { createAnnotation() };
    var adapter = new AnnotatedElementAdapter(annotations, null);
    var hashCode1 = adapter.hashCode();
    var hashCode2 = adapter.hashCode();

    assertThat(hashCode1).isEqualTo(hashCode2);
  }

  private static Annotation createAnnotation() {
    return () -> Override.class;
  }

}