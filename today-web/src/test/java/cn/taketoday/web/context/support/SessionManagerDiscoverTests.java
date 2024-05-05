/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.context.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.session.DefaultSessionManager;
import cn.taketoday.session.InMemorySessionRepository;
import cn.taketoday.session.SecureRandomSessionIdGenerator;
import cn.taketoday.session.SessionEventDispatcher;
import cn.taketoday.web.mock.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/31 09:25
 */
class SessionManagerDiscoverTests {

  @Test
  void illegalArgument() {
    assertThatThrownBy(() ->
            new SessionManagerDiscover(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("beanFactory is required");
  }

  @Test
  void find() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    SessionManagerDiscover discover = new SessionManagerDiscover(beanFactory);

    beanFactory.preInstantiateSingletons();
    assertThat(discover.find()).isNull();
    assertThat(discover.find()).isNull();

    beanFactory.registerSingleton(createSessionManager());

    assertThat(new SessionManagerDiscover(beanFactory).find()).isNotNull();
  }

  @Test
  void obtain() {
    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    applicationContext.refresh();
    ServletRequestContext context = new ServletRequestContext(applicationContext);

    StandardBeanFactory beanFactory = new StandardBeanFactory();
    SessionManagerDiscover discover = new SessionManagerDiscover(beanFactory);

    beanFactory.preInstantiateSingletons();

    assertThatThrownBy(() ->
            discover.obtain(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No SessionManager in context");

    beanFactory.registerSingleton(createSessionManager());

    assertThat(new SessionManagerDiscover(beanFactory).obtain(context)).isNotNull();
  }

  private static DefaultSessionManager createSessionManager() {
    return new DefaultSessionManager(
            new InMemorySessionRepository(new SessionEventDispatcher(),
                    new SecureRandomSessionIdGenerator()), null);
  }

}