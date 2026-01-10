/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation.configuration;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Scope;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 */
public class Spr10744Tests {

  private static int createCount = 0;

  private static int scopeCount = 0;

  @Test
  public void testSpr10744() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
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

  private static class MyTestScope implements infra.beans.factory.config.Scope {

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
