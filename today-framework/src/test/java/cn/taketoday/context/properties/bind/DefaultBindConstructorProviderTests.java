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

package cn.taketoday.context.properties.bind;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/14 23:21
 */
class DefaultBindConstructorProviderTests {

  private final DefaultBindConstructorProvider provider = new DefaultBindConstructorProvider();

  @Test
  void getBindConstructorWhenHasOnlyDefaultConstructorReturnsNull() {
    Constructor<?> constructor = this.provider.getBindConstructor(OnlyDefaultConstructor.class, false);
    assertThat(constructor).isNull();
  }

  @Test
  void getBindConstructorWhenHasMultipleAmbiguousConstructorsReturnsNull() {
    Constructor<?> constructor = this.provider.getBindConstructor(MultipleAmbiguousConstructors.class, false);
    assertThat(constructor).isNull();
  }

  @Test
  void getBindConstructorWhenHasTwoConstructorsWithOneConstructorBindingReturnsConstructor() {
    Constructor<?> constructor = this.provider.getBindConstructor(TwoConstructorsWithOneConstructorBinding.class,
            false);
    assertThat(constructor).isNotNull();
    assertThat(constructor.getParameterCount()).isOne();
  }

  @Test
  void getBindConstructorWhenHasOneConstructorWithAutowiredReturnsNull() {
    Constructor<?> constructor = this.provider.getBindConstructor(OneConstructorWithAutowired.class, false);
    assertThat(constructor).isNull();
  }

  @Test
  void getBindConstructorWhenHasTwoConstructorsWithOneAutowiredReturnsNull() {
    Constructor<?> constructor = this.provider.getBindConstructor(TwoConstructorsWithOneAutowired.class, false);
    assertThat(constructor).isNull();
  }

  @Test
  void getBindConstructorWhenHasTwoConstructorsWithOneAutowiredAndOneConstructorBindingThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.provider
                    .getBindConstructor(TwoConstructorsWithOneAutowiredAndOneConstructorBinding.class, false))
            .withMessageContaining("declares @ConstructorBinding and @Autowired");
  }

  @Test
  void getBindConstructorWhenHasOneConstructorWithConstructorBindingReturnsConstructor() {
    Constructor<?> constructor = this.provider.getBindConstructor(OneConstructorWithConstructorBinding.class,
            false);
    assertThat(constructor).isNotNull();
  }

  @Test
  void getBindConstructorWhenHasTwoConstructorsWithBothConstructorBindingThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.provider.getBindConstructor(TwoConstructorsWithBothConstructorBinding.class, false))
            .withMessageContaining("has more than one @ConstructorBinding");
  }

  @Test
  void getBindConstructorWhenIsMemberTypeWithPrivateConstructorReturnsNull() {
    Constructor<?> constructor = this.provider.getBindConstructor(MemberTypeWithPrivateConstructor.Member.class,
            false);
    assertThat(constructor).isNull();
  }

  @Test
  void getBindConstructorFromProxiedClassWithOneAutowiredConstructorReturnsNull() {
    try (var context = new AnnotationConfigApplicationContext(ProxiedWithOneConstructorWithAutowired.class)) {
      ProxiedWithOneConstructorWithAutowired bean = context.getBean(ProxiedWithOneConstructorWithAutowired.class);
      Constructor<?> bindConstructor = this.provider.getBindConstructor(bean.getClass(), false);
      assertThat(bindConstructor).isNull();
    }
  }

  @Test
  void getBindConstructorWhenHasExistingValueAndOneConstructorWithoutAnnotationsReturnsNull() {
    OneConstructorWithoutAnnotations existingValue = new OneConstructorWithoutAnnotations("name", 123);
    Bindable<?> bindable = Bindable.of(OneConstructorWithoutAnnotations.class).withExistingValue(existingValue);
    Constructor<?> bindConstructor = this.provider.getBindConstructor(bindable, false);
    assertThat(bindConstructor).isNull();
  }

  @Test
  void getBindConstructorWhenHasExistingValueAndOneConstructorWithConstructorBindingReturnsConstructor() {
    OneConstructorWithConstructorBinding existingValue = new OneConstructorWithConstructorBinding("name", 123);
    Bindable<?> bindable = Bindable.of(OneConstructorWithConstructorBinding.class).withExistingValue(existingValue);
    Constructor<?> bindConstructor = this.provider.getBindConstructor(bindable, false);
    assertThat(bindConstructor).isNotNull();
  }

  @Test
  void getBindConstructorWhenHasExistingValueAndValueIsRecordReturnsConstructor() {
    OneConstructorOnRecord existingValue = new OneConstructorOnRecord("name", 123);
    Bindable<?> bindable = Bindable.of(OneConstructorOnRecord.class).withExistingValue(existingValue);
    Constructor<?> bindConstructor = this.provider.getBindConstructor(bindable, false);
    assertThat(bindConstructor).isNotNull();
  }

  static class OnlyDefaultConstructor {

  }

  static class MultipleAmbiguousConstructors {

    MultipleAmbiguousConstructors() {
    }

    MultipleAmbiguousConstructors(String name) {
    }

  }

  static class TwoConstructorsWithOneConstructorBinding {

    @ConstructorBinding
    TwoConstructorsWithOneConstructorBinding(String name) {
      this(name, 100);
    }

    TwoConstructorsWithOneConstructorBinding(String name, int age) {
    }

  }

  static class OneConstructorWithAutowired {

    @Autowired
    OneConstructorWithAutowired(String name, int age) {
    }

  }

  static class TwoConstructorsWithOneAutowired {

    @Autowired
    TwoConstructorsWithOneAutowired(String name) {
      this(name, 100);
    }

    TwoConstructorsWithOneAutowired(String name, int age) {
    }

  }

  static class TwoConstructorsWithOneAutowiredAndOneConstructorBinding {

    @Autowired
    TwoConstructorsWithOneAutowiredAndOneConstructorBinding(String name) {
      this(name, 100);
    }

    @ConstructorBinding
    TwoConstructorsWithOneAutowiredAndOneConstructorBinding(String name, int age) {
    }

  }

  static class OneConstructorWithConstructorBinding {

    @ConstructorBinding
    OneConstructorWithConstructorBinding(String name, int age) {
    }

  }

  static class OneConstructorWithoutAnnotations {

    OneConstructorWithoutAnnotations(String name, int age) {
    }

  }

  static record OneConstructorOnRecord(String name, int age) {

  }

  static class TwoConstructorsWithBothConstructorBinding {

    @ConstructorBinding
    TwoConstructorsWithBothConstructorBinding(String name) {
      this(name, 100);
    }

    @ConstructorBinding
    TwoConstructorsWithBothConstructorBinding(String name, int age) {
    }

  }

  static class MemberTypeWithPrivateConstructor {

    static final class Member {

      private Member(String name) {
      }

    }

  }

  @Configuration
  static class ProxiedWithOneConstructorWithAutowired {

    @Autowired
    ProxiedWithOneConstructorWithAutowired(Environment environment) {
    }

  }

}