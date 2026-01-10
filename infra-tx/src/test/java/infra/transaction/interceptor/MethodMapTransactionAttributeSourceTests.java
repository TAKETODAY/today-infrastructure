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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 22:26
 */
class MethodMapTransactionAttributeSourceTests {

  @Test
  void defaultConstructorCreatesInstance() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    assertThat(source).isNotNull();
  }

  @Test
  void addTransactionalMethodWithStringName() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    TransactionAttribute attr = new DefaultTransactionAttribute();

    assertThatCode(() -> source.addTransactionalMethod("java.lang.String.valueOf", attr))
            .doesNotThrowAnyException();
  }

  @Test
  void addTransactionalMethodWithInvalidNameThrowsException() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    TransactionAttribute attr = new DefaultTransactionAttribute();

    assertThatThrownBy(() -> source.addTransactionalMethod("invalidName", attr))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addTransactionalMethodWithClassAndMethodName() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    TransactionAttribute attr = new DefaultTransactionAttribute();

    assertThatCode(() -> source.addTransactionalMethod(String.class, "valueOf", attr))
            .doesNotThrowAnyException();
  }

  @Test
  void addTransactionalMethodWithNonExistentMethodThrowsException() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    TransactionAttribute attr = new DefaultTransactionAttribute();

    assertThatThrownBy(() -> source.addTransactionalMethod(String.class, "nonExistentMethod", attr))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addTransactionalMethodWithMethodObject() throws NoSuchMethodException {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    TransactionAttribute attr = new DefaultTransactionAttribute();
    Method method = String.class.getMethod("valueOf", Object.class);

    assertThatCode(() -> source.addTransactionalMethod(method, attr)).doesNotThrowAnyException();
  }

  @Test
  void getTransactionAttributeReturnsAttribute() throws NoSuchMethodException {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    DefaultTransactionAttribute attr = new DefaultTransactionAttribute();
    Method method = String.class.getMethod("valueOf", Object.class);

    source.addTransactionalMethod(method, attr);
    source.afterPropertiesSet();

    TransactionAttribute result = source.getTransactionAttribute(method, String.class);
    assertThat(result).isEqualTo(attr);
  }

  @Test
  void getTransactionAttributeReturnsNullForUnmappedMethod() throws NoSuchMethodException {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    Method method = String.class.getMethod("toString");
    source.afterPropertiesSet();

    TransactionAttribute result = source.getTransactionAttribute(method, String.class);
    assertThat(result).isNull();
  }

  @Test
  void isMatchWithExactName() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    assertThat(source.isMatch("testMethod", "testMethod")).isTrue();
  }

  @Test
  void isMatchWithWildcardPattern() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    assertThat(source.isMatch("testMethod", "test*")).isTrue();
    assertThat(source.isMatch("testMethod", "*Method")).isTrue();
    assertThat(source.isMatch("testMethod", "*estMet*")).isTrue();
  }

  @Test
  void equalsWithSameInstance() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    assertThat(source.equals(source)).isTrue();
  }

  @Test
  void equalsWithDifferentType() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    assertThat(source.equals("different")).isFalse();
  }

  @Test
  void equalsWithNull() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    assertThat(source.equals(null)).isFalse();
  }

  @Test
  void hashCodeReturnsClassHashCode() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    assertThat(source.hashCode()).isEqualTo(MethodMapTransactionAttributeSource.class.hashCode());
  }

  @Test
  void toStringReturnsFormattedString() {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    assertThat(source.toString()).startsWith(MethodMapTransactionAttributeSource.class.getName());
  }

}