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

package infra.transaction.interceptor;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 22:22
 */
class MatchAlwaysTransactionAttributeSourceTests {

  @Test
  void getTransactionAttributeForUserLevelMethod() throws NoSuchMethodException {
    MatchAlwaysTransactionAttributeSource source = new MatchAlwaysTransactionAttributeSource();
    Method method = TestClass.class.getMethod("testMethod");

    TransactionAttribute attribute = source.getTransactionAttribute(method, TestClass.class);

    assertThat(attribute).isNotNull();
    assertThat(attribute).isInstanceOf(DefaultTransactionAttribute.class);
  }

  @Test
  void equalsWithSameInstance() {
    MatchAlwaysTransactionAttributeSource source = new MatchAlwaysTransactionAttributeSource();

    assertThat(source.equals(source)).isTrue();
  }

  @Test
  void equalsWithDifferentType() {
    MatchAlwaysTransactionAttributeSource source = new MatchAlwaysTransactionAttributeSource();

    assertThat(source.equals("different")).isFalse();
  }

  @Test
  void equalsWithSameTransactionAttribute() {
    MatchAlwaysTransactionAttributeSource source1 = new MatchAlwaysTransactionAttributeSource();
    MatchAlwaysTransactionAttributeSource source2 = new MatchAlwaysTransactionAttributeSource();

    assertThat(source1.equals(source2)).isTrue();
  }

  @Test
  void equalsWithDifferentTransactionAttribute() {
    MatchAlwaysTransactionAttributeSource source1 = new MatchAlwaysTransactionAttributeSource();
    MatchAlwaysTransactionAttributeSource source2 = new MatchAlwaysTransactionAttributeSource();
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    source2.setTransactionAttribute(attribute);

    assertThat(source1.equals(source2)).isFalse();
  }

  @Test
  void hashCodeReturnsClassHashCode() {
    MatchAlwaysTransactionAttributeSource source = new MatchAlwaysTransactionAttributeSource();

    assertThat(source.hashCode()).isEqualTo(MatchAlwaysTransactionAttributeSource.class.hashCode());
  }

  @Test
  void toStringReturnsFormattedString() {
    MatchAlwaysTransactionAttributeSource source = new MatchAlwaysTransactionAttributeSource();

    assertThat(source.toString()).startsWith(MatchAlwaysTransactionAttributeSource.class.getName());
  }

  static class TestClass {
    public void testMethod() {
    }
  }

}