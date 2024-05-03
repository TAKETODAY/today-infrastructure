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

package cn.taketoday.web.handler;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.lang.Nullable;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/3 22:25
 */
class AbstractUrlHandlerMappingTests {

  private final AbstractUrlHandlerMapping mapping = new AbstractUrlHandlerMapping() { };

  @Test
  void registerRootHandler() {
    TestController rootHandler = new TestController();
    mapping.registerHandler("/", rootHandler);
    assertThat(mapping).satisfies(hasMappings(rootHandler, null, Map.of()));
  }

  @Test
  void registerDefaultHandler() {
    TestController defaultHandler = new TestController();
    mapping.registerHandler("/*", defaultHandler);
    assertThat(mapping).satisfies(hasMappings(null, defaultHandler, Map.of()));
  }

  @Test
  void registerSpecificMapping() {
    TestController testHandler = new TestController();
    mapping.registerHandler("/test", testHandler);
    assertThat(mapping).satisfies(hasMappings(null, null, Map.of("/test", testHandler)));
  }

  @Test
  void registerSpecificMappingWithBeanName() {
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerSingleton("controller", TestController.class);
    mapping.setApplicationContext(context);
    mapping.registerHandler("/test", "controller");
    assertThat(mapping.getHandlerMap().get("/test")).isSameAs(context.getBean("controller"));
  }

  @Test
  void unregisterRootHandler() {
    TestController rootHandler = new TestController();
    TestController defaultHandler = new TestController();
    TestController specificHandler = new TestController();
    mapping.registerHandler("/", rootHandler);
    mapping.registerHandler("/*", defaultHandler);
    mapping.registerHandler("/test", specificHandler);
    assertThat(mapping).satisfies(hasMappings(rootHandler, defaultHandler, Map.of("/test", specificHandler)));

    mapping.unregisterHandler("/");
    assertThat(mapping).satisfies(hasMappings(null, defaultHandler, Map.of("/test", specificHandler)));
  }

  @Test
  void unregisterDefaultHandler() {
    TestController rootHandler = new TestController();
    TestController defaultHandler = new TestController();
    TestController specificHandler = new TestController();
    mapping.registerHandler("/", rootHandler);
    mapping.registerHandler("/*", defaultHandler);
    mapping.registerHandler("/test", specificHandler);
    assertThat(mapping).satisfies(hasMappings(rootHandler, defaultHandler, Map.of("/test", specificHandler)));

    mapping.unregisterHandler("/*");
    assertThat(mapping).satisfies(hasMappings(rootHandler, null, Map.of("/test", specificHandler)));
  }

  @Test
  void unregisterSpecificHandler() {
    TestController rootHandler = new TestController();
    TestController defaultHandler = new TestController();
    TestController specificHandler = new TestController();
    mapping.registerHandler("/", rootHandler);
    mapping.registerHandler("/*", defaultHandler);
    mapping.registerHandler("/test", specificHandler);
    assertThat(mapping).satisfies(hasMappings(rootHandler, defaultHandler, Map.of("/test", specificHandler)));

    mapping.unregisterHandler("/test");
    assertThat(mapping).satisfies(hasMappings(rootHandler, defaultHandler, Map.of()));
  }

  @Test
  void unregisterUnsetRootHandler() {
    assertThatNoException().isThrownBy(() -> mapping.unregisterHandler("/"));
  }

  @Test
  void unregisterUnsetDefaultHandler() {
    assertThatNoException().isThrownBy(() -> mapping.unregisterHandler("/*"));
  }

  @Test
  void unregisterUnknownHandler() {
    TestController specificHandler = new TestController();
    mapping.registerHandler("/test", specificHandler);

    mapping.unregisterHandler("/test/*");
    assertThat(mapping.getHandlerMap()).containsExactly(entry("/test", specificHandler));
  }

  private Consumer<AbstractUrlHandlerMapping> hasMappings(@Nullable Object rootHandler,
          @Nullable Object defaultHandler, Map<String, Object> handlerMap) {
    return actual -> {
      assertThat(actual.getRootHandler()).isEqualTo(rootHandler);
      assertThat(actual.getDefaultHandler()).isEqualTo(defaultHandler);
      assertThat(actual.getHandlerMap()).containsExactlyEntriesOf(handlerMap);
    };
  }

  private static class TestController { }

}