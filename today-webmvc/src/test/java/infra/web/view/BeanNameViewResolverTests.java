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

package infra.web.view;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import infra.context.ApplicationContext;
import infra.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:42
 */
class BeanNameViewResolverTests {

  @Test
  void shouldReturnViewWhenBeanExistsAndImplementsView() {
    // given
    BeanNameViewResolver resolver = new BeanNameViewResolver();
    ApplicationContext context = mock(ApplicationContext.class);
    View mockView = mock(View.class);

    when(context.containsBean("testView")).thenReturn(true);
    when(context.isTypeMatch("testView", View.class)).thenReturn(true);
    when(context.getBean("testView", View.class)).thenReturn(mockView);

    resolver.setApplicationContext(context);

    // when
    View result = resolver.resolveViewName("testView", Locale.getDefault());

    // then
    assertThat(result).isEqualTo(mockView);
  }

  @Test
  void shouldReturnNullWhenBeanDoesNotExist() {
    // given
    BeanNameViewResolver resolver = new BeanNameViewResolver();
    ApplicationContext context = mock(ApplicationContext.class);

    when(context.containsBean("nonExistentView")).thenReturn(false);

    resolver.setApplicationContext(context);

    // when
    View result = resolver.resolveViewName("nonExistentView", Locale.getDefault());

    // then
    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullWhenBeanExistsButDoesNotImplementView() {
    // given
    BeanNameViewResolver resolver = new BeanNameViewResolver();
    ApplicationContext context = mock(ApplicationContext.class);

    when(context.containsBean("notAView")).thenReturn(true);
    when(context.isTypeMatch("notAView", View.class)).thenReturn(false);

    resolver.setApplicationContext(context);

    // when
    View result = resolver.resolveViewName("notAView", Locale.getDefault());

    // then
    assertThat(result).isNull();
  }

  @Test
  void shouldReturnLowestPrecedenceByDefault() {
    // given
    BeanNameViewResolver resolver = new BeanNameViewResolver();

    // when
    int order = resolver.getOrder();

    // then
    assertThat(order).isEqualTo(Ordered.LOWEST_PRECEDENCE);
  }

  @Test
  void shouldSetAndGetOrder() {
    // given
    BeanNameViewResolver resolver = new BeanNameViewResolver();
    int customOrder = 100;

    // when
    resolver.setOrder(customOrder);

    // then
    assertThat(resolver.getOrder()).isEqualTo(customOrder);
  }

}