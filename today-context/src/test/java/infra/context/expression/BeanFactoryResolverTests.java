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

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.expression.AccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 15:35
 */
class BeanFactoryResolverTests {

  @Test
  void constructorThrowsExceptionForNullBeanFactory() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new BeanFactoryResolver(null))
            .withMessage("BeanFactory is required");
  }

  @Test
  void resolvesExistingBean() throws AccessException {
    BeanFactory beanFactory = mock(BeanFactory.class);
    Object expectedBean = new Object();
    when(beanFactory.getBean("testBean")).thenReturn(expectedBean);

    BeanFactoryResolver resolver = new BeanFactoryResolver(beanFactory);
    Object resolvedBean = resolver.resolve(null, "testBean");

    assertThat(resolvedBean).isSameAs(expectedBean);
  }

  @Test
  void throwsAccessExceptionWhenBeanNotFound() {
    BeanFactory beanFactory = mock(BeanFactory.class);
    when(beanFactory.getBean("nonExistingBean"))
            .thenThrow(new BeansException("Bean not found"));

    BeanFactoryResolver resolver = new BeanFactoryResolver(beanFactory);

    assertThatExceptionOfType(AccessException.class)
            .isThrownBy(() -> resolver.resolve(null, "nonExistingBean"))
            .withMessage("Could not resolve bean reference against BeanFactory")
            .withCauseInstanceOf(BeansException.class);
  }

  @Test
  void resolvesWithNullEvaluationContext() throws AccessException {
    BeanFactory beanFactory = mock(BeanFactory.class);
    Object expectedBean = new Object();
    when(beanFactory.getBean("testBean")).thenReturn(expectedBean);

    BeanFactoryResolver resolver = new BeanFactoryResolver(beanFactory);
    Object resolvedBean = resolver.resolve(null, "testBean");

    assertThat(resolvedBean).isSameAs(expectedBean);
  }
}