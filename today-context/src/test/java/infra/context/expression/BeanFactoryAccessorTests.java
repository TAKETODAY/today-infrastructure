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

package infra.context.expression;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanFactory;
import infra.expression.AccessException;
import infra.expression.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 11:46
 */
class BeanFactoryAccessorTests {

  @Test
  void specificTargetClassesReturnsBeanFactory() {
    BeanFactoryAccessor accessor = new BeanFactoryAccessor();
    assertThat(accessor.getSpecificTargetClasses()).contains(BeanFactory.class);
  }

  @Test
  void cannotWriteBeans() throws AccessException {
    BeanFactoryAccessor accessor = new BeanFactoryAccessor();
    BeanFactory beanFactory = mock(BeanFactory.class);

    assertThat(accessor.canWrite(null, beanFactory, "testBean")).isFalse();
  }

  @Test
  void writeThrowsAccessException() {
    BeanFactoryAccessor accessor = new BeanFactoryAccessor();
    BeanFactory beanFactory = mock(BeanFactory.class);

    assertThatExceptionOfType(AccessException.class)
            .isThrownBy(() -> accessor.write(null, beanFactory, "testBean", new Object()))
            .withMessage("Beans in a BeanFactory are read-only");
  }

  @Test
  void canReadExistingBean() throws AccessException {
    BeanFactoryAccessor accessor = new BeanFactoryAccessor();
    BeanFactory beanFactory = mock(BeanFactory.class);
    when(beanFactory.containsBean("testBean")).thenReturn(true);

    assertThat(accessor.canRead(null, beanFactory, "testBean")).isTrue();
  }

  @Test
  void cannotReadNonExistentBean() throws AccessException {
    BeanFactoryAccessor accessor = new BeanFactoryAccessor();
    BeanFactory beanFactory = mock(BeanFactory.class);
    when(beanFactory.containsBean("testBean")).thenReturn(false);

    assertThat(accessor.canRead(null, beanFactory, "testBean")).isFalse();
  }

  @Test
  void readReturnsExistingBean() throws AccessException {
    BeanFactoryAccessor accessor = new BeanFactoryAccessor();
    BeanFactory beanFactory = mock(BeanFactory.class);
    Object bean = new Object();
    when(beanFactory.getBean("testBean")).thenReturn(bean);

    TypedValue result = accessor.read(null, beanFactory, "testBean");
    assertThat(result.getValue()).isSameAs(bean);
  }

  @Test
  void readWithNonBeanFactoryTargetThrowsException() {
    BeanFactoryAccessor accessor = new BeanFactoryAccessor();
    Object nonBeanFactory = new Object();

    assertThatIllegalStateException()
            .isThrownBy(() -> accessor.read(null, nonBeanFactory, "testBean"))
            .withMessage("Target must be of type BeanFactory");
  }

  @Test
  void canReadWithNonBeanFactoryTargetReturnsFalse() throws AccessException {
    BeanFactoryAccessor accessor = new BeanFactoryAccessor();
    Object nonBeanFactory = new Object();

    assertThat(accessor.canRead(null, nonBeanFactory, "testBean")).isFalse();
  }

  @Test
  void canWriteWithNonBeanFactoryTargetReturnsFalse() throws AccessException {
    BeanFactoryAccessor accessor = new BeanFactoryAccessor();
    Object nonBeanFactory = new Object();

    assertThat(accessor.canWrite(null, nonBeanFactory, "testBean")).isFalse();
  }

}