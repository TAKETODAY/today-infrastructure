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

package infra.context.properties.bind.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.context.properties.bind.BindException;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.MockConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link IgnoreErrorsBindHandler}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class IgnoreErrorsBindHandlerTests {

  private List<ConfigurationPropertySource> sources = new ArrayList<>();

  private Binder binder;

  @BeforeEach
  void setup() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("example.foo", "bar");
    this.sources.add(source);
    this.binder = new Binder(this.sources);
  }

  @Test
  void bindWhenNotIgnoringErrorsShouldFail() {
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind("example", Bindable.of(Example.class)));
  }

  @Test
  void bindWhenIgnoringErrorsShouldBind() {
    Example bound = this.binder.bind("example", Bindable.of(Example.class), new IgnoreErrorsBindHandler()).get();
    assertThat(bound.getFoo()).isEqualTo(0);
  }

  static class Example {

    private int foo;

    int getFoo() {
      return this.foo;
    }

    void setFoo(int foo) {
      this.foo = foo;
    }

  }

}
