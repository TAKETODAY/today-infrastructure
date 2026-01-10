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

package infra.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 18:36
 */
class ReactiveTypeDescriptorTests {

  @Test
  void multiValueTypeProduces0ToNValues() {
    ReactiveTypeDescriptor descriptor = ReactiveTypeDescriptor.multiValue(List.class, ArrayList::new);
    assertThat(descriptor.isMultiValue()).isTrue();
    assertThat(descriptor.isNoValue()).isFalse();
    assertThat(descriptor.supportsEmpty()).isTrue();
    assertThat(descriptor.getEmptyValue()).isInstanceOf(ArrayList.class);
    assertThat(descriptor.isDeferred()).isTrue();
  }

  @Test
  void singleOptionalValueTypeProduces0To1Values() {
    ReactiveTypeDescriptor descriptor = ReactiveTypeDescriptor.singleOptionalValue(Optional.class, Optional::empty);
    assertThat(descriptor.isMultiValue()).isFalse();
    assertThat(descriptor.isNoValue()).isFalse();
    assertThat(descriptor.supportsEmpty()).isTrue();
    assertThat(descriptor.getEmptyValue()).isEqualTo(Optional.empty());
  }

  @Test
  void singleRequiredValueTypeMustProduceOneValue() {
    ReactiveTypeDescriptor descriptor = ReactiveTypeDescriptor.singleRequiredValue(String.class);
    assertThat(descriptor.isMultiValue()).isFalse();
    assertThat(descriptor.isNoValue()).isFalse();
    assertThat(descriptor.supportsEmpty()).isFalse();
    assertThatThrownBy(descriptor::getEmptyValue)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Empty values not supported");
  }

  @Test
  void noValueTypeProducesNoValues() {
    ReactiveTypeDescriptor descriptor = ReactiveTypeDescriptor.noValue(Void.class, () -> null);
    assertThat(descriptor.isMultiValue()).isFalse();
    assertThat(descriptor.isNoValue()).isTrue();
    assertThat(descriptor.supportsEmpty()).isTrue();
    assertThat(descriptor.getEmptyValue()).isNull();
  }

  @Test
  void equalsAndHashCodeBasedOnReactiveType() {
    ReactiveTypeDescriptor descriptor1 = ReactiveTypeDescriptor.multiValue(List.class, ArrayList::new);
    ReactiveTypeDescriptor descriptor2 = ReactiveTypeDescriptor.multiValue(List.class, LinkedList::new);
    ReactiveTypeDescriptor descriptor3 = ReactiveTypeDescriptor.multiValue(Set.class, HashSet::new);

    assertThat(descriptor1).isEqualTo(descriptor2);
    assertThat(descriptor1).isNotEqualTo(descriptor3);
    assertThat(descriptor1).isNotEqualTo(null);
    assertThat(descriptor1.hashCode()).isEqualTo(descriptor2.hashCode());
  }

  @Test
  void nullReactiveTypeNotAllowed() {
    assertThatThrownBy(() -> ReactiveTypeDescriptor.multiValue(null, ArrayList::new))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'reactiveType' is required");
  }

  @Test
  void nullEmptySupplierAllowedForSingleRequiredValue() {
    ReactiveTypeDescriptor descriptor = ReactiveTypeDescriptor.singleRequiredValue(String.class);
    assertThat(descriptor.supportsEmpty()).isFalse();
  }

  @Test
  void sameInstanceEqualsItself() {
    ReactiveTypeDescriptor descriptor = ReactiveTypeDescriptor.noValue(Void.class, () -> null);
    assertThat(descriptor).isEqualTo(descriptor);
  }

  @Test
  void differentTypesAreNotEqual() {
    ReactiveTypeDescriptor descriptor1 = ReactiveTypeDescriptor.singleRequiredValue(String.class);
    ReactiveTypeDescriptor descriptor2 = ReactiveTypeDescriptor.singleRequiredValue(Integer.class);
    assertThat(descriptor1).isNotEqualTo(descriptor2);
  }

  @Test
  void nullEmptySupplierGetEmptyValueThrowsException() {
    ReactiveTypeDescriptor descriptor = ReactiveTypeDescriptor.singleRequiredValue(String.class);
    assertThatThrownBy(descriptor::getEmptyValue)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Empty values not supported");
  }

  @Test
  void emptySupplierValuePreserved() {
    Object empty = new Object();
    ReactiveTypeDescriptor descriptor = ReactiveTypeDescriptor.noValue(Object.class, () -> empty);
    assertThat(descriptor.getEmptyValue()).isSameAs(empty);
  }

  @Test
  void differentConstructorsSameTypeAreEqual() {
    ReactiveTypeDescriptor descriptor1 = ReactiveTypeDescriptor.singleOptionalValue(Optional.class, Optional::empty);
    ReactiveTypeDescriptor descriptor2 = ReactiveTypeDescriptor.nonDeferredAsyncValue(Optional.class, Optional::empty);
    assertThat(descriptor1).isEqualTo(descriptor2);
  }

}