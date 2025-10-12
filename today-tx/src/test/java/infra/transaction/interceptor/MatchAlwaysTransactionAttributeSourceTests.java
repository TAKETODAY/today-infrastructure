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