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