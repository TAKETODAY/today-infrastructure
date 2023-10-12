/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.stereotype.Component;
import cn.taketoday.scheduling.annotation.Async;
import cn.taketoday.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class Spr12636Tests {

  private ConfigurableApplicationContext context;

  @AfterEach
  public void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  public void orderOnImplementation() {
    this.context = new AnnotationConfigApplicationContext(
            UserServiceTwo.class, UserServiceOne.class, UserServiceCollector.class);
    UserServiceCollector bean = this.context.getBean(UserServiceCollector.class);
    assertThat(bean.userServices.get(0)).isSameAs(context.getBean("serviceOne", UserService.class));
    assertThat(bean.userServices.get(1)).isSameAs(context.getBean("serviceTwo", UserService.class));

  }

  @Test
  public void orderOnImplementationWithProxy() {
    this.context = new AnnotationConfigApplicationContext(
            UserServiceTwo.class, UserServiceOne.class, UserServiceCollector.class, AsyncConfig.class);

    // Validate those beans are indeed wrapped by a proxy
    UserService serviceOne = this.context.getBean("serviceOne", UserService.class);
    UserService serviceTwo = this.context.getBean("serviceTwo", UserService.class);
    assertThat(AopUtils.isAopProxy(serviceOne)).isTrue();
    assertThat(AopUtils.isAopProxy(serviceTwo)).isTrue();

    UserServiceCollector bean = this.context.getBean(UserServiceCollector.class);
    assertThat(bean.userServices.get(0)).isSameAs(serviceOne);
    assertThat(bean.userServices.get(1)).isSameAs(serviceTwo);
  }

  @Configuration
  @EnableAsync
  static class AsyncConfig {
  }

  @Component
  static class UserServiceCollector {

    public final List<UserService> userServices;

    @Autowired
    UserServiceCollector(List<UserService> userServices) {
      this.userServices = userServices;
    }
  }

  interface UserService {

    void doIt();
  }

  @Component("serviceOne")
  @Order(1)
  static class UserServiceOne implements UserService {

    @Async
    @Override
    public void doIt() {

    }
  }

  @Component("serviceTwo")
  @Order(2)
  static class UserServiceTwo implements UserService {

    @Async
    @Override
    public void doIt() {

    }
  }
}
