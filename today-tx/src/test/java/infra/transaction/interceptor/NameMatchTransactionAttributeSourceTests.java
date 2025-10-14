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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import infra.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 22:30
 */
class NameMatchTransactionAttributeSourceTests {
  @Test
  void defaultConstructorCreatesInstance() {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    assertThat(source).isNotNull();
  }

  @Test
  void setNameMapWithValidMap() {
    Map<String, TransactionAttribute> nameMap = new HashMap<>();
    nameMap.put("testMethod", new DefaultTransactionAttribute());

    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    source.setNameMap(nameMap);

    assertThatCode(() -> source.afterPropertiesSet()).doesNotThrowAnyException();
  }

  @Test
  void setPropertiesWithValidProperties() {
    Properties properties = new Properties();
    properties.setProperty("testMethod", "PROPAGATION_REQUIRED");

    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    source.setProperties(properties);

    assertThatCode(() -> source.afterPropertiesSet()).doesNotThrowAnyException();
  }

  @Test
  void addTransactionalMethod() {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    TransactionAttribute attr = new DefaultTransactionAttribute();

    assertThatCode(() -> source.addTransactionalMethod("testMethod", attr)).doesNotThrowAnyException();
  }

  @Test
  void getTransactionAttributeForExactMatch() throws NoSuchMethodException {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    DefaultTransactionAttribute attr = new DefaultTransactionAttribute();
    Method method = TestClass.class.getMethod("testMethod");

    source.addTransactionalMethod("testMethod", attr);

    TransactionAttribute result = source.getTransactionAttribute(method, TestClass.class);
    assertThat(result).isEqualTo(attr);
  }

  @Test
  void getTransactionAttributeForPatternMatch() throws NoSuchMethodException {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    DefaultTransactionAttribute attr = new DefaultTransactionAttribute();
    Method method = TestClass.class.getMethod("testMethod");

    source.addTransactionalMethod("test*", attr);

    TransactionAttribute result = source.getTransactionAttribute(method, TestClass.class);
    assertThat(result).isEqualTo(attr);
  }

  @Test
  void getTransactionAttributeReturnsNullForNonUserLevelMethod() throws NoSuchMethodException {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    Method method = Object.class.getMethod("toString");

    TransactionAttribute result = source.getTransactionAttribute(method, TestClass.class);
    assertThat(result).isNull();
  }

  @Test
  void getTransactionAttributeReturnsNullForUnmappedMethod() throws NoSuchMethodException {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    Method method = TestClass.class.getMethod("unmappedMethod");

    TransactionAttribute result = source.getTransactionAttribute(method, TestClass.class);
    assertThat(result).isNull();
  }

  @Test
  void isMatchWithExactName() {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    assertThat(source.isMatch("testMethod", "testMethod")).isTrue();
  }

  @Test
  void isMatchWithWildcardPattern() {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    assertThat(source.isMatch("testMethod", "test*")).isTrue();
    assertThat(source.isMatch("testMethod", "*Method")).isTrue();
    assertThat(source.isMatch("testMethod", "*estMet*")).isTrue();
  }

  @Test
  void equalsWithSameInstance() {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    assertThat(source.equals(source)).isTrue();
  }

  @Test
  void equalsWithDifferentType() {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    assertThat(source.equals("different")).isFalse();
  }

  @Test
  void equalsWithNull() {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    assertThat(source.equals(null)).isFalse();
  }

  @Test
  void equalsWithSameNameMap() {
    NameMatchTransactionAttributeSource source1 = new NameMatchTransactionAttributeSource();
    NameMatchTransactionAttributeSource source2 = new NameMatchTransactionAttributeSource();
    DefaultTransactionAttribute attr = new DefaultTransactionAttribute();
    source1.addTransactionalMethod("testMethod", attr);
    source2.addTransactionalMethod("testMethod", attr);

    assertThat(source1.equals(source2)).isTrue();
  }

  @Test
  void equalsWithDifferentNameMap() {
    NameMatchTransactionAttributeSource source1 = new NameMatchTransactionAttributeSource();
    NameMatchTransactionAttributeSource source2 = new NameMatchTransactionAttributeSource();
    DefaultTransactionAttribute attr1 = new DefaultTransactionAttribute();
    DefaultTransactionAttribute attr2 = new DefaultTransactionAttribute();
    attr2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    source1.addTransactionalMethod("testMethod", attr1);
    source2.addTransactionalMethod("testMethod", attr2);

    assertThat(source1.equals(source2)).isFalse();
  }

  @Test
  void hashCodeReturnsClassHashCode() {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    assertThat(source.hashCode()).isEqualTo(NameMatchTransactionAttributeSource.class.hashCode());
  }

  @Test
  void toStringReturnsFormattedString() {
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    assertThat(source.toString()).startsWith(NameMatchTransactionAttributeSource.class.getName());
  }

  static class TestClass {
    public void testMethod() {
    }

    public void unmappedMethod() {
    }
  }

}