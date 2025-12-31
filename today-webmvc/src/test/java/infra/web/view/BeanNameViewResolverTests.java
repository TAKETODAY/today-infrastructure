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