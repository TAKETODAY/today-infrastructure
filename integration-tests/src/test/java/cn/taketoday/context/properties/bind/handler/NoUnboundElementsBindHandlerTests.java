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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.BindHandler;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MockConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link NoUnboundElementsBindHandler}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class NoUnboundElementsBindHandlerTests {

  private List<ConfigurationPropertySource> sources = new ArrayList<>();

  private Binder binder;

  @Test
  void bindWhenNotUsingNoUnboundElementsHandlerShouldBind() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("example.foo", "bar");
    source.put("example.baz", "bar");
    this.sources.add(source);
    this.binder = new Binder(this.sources);
    Example bound = this.binder.bind(ConfigurationPropertyName.of("example"), Bindable.of(Example.class)).get();
    assertThat(bound.getFoo()).isEqualTo("bar");
  }

  @Test
  void bindWhenUsingNoUnboundElementsHandlerShouldBind() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("example.foo", "bar");
    this.sources.add(source);
    this.binder = new Binder(this.sources);
    Example bound = this.binder.bind("example", Bindable.of(Example.class), new NoUnboundElementsBindHandler())
            .get();
    assertThat(bound.getFoo()).isEqualTo("bar");
  }

  @Test
  void bindWhenUsingNoUnboundElementsHandlerThrowException() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("example.foo", "bar");
    source.put("example.baz", "bar");
    this.sources.add(source);
    this.binder = new Binder(this.sources);
    assertThatExceptionOfType(BindException.class).isThrownBy(
                    () -> this.binder.bind("example", Bindable.of(Example.class), new NoUnboundElementsBindHandler()))
            .satisfies((ex) -> assertThat(ex.getCause().getMessage())
                    .contains("The elements [example.baz] were left unbound"));
  }

  @Test
  void bindWhenUsingNoUnboundElementsHandlerShouldBindIfPrefixDifferent() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("example.foo", "bar");
    source.put("other.baz", "bar");
    this.sources.add(source);
    this.binder = new Binder(this.sources);
    Example bound = this.binder.bind("example", Bindable.of(Example.class), new NoUnboundElementsBindHandler())
            .get();
    assertThat(bound.getFoo()).isEqualTo("bar");
  }

  @Test
  void bindWhenUsingNoUnboundElementsHandlerShouldBindIfUnboundSystemProperties() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("example.foo", "bar");
    source.put("example.other", "baz");
    this.sources.add(source);
    this.binder = new Binder(this.sources);
    NoUnboundElementsBindHandler handler = new NoUnboundElementsBindHandler(BindHandler.DEFAULT,
            ((configurationPropertySource) -> false));
    Example bound = this.binder.bind("example", Bindable.of(Example.class), handler).get();
    assertThat(bound.getFoo()).isEqualTo("bar");
  }

  @Test
  void bindWhenUsingNoUnboundElementsHandlerShouldBindIfUnboundCollectionProperties() {
    MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
    source1.put("example.foo[0]", "bar");
    MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
    source2.put("example.foo[0]", "bar");
    source2.put("example.foo[1]", "baz");
    this.sources.add(source1);
    this.sources.add(source2);
    this.binder = new Binder(this.sources);
    NoUnboundElementsBindHandler handler = new NoUnboundElementsBindHandler();
    ExampleWithList bound = this.binder.bind("example", Bindable.of(ExampleWithList.class), handler).get();
    assertThat(bound.getFoo()).containsExactly("bar");
  }

  @Test
  void bindWhenUsingNoUnboundElementsHandlerAndUnboundListElementsShouldThrowException() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("example.foo[0]", "bar");
    this.sources.add(source);
    this.binder = new Binder(this.sources);
    assertThatExceptionOfType(BindException.class).isThrownBy(
                    () -> this.binder.bind("example", Bindable.of(Example.class), new NoUnboundElementsBindHandler()))
            .satisfies((ex) -> assertThat(ex.getCause().getMessage())
                    .contains("The elements [example.foo[0]] were left unbound"));
  }

  @Test
  void bindWhenUsingNoUnboundElementsHandlerShouldBindIfUnboundNestedCollectionProperties() {
    MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
    source1.put("example.nested[0].string-value", "bar");
    MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
    source2.put("example.nested[0].string-value", "bar");
    source2.put("example.nested[0].int-value", "2");
    source2.put("example.nested[1].string-value", "baz");
    source2.put("example.nested[1].other-nested.baz", "baz");
    this.sources.add(source1);
    this.sources.add(source2);
    this.binder = new Binder(this.sources);
    NoUnboundElementsBindHandler handler = new NoUnboundElementsBindHandler();
    ExampleWithNestedList bound = this.binder.bind("example", Bindable.of(ExampleWithNestedList.class), handler)
            .get();
    assertThat(bound.getNested().get(0).getStringValue()).isEqualTo("bar");
  }

  @Test
  void bindWhenUsingNoUnboundElementsHandlerAndUnboundCollectionElementsWithInvalidPropertyShouldThrowException() {
    MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
    source1.put("example.nested[0].string-value", "bar");
    MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
    source2.put("example.nested[0].string-value", "bar");
    source2.put("example.nested[1].int-value", "1");
    source2.put("example.nested[1].invalid", "baz");
    this.sources.add(source1);
    this.sources.add(source2);
    this.binder = new Binder(this.sources);
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind("example", Bindable.of(ExampleWithNestedList.class),
                    new NoUnboundElementsBindHandler()))
            .satisfies((ex) -> assertThat(ex.getCause().getMessage())
                    .contains("The elements [example.nested[1].invalid] were left unbound"));
  }

  static class Example {

    private String foo;

    String getFoo() {
      return this.foo;
    }

    void setFoo(String foo) {
      this.foo = foo;
    }

  }

  static class ExampleWithList {

    private List<String> foo;

    List<String> getFoo() {
      return this.foo;
    }

    void setFoo(List<String> foo) {
      this.foo = foo;
    }

  }

  static class ExampleWithNestedList {

    private List<Nested> nested;

    List<Nested> getNested() {
      return this.nested;
    }

    void setNested(List<Nested> nested) {
      this.nested = nested;
    }

  }

  static class Nested {

    private String stringValue;

    private Integer intValue;

    private OtherNested otherNested;

    String getStringValue() {
      return this.stringValue;
    }

    void setStringValue(String value) {
      this.stringValue = value;
    }

    Integer getIntValue() {
      return this.intValue;
    }

    void setIntValue(Integer intValue) {
      this.intValue = intValue;
    }

    OtherNested getOtherNested() {
      return this.otherNested;
    }

    void setOtherNested(OtherNested otherNested) {
      this.otherNested = otherNested;
    }

  }

  static class OtherNested {

    private String baz;

    String getBaz() {
      return this.baz;
    }

    void setBaz(String baz) {
      this.baz = baz;
    }

  }

}
