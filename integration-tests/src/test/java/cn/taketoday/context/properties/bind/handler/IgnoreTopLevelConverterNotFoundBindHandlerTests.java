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

package cn.taketoday.context.properties.bind.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MockConfigurationPropertySource;
import cn.taketoday.core.conversion.ConverterNotFoundException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link IgnoreTopLevelConverterNotFoundBindHandler}.
 *
 * @author Madhura Bhave
 */
class IgnoreTopLevelConverterNotFoundBindHandlerTests {

  private List<ConfigurationPropertySource> sources = new ArrayList<>();

  private Binder binder;

  @BeforeEach
  void setup() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("example", "bar");
    this.sources.add(source);
    this.binder = new Binder(this.sources);
  }

  @Test
  void bindWhenHandlerNotPresentShouldFail() {
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind("example", Bindable.of(Example.class)))
            .withCauseInstanceOf(ConverterNotFoundException.class);
  }

  @Test
  void bindWhenTopLevelContextAndExceptionIgnorableShouldNotFail() {
    this.binder.bind("example", Bindable.of(Example.class), new IgnoreTopLevelConverterNotFoundBindHandler());
  }

  @Test
  void bindWhenExceptionNotIgnorableShouldFail() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("example.foo", "1");
    this.sources.add(source);
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind("example", Bindable.of(Example.class),
                    new IgnoreTopLevelConverterNotFoundBindHandler()))
            .withCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void bindWhenExceptionInNestedContextShouldFail() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("example.map", "hello");
    this.sources.add(source);
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind("example", Bindable.of(Example.class),
                    new IgnoreTopLevelConverterNotFoundBindHandler()))
            .withCauseInstanceOf(ConverterNotFoundException.class);
  }

  static class Example {

    private int foo;

    private Map<String, String> map;

    int getFoo() {
      return this.foo;
    }

    void setFoo(int foo) {
      throw new IllegalStateException();
    }

    Map<String, String> getMap() {
      return this.map;
    }

    void setMap(Map<String, String> map) {
      this.map = map;
    }

  }

}
