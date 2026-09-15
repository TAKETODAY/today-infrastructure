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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import infra.beans.BeanProperty;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.jdbc.type.TypeHandler;
import infra.persistence.annotation.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class EntityPropertyTests {

  private final BeanProperty beanProperty = mock(BeanProperty.class);

  @SuppressWarnings("unchecked")
  private final TypeHandler<Object> typeHandler = mock(TypeHandler.class);

  private EntityProperty entityProperty(boolean isIdProperty) {
    return new EntityProperty(beanProperty, "test_column", typeHandler, isIdProperty);
  }

  @Test
  void getNameDelegatesToBeanProperty() {
    when(beanProperty.getName()).thenReturn("name");

    assertThat(entityProperty(false).getName()).isEqualTo("name");
    verify(beanProperty).getName();
  }

  @Test
  void getTypeDelegatesToBeanProperty() {
    doReturn(String.class).when(beanProperty).getType();

    assertThat(entityProperty(false).getType()).isEqualTo(String.class);
  }

  @Test
  void hasValueReturnsFalseWhenValueIsNull() {
    Object entity = new Object();
    when(beanProperty.getValue(entity)).thenReturn(null);

    assertThat(entityProperty(false).hasValue(entity)).isFalse();
  }

  @Test
  void hasValueReturnsTrueWhenValueIsPresent() {
    Object entity = new Object();
    when(beanProperty.getValue(entity)).thenReturn("TODAY");

    assertThat(entityProperty(false).hasValue(entity)).isTrue();
  }

  @Test
  void synthesizedAnnotationReturnsSynthesizedInstance() {
    MergedAnnotations annotations = mock(MergedAnnotations.class);
    @SuppressWarnings("unchecked")
    MergedAnnotation<Id> mergedAnnotation = mock(MergedAnnotation.class);
    Id synthesized = mock(Id.class);

    when(beanProperty.mergedAnnotations()).thenReturn(annotations);
    when(annotations.get(Id.class)).thenReturn(mergedAnnotation);
    when(mergedAnnotation.synthesize()).thenReturn(synthesized);

    assertThat(entityProperty(false).synthesizedAnnotation(Id.class)).isSameAs(synthesized);
  }

  @Test
  void synthesizedAnnotationThrowsWhenAnnotationMissing() {
    MergedAnnotations annotations = mock(MergedAnnotations.class);
    @SuppressWarnings("unchecked")
    MergedAnnotation<Id> mergedAnnotation = mock(MergedAnnotation.class);

    when(beanProperty.mergedAnnotations()).thenReturn(annotations);
    when(annotations.get(Id.class)).thenReturn(mergedAnnotation);
    when(mergedAnnotation.synthesize()).thenThrow(new NoSuchElementException());

    assertThatThrownBy(() -> entityProperty(false).synthesizedAnnotation(Id.class))
            .isInstanceOf(NoSuchElementException.class);
  }

}