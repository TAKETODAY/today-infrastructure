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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 22:24
 */
class CompositeTransactionAttributeSourceTests {

  @Test
  void constructorWithNullSourcesThrowsException() {
    assertThatThrownBy(() -> new CompositeTransactionAttributeSource(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("TransactionAttributeSource array is required");
  }

  @Test
  void constructorWithValidSources() {
    TransactionAttributeSource source1 = new MatchAlwaysTransactionAttributeSource();
    TransactionAttributeSource source2 = new MatchAlwaysTransactionAttributeSource();

    CompositeTransactionAttributeSource compositeSource = new CompositeTransactionAttributeSource(source1, source2);

    assertThat(compositeSource).isNotNull();
    assertThat(compositeSource.getTransactionAttributeSources()).hasSize(2);
    assertThat(compositeSource.getTransactionAttributeSources()[0]).isEqualTo(source1);
    assertThat(compositeSource.getTransactionAttributeSources()[1]).isEqualTo(source2);
  }

  @Test
  void getTransactionAttributeSourcesReturnsArray() {
    TransactionAttributeSource source1 = new MatchAlwaysTransactionAttributeSource();
    TransactionAttributeSource source2 = new MatchAlwaysTransactionAttributeSource();
    CompositeTransactionAttributeSource compositeSource = new CompositeTransactionAttributeSource(source1, source2);

    TransactionAttributeSource[] sources = compositeSource.getTransactionAttributeSources();

    assertThat(sources).isNotNull();
    assertThat(sources).hasSize(2);
    assertThat(sources[0]).isEqualTo(source1);
    assertThat(sources[1]).isEqualTo(source2);
  }

  @Test
  void isCandidateClassReturnsTrueWhenAnySourceReturnsTrue() {
    TransactionAttributeSource source1 = new TestTransactionAttributeSource(false, false, null);
    TransactionAttributeSource source2 = new TestTransactionAttributeSource(true, false, null);
    CompositeTransactionAttributeSource compositeSource = new CompositeTransactionAttributeSource(source1, source2);

    boolean result = compositeSource.isCandidateClass(TestClass.class);

    assertThat(result).isTrue();
  }

  @Test
  void isCandidateClassReturnsFalseWhenAllSourcesReturnFalse() {
    TransactionAttributeSource source1 = new TestTransactionAttributeSource(false, false, null);
    TransactionAttributeSource source2 = new TestTransactionAttributeSource(false, false, null);
    CompositeTransactionAttributeSource compositeSource = new CompositeTransactionAttributeSource(source1, source2);

    boolean result = compositeSource.isCandidateClass(TestClass.class);

    assertThat(result).isFalse();
  }

  @Test
  void hasTransactionAttributeReturnsTrueWhenAnySourceReturnsTrue() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("testMethod");
    TransactionAttributeSource source1 = new TestTransactionAttributeSource(false, false, null);
    TransactionAttributeSource source2 = new TestTransactionAttributeSource(false, true, null);
    CompositeTransactionAttributeSource compositeSource = new CompositeTransactionAttributeSource(source1, source2);

    boolean result = compositeSource.hasTransactionAttribute(method, TestClass.class);

    assertThat(result).isTrue();
  }

  @Test
  void hasTransactionAttributeReturnsFalseWhenAllSourcesReturnFalse() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("testMethod");
    TransactionAttributeSource source1 = new TestTransactionAttributeSource(false, false, null);
    TransactionAttributeSource source2 = new TestTransactionAttributeSource(false, false, null);
    CompositeTransactionAttributeSource compositeSource = new CompositeTransactionAttributeSource(source1, source2);

    boolean result = compositeSource.hasTransactionAttribute(method, TestClass.class);

    assertThat(result).isFalse();
  }

  @Test
  void getTransactionAttributeReturnsAttributeWhenAnySourceReturnsAttribute() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("testMethod");
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    TransactionAttributeSource source1 = new TestTransactionAttributeSource(false, false, null);
    TransactionAttributeSource source2 = new TestTransactionAttributeSource(false, false, attribute);
    CompositeTransactionAttributeSource compositeSource = new CompositeTransactionAttributeSource(source1, source2);

    TransactionAttribute result = compositeSource.getTransactionAttribute(method, TestClass.class);

    assertThat(result).isEqualTo(attribute);
  }

  @Test
  void getTransactionAttributeReturnsNullWhenAllSourcesReturnNull() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("testMethod");
    TransactionAttributeSource source1 = new TestTransactionAttributeSource(false, false, null);
    TransactionAttributeSource source2 = new TestTransactionAttributeSource(false, false, null);
    CompositeTransactionAttributeSource compositeSource = new CompositeTransactionAttributeSource(source1, source2);

    TransactionAttribute result = compositeSource.getTransactionAttribute(method, TestClass.class);

    assertThat(result).isNull();
  }

  @Test
  void getTransactionAttributeReturnsFirstNonNullAttribute() throws NoSuchMethodException {
    Method method = TestClass.class.getMethod("testMethod");
    DefaultTransactionAttribute attribute1 = new DefaultTransactionAttribute();
    DefaultTransactionAttribute attribute2 = new DefaultTransactionAttribute();
    TransactionAttributeSource source1 = new TestTransactionAttributeSource(false, false, attribute1);
    TransactionAttributeSource source2 = new TestTransactionAttributeSource(false, false, attribute2);
    CompositeTransactionAttributeSource compositeSource = new CompositeTransactionAttributeSource(source1, source2);

    TransactionAttribute result = compositeSource.getTransactionAttribute(method, TestClass.class);

    assertThat(result).isEqualTo(attribute1);
  }

  static class TestClass {
    public void testMethod() {
    }
  }

  static class TestTransactionAttributeSource implements TransactionAttributeSource {
    private final boolean candidateClass;
    private final boolean hasAttribute;
    private final TransactionAttribute attribute;

    public TestTransactionAttributeSource(boolean candidateClass, boolean hasAttribute, TransactionAttribute attribute) {
      this.candidateClass = candidateClass;
      this.hasAttribute = hasAttribute;
      this.attribute = attribute;
    }

    @Override
    public boolean isCandidateClass(Class<?> targetClass) {
      return candidateClass;
    }

    @Override
    public boolean hasTransactionAttribute(Method method, Class<?> targetClass) {
      return hasAttribute;
    }

    @Override
    public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
      return attribute;
    }
  }

}