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

package infra.app.context.config;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import infra.app.context.config.FilteredPropertySource;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FilteredPropertySource}.
 *
 * @author Phillip Webb
 */
class FilteredPropertySourceTests {

  @Test
  void applyWhenHasNoSourceShouldRunOperation() {
    ConfigurableEnvironment environment = new MockEnvironment();
    TestOperation operation = new TestOperation();
    FilteredPropertySource.apply(environment, "test", Collections.emptySet(), operation);
    assertThat(operation.isCalled()).isTrue();
    assertThat(operation.getOriginal()).isNull();
  }

  @Test
  void applyWhenHasSourceShouldRunWithReplacedSource() {
    ConfigurableEnvironment environment = new MockEnvironment();
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("regular", "regularValue");
    map.put("filtered", "filteredValue");
    PropertySource<?> propertySource = new MapPropertySource("test", map);
    environment.getPropertySources().addFirst(propertySource);
    TestOperation operation = new TestOperation(() -> {
      assertThat(environment.containsProperty("regular")).isTrue();
      assertThat(environment.containsProperty("filtered")).isFalse();
    });
    FilteredPropertySource.apply(environment, "test", Collections.singleton("filtered"), operation);
    assertThat(operation.isCalled()).isTrue();
    assertThat(operation.getOriginal()).isSameAs(propertySource);
    assertThat(environment.getPropertySources().get("test")).isSameAs(propertySource);

  }

  static class TestOperation implements Consumer<PropertySource<?>> {

    private boolean called;

    private PropertySource<?> original;

    private Runnable operation;

    TestOperation() {
      this(null);
    }

    TestOperation(Runnable operation) {
      this.operation = operation;
    }

    @Override
    public void accept(PropertySource<?> original) {
      this.called = true;
      this.original = original;
      if (this.operation != null) {
        this.operation.run();
      }
    }

    boolean isCalled() {
      return this.called;
    }

    PropertySource<?> getOriginal() {
      return this.original;
    }

  }

}
