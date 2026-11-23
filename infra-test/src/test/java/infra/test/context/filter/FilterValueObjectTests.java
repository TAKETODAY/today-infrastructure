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

package infra.test.context.filter;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

class FilterValueObjectTests {

  @Test
  void testFiltersUseExactTypeEqualityAndRegistrationIsIdempotent() {
    TestTypeExcludeFilter filter = new TestTypeExcludeFilter();
    assertThat(filter).isEqualTo(filter).isEqualTo(new TestTypeExcludeFilter())
            .hasSameHashCodeAs(new TestTypeExcludeFilter()).isNotEqualTo(null).isNotEqualTo(new Object());
    StandardBeanFactory factory = new StandardBeanFactory();
    TestTypeExcludeFilter.registerWith(factory);
    TestTypeExcludeFilter.registerWith(factory);
    assertThat(factory.getSingletonCount()).isEqualTo(1);
  }

  @Test
  void contextCustomizersHaveStableTypeBasedEquality() {
    ExcludeFilterContextCustomizer customizer = new ExcludeFilterContextCustomizer();
    assertThat(customizer).isEqualTo(new ExcludeFilterContextCustomizer())
            .hasSameHashCodeAs(new ExcludeFilterContextCustomizer()).isNotEqualTo(null).isNotEqualTo(new Object());
  }

}
