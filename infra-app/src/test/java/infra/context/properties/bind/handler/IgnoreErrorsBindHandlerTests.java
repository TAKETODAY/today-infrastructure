/*
 * Copyright 2012-present the original author or authors.
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
