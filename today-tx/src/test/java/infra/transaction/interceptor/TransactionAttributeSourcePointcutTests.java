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

import infra.aop.ClassFilter;
import infra.dao.support.PersistenceExceptionTranslator;
import infra.transaction.TransactionManager;
import infra.transaction.interceptor.TransactionAttributeSourcePointcut.TransactionAttributeSourceClassFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:41
 */
class TransactionAttributeSourcePointcutTests {

  @Test
  void constructorInitializesClassFilter() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();

    assertThat(pointcut.getClassFilter()).isNotNull();
  }

  @Test
  void setTransactionAttributeSourceUpdatesSource() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    TransactionAttributeSource source = mock(TransactionAttributeSource.class);

    pointcut.setTransactionAttributeSource(source);

    // We'll verify this indirectly through other tests
  }

  @Test
  void matchesWithNullTransactionAttributeSourceReturnsTrue() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    Method method = mock(Method.class);
    Class<?> targetClass = Object.class;

    boolean result = pointcut.matches(method, targetClass);

    assertThat(result).isTrue();
  }

  @Test
  void matchesDelegatesToTransactionAttributeSource() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    TransactionAttributeSource source = mock(TransactionAttributeSource.class);
    pointcut.setTransactionAttributeSource(source);

    Method method = mock(Method.class);
    Class<?> targetClass = Object.class;

    when(source.hasTransactionAttribute(method, targetClass)).thenReturn(true);

    boolean result = pointcut.matches(method, targetClass);

    assertThat(result).isTrue();
  }

  @Test
  void matchesReturnsFalseWhenTransactionAttributeSourceReturnsFalse() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    TransactionAttributeSource source = mock(TransactionAttributeSource.class);
    pointcut.setTransactionAttributeSource(source);

    Method method = mock(Method.class);
    Class<?> targetClass = Object.class;

    when(source.hasTransactionAttribute(method, targetClass)).thenReturn(false);

    boolean result = pointcut.matches(method, targetClass);

    assertThat(result).isFalse();
  }

  @Test
  void equalsWithSameInstanceReturnsTrue() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();

    boolean result = pointcut.equals(pointcut);

    assertThat(result).isTrue();
  }

  @Test
  void equalsWithDifferentTypeReturnsFalse() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();

    boolean result = pointcut.equals(new Object());

    assertThat(result).isFalse();
  }

  @Test
  void equalsWithEqualTransactionAttributeSourceReturnsTrue() {
    TransactionAttributeSourcePointcut pointcut1 = new TransactionAttributeSourcePointcut();
    TransactionAttributeSource source = mock(TransactionAttributeSource.class);
    pointcut1.setTransactionAttributeSource(source);

    TransactionAttributeSourcePointcut pointcut2 = new TransactionAttributeSourcePointcut();
    pointcut2.setTransactionAttributeSource(source);

    boolean result = pointcut1.equals(pointcut2);

    assertThat(result).isTrue();
  }

  @Test
  void equalsWithDifferentTransactionAttributeSourceReturnsFalse() {
    TransactionAttributeSourcePointcut pointcut1 = new TransactionAttributeSourcePointcut();
    TransactionAttributeSource source1 = mock(TransactionAttributeSource.class);
    pointcut1.setTransactionAttributeSource(source1);

    TransactionAttributeSourcePointcut pointcut2 = new TransactionAttributeSourcePointcut();
    TransactionAttributeSource source2 = mock(TransactionAttributeSource.class);
    pointcut2.setTransactionAttributeSource(source2);

    boolean result = pointcut1.equals(pointcut2);

    assertThat(result).isFalse();
  }

  @Test
  void hashCodeReturnsConstantValue() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();

    int hashCode1 = pointcut.hashCode();
    int hashCode2 = pointcut.hashCode();

    assertThat(hashCode1).isEqualTo(hashCode2);
    assertThat(hashCode1).isEqualTo(TransactionAttributeSourcePointcut.class.hashCode());
  }

  @Test
  void toStringReturnsFormattedString() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    TransactionAttributeSource source = mock(TransactionAttributeSource.class);
    pointcut.setTransactionAttributeSource(source);

    String result = pointcut.toString();

    assertThat(result).isEqualTo("infra.transaction.interceptor.TransactionAttributeSourcePointcut: " + source);
  }

  @Test
  void classFilterMatchesRegularClasses() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();

    boolean result = pointcut.getClassFilter().matches(String.class);

    assertThat(result).isTrue();
  }

  @Test
  void classFilterDoesNotMatchTransactionalProxy() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();

    boolean result = pointcut.getClassFilter().matches(TransactionalProxy.class);

    assertThat(result).isFalse();
  }

  @Test
  void classFilterDoesNotMatchTransactionManager() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();

    boolean result = pointcut.getClassFilter().matches(TransactionManager.class);

    assertThat(result).isFalse();
  }

  @Test
  void classFilterDoesNotMatchPersistenceExceptionTranslator() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();

    boolean result = pointcut.getClassFilter().matches(PersistenceExceptionTranslator.class);

    assertThat(result).isFalse();
  }

  @Test
  void classFilterDelegatesToTransactionAttributeSource() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    TransactionAttributeSource source = mock(TransactionAttributeSource.class);
    pointcut.setTransactionAttributeSource(source);

    Class<?> clazz = Object.class;
    when(source.isCandidateClass(clazz)).thenReturn(true);

    boolean result = pointcut.getClassFilter().matches(clazz);

    assertThat(result).isTrue();
  }

  @Test
  void classFilterReturnsFalseWhenTransactionAttributeSourceReturnsFalse() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    TransactionAttributeSource source = mock(TransactionAttributeSource.class);
    pointcut.setTransactionAttributeSource(source);

    Class<?> clazz = Object.class;
    when(source.isCandidateClass(clazz)).thenReturn(false);

    boolean result = pointcut.getClassFilter().matches(clazz);

    assertThat(result).isFalse();
  }

  @Test
  void classFilterEqualsWithSameInstanceReturnsTrue() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    ClassFilter classFilter = pointcut.getClassFilter();

    boolean result = classFilter.equals(classFilter);

    assertThat(result).isTrue();
  }

  @Test
  void classFilterEqualsWithDifferentTypeReturnsFalse() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    ClassFilter classFilter = pointcut.getClassFilter();

    boolean result = classFilter.equals(new Object());

    assertThat(result).isFalse();
  }

  @Test
  void classFilterToStringReturnsFormattedString() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    TransactionAttributeSource source = mock(TransactionAttributeSource.class);
    pointcut.setTransactionAttributeSource(source);
    ClassFilter classFilter = pointcut.getClassFilter();

    String result = classFilter.toString();

    assertThat(result).isEqualTo("infra.transaction.interceptor.TransactionAttributeSourcePointcut$TransactionAttributeSourceClassFilter: " + source);
  }

  @Test
  void classFilterHashCodeReturnsConstantValue() {
    TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
    ClassFilter classFilter = pointcut.getClassFilter();

    int hashCode1 = classFilter.hashCode();
    int hashCode2 = classFilter.hashCode();

    assertThat(hashCode1).isEqualTo(hashCode2);
    assertThat(hashCode1).isEqualTo(TransactionAttributeSourceClassFilter.class.hashCode());
  }
}