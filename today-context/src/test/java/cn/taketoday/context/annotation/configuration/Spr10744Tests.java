/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 */
public class Spr10744Tests {

  private static int createCount = 0;

  private static int scopeCount = 0;

  @Test
  public void testSpr10744() throws Exception {
    StandardApplicationContext context = new StandardApplicationContext();
    context.getBeanFactory().registerScope("myTestScope", new MyTestScope());
    context.register(MyTestConfiguration.class);
    context.refresh();

    Foo bean1 = context.getBean("foo", Foo.class);
    Foo bean2 = context.getBean("foo", Foo.class);
//    assertThat(bean1).isSameAs(bean2); // TODO proxyMode
    assertThat(bean1).isNotSameAs(bean2);

    // Should not have invoked constructor for the proxy instance
    assertThat(createCount).isEqualTo(2);
    assertThat(scopeCount).isEqualTo(2);

    // Proxy mode should create new scoped object on each method call
    bean1.getMessage();
    assertThat(createCount).isEqualTo(2);
    assertThat(scopeCount).isEqualTo(2);
    bean1.getMessage();
    assertThat(createCount).isEqualTo(2);
    assertThat(scopeCount).isEqualTo(2);

    context.close();
  }

  private static class MyTestScope implements cn.taketoday.beans.factory.config.Scope {

    @Override
    public Object get(String name, Supplier<?> objectFactory) {
      scopeCount++;
      return objectFactory.get();
    }

    @Override
    public Object remove(String name) {
      return null;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
    }

    @Nullable
    @Override
    public Object resolveContextualObject(String key) {
      return null;
    }

    @Override
    public String getConversationId() {
      return null;
    }
  }

  static class Foo {

    public Foo() {
      createCount++;
    }

    public String getMessage() {
      return "Hello";
    }
  }

  @Configuration
  static class MyConfiguration {

    @Bean
    public Foo foo() {
      return new Foo();
    }
  }

  @Configuration
  static class MyTestConfiguration extends MyConfiguration {

    @Bean
    @Scope(value = "myTestScope"/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
    @Override
    public Foo foo() {
      return new Foo();
    }
  }

}
