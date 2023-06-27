/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.context.assertj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.framework.test.context.assertj.ApplicationContextAssert.Scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ApplicationContextAssert}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ApplicationContextAssertTests {

  private StaticApplicationContext parent;

  private StaticApplicationContext context;

  private final RuntimeException failure = new RuntimeException();

  @BeforeEach
  void setup() {
    this.parent = new StaticApplicationContext();
    this.context = new StaticApplicationContext();
    this.context.setParent(this.parent);
  }

  @AfterEach
  void cleanup() {
    this.context.close();
    this.parent.close();
  }

  @Test
  void createWhenApplicationContextIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationContextAssert<>(null, null))
            .withMessageContaining("ApplicationContext must not be null");
  }

  @Test
  void createWhenHasApplicationContextShouldSetActual() {
    assertThat(getAssert(this.context).getSourceApplicationContext()).isSameAs(this.context);
  }

  @Test
  void createWhenHasExceptionShouldSetFailure() {
    assertThat(getAssert(this.failure)).getFailure().isSameAs(this.failure);
  }

  @Test
  void hasBeanWhenHasBeanShouldPass() {
    this.context.registerSingleton("foo", Foo.class);
    assertThat(getAssert(this.context)).hasBean("foo");
  }

  @Test
  void hasBeanWhenHasNoBeanShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).hasBean("foo"))
            .withMessageContaining("no such bean");
  }

  @Test
  void hasBeanWhenNotStartedShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.failure)).hasBean("foo"))
            .withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
  }

  @Test
  void hasSingleBeanWhenHasSingleBeanShouldPass() {
    this.context.registerSingleton("foo", Foo.class);
    assertThat(getAssert(this.context)).hasSingleBean(Foo.class);
  }

  @Test
  void hasSingleBeanWhenHasNoBeansShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).hasSingleBean(Foo.class))
            .withMessageContaining("to have a single bean of type");
  }

  @Test
  void hasSingleBeanWhenHasMultipleShouldFail() {
    this.context.registerSingleton("foo", Foo.class);
    this.context.registerSingleton("bar", Foo.class);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).hasSingleBean(Foo.class))
            .withMessageContaining("but found:");
  }

  @Test
  void hasSingleBeanWhenFailedToStartShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.failure)).hasSingleBean(Foo.class))
            .withMessageContaining("to have a single bean of type")
            .withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
  }

  @Test
  void hasSingleBeanWhenInParentShouldFail() {
    this.parent.registerSingleton("foo", Foo.class);
    this.context.registerSingleton("bar", Foo.class);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).hasSingleBean(Foo.class))
            .withMessageContaining("but found:");
  }

  @Test
  void hasSingleBeanWithLimitedScopeWhenInParentShouldPass() {
    this.parent.registerSingleton("foo", Foo.class);
    this.context.registerSingleton("bar", Foo.class);
    assertThat(getAssert(this.context)).hasSingleBean(Foo.class, Scope.NO_ANCESTORS);
  }

  @Test
  void doesNotHaveBeanOfTypeWhenHasNoBeanOfTypeShouldPass() {
    assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class);
  }

  @Test
  void doesNotHaveBeanOfTypeWhenHasBeanOfTypeShouldFail() {
    this.context.registerSingleton("foo", Foo.class);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class))
            .withMessageContaining("but found");
  }

  @Test
  void doesNotHaveBeanOfTypeWhenFailedToStartShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.failure)).doesNotHaveBean(Foo.class))
            .withMessageContaining("not to have any beans of type")
            .withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
  }

  @Test
  void doesNotHaveBeanOfTypeWhenInParentShouldFail() {
    this.parent.registerSingleton("foo", Foo.class);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class))
            .withMessageContaining("but found");
  }

  @Test
  void doesNotHaveBeanOfTypeWithLimitedScopeWhenInParentShouldPass() {
    this.parent.registerSingleton("foo", Foo.class);
    assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class, Scope.NO_ANCESTORS);
  }

  @Test
  void doesNotHaveBeanOfNameWhenHasNoBeanOfTypeShouldPass() {
    assertThat(getAssert(this.context)).doesNotHaveBean("foo");
  }

  @Test
  void doesNotHaveBeanOfNameWhenHasBeanOfTypeShouldFail() {
    this.context.registerSingleton("foo", Foo.class);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).doesNotHaveBean("foo"))
            .withMessageContaining("but found");
  }

  @Test
  void doesNotHaveBeanOfNameWhenFailedToStartShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.failure)).doesNotHaveBean("foo"))
            .withMessageContaining("not to have any beans of name")
            .withMessageContaining("failed to start");
  }

  @Test
  void getBeanNamesWhenHasNamesShouldReturnNamesAssert() {
    this.context.registerSingleton("foo", Foo.class);
    this.context.registerSingleton("bar", Foo.class);
    assertThat(getAssert(this.context)).getBeanNames(Foo.class).containsOnly("foo", "bar");
  }

  @Test
  void getBeanNamesWhenHasNoNamesShouldReturnEmptyAssert() {
    assertThat(getAssert(this.context)).getBeanNames(Foo.class).isEmpty();
  }

  @Test
  void getBeanNamesWhenFailedToStartShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.failure)).doesNotHaveBean("foo"))
            .withMessageContaining("not to have any beans of name")
            .withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
  }

  @Test
  void getBeanOfTypeWhenHasBeanShouldReturnBeanAssert() {
    this.context.registerSingleton("foo", Foo.class);
    assertThat(getAssert(this.context)).getBean(Foo.class).isNotNull();
  }

  @Test
  void getBeanOfTypeWhenHasNoBeanShouldReturnNullAssert() {
    assertThat(getAssert(this.context)).getBean(Foo.class).isNull();
  }

  @Test
  void getBeanOfTypeWhenHasMultipleBeansShouldFail() {
    this.context.registerSingleton("foo", Foo.class);
    this.context.registerSingleton("bar", Foo.class);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).getBean(Foo.class))
            .withMessageContaining("but found");
  }

  @Test
  void getBeanOfTypeWhenHasPrimaryBeanShouldReturnPrimary() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(PrimaryFooConfig.class);
    assertThat(getAssert(context)).getBean(Foo.class).isInstanceOf(Bar.class);
    context.close();
  }

  @Test
  void getBeanOfTypeWhenFailedToStartShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.failure)).getBean(Foo.class))
            .withMessageContaining("to contain bean of type")
            .withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
  }

  @Test
  void getBeanOfTypeWhenInParentShouldReturnBeanAssert() {
    this.parent.registerSingleton("foo", Foo.class);
    assertThat(getAssert(this.context)).getBean(Foo.class).isNotNull();
  }

  @Test
  void getBeanOfTypeWhenInParentWithLimitedScopeShouldReturnNullAssert() {
    this.parent.registerSingleton("foo", Foo.class);
    assertThat(getAssert(this.context)).getBean(Foo.class, Scope.NO_ANCESTORS).isNull();
  }

  @Test
  void getBeanOfTypeWhenHasMultipleBeansIncludingParentShouldFail() {
    this.parent.registerSingleton("foo", Foo.class);
    this.context.registerSingleton("bar", Foo.class);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).getBean(Foo.class))
            .withMessageContaining("but found");
  }

  @Test
  void getBeanOfTypeWithLimitedScopeWhenHasMultipleBeansIncludingParentShouldReturnBeanAssert() {
    this.parent.registerSingleton("foo", Foo.class);
    this.context.registerSingleton("bar", Foo.class);
    assertThat(getAssert(this.context)).getBean(Foo.class, Scope.NO_ANCESTORS).isNotNull();
  }

  @Test
  void getBeanOfNameWhenHasBeanShouldReturnBeanAssert() {
    this.context.registerSingleton("foo", Foo.class);
    assertThat(getAssert(this.context)).getBean("foo").isNotNull();
  }

  @Test
  void getBeanOfNameWhenHasNoBeanOfNameShouldReturnNullAssert() {
    assertThat(getAssert(this.context)).getBean("foo").isNull();
  }

  @Test
  void getBeanOfNameWhenFailedToStartShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.failure)).getBean("foo"))
            .withMessageContaining("to contain a bean of name")
            .withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
  }

  @Test
  void getBeanOfNameAndTypeWhenHasBeanShouldReturnBeanAssert() {
    this.context.registerSingleton("foo", Foo.class);
    assertThat(getAssert(this.context)).getBean("foo", Foo.class).isNotNull();
  }

  @Test
  void getBeanOfNameAndTypeWhenHasNoBeanOfNameShouldReturnNullAssert() {
    assertThat(getAssert(this.context)).getBean("foo", Foo.class).isNull();
  }

  @Test
  void getBeanOfNameAndTypeWhenHasNoBeanOfNameButDifferentTypeShouldFail() {
    this.context.registerSingleton("foo", Foo.class);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).getBean("foo", String.class))
            .withMessageContaining("of type");
  }

  @Test
  void getBeanOfNameAndTypeWhenFailedToStartShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.failure)).getBean("foo", Foo.class))
            .withMessageContaining("to contain a bean of name")
            .withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
  }

  @Test
  void getBeansWhenHasBeansShouldReturnMapAssert() {
    this.context.registerSingleton("foo", Foo.class);
    this.context.registerSingleton("bar", Foo.class);
    assertThat(getAssert(this.context)).getBeans(Foo.class).hasSize(2).containsKeys("foo", "bar");
  }

  @Test
  void getBeansWhenHasNoBeansShouldReturnEmptyMapAssert() {
    assertThat(getAssert(this.context)).getBeans(Foo.class).isEmpty();
  }

  @Test
  void getBeansWhenFailedToStartShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.failure)).getBeans(Foo.class))
            .withMessageContaining("to get beans of type")
            .withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
  }

  @Test
  void getBeansShouldIncludeBeansFromParentScope() {
    this.parent.registerSingleton("foo", Foo.class);
    this.context.registerSingleton("bar", Foo.class);
    assertThat(getAssert(this.context)).getBeans(Foo.class).hasSize(2).containsKeys("foo", "bar");
  }

  @Test
  void getBeansWithLimitedScopeShouldNotIncludeBeansFromParentScope() {
    this.parent.registerSingleton("foo", Foo.class);
    this.context.registerSingleton("bar", Foo.class);
    assertThat(getAssert(this.context)).getBeans(Foo.class, Scope.NO_ANCESTORS).hasSize(1).containsKeys("bar");
  }

  @Test
  void getFailureWhenFailedShouldReturnFailure() {
    assertThat(getAssert(this.failure)).getFailure().isSameAs(this.failure);
  }

  @Test
  void getFailureWhenDidNotFailShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).getFailure())
            .withMessageContaining("context started");
  }

  @Test
  void hasFailedWhenFailedShouldPass() {
    assertThat(getAssert(this.failure)).hasFailed();
  }

  @Test
  void hasFailedWhenNotFailedShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.context)).hasFailed())
            .withMessageContaining("to have failed");
  }

  @Test
  void hasNotFailedWhenFailedShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(getAssert(this.failure)).hasNotFailed())
            .withMessageContaining("to have not failed")
            .withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
  }

  @Test
  void hasNotFailedWhenNotFailedShouldPass() {
    assertThat(getAssert(this.context)).hasNotFailed();
  }

  private AssertableApplicationContext getAssert(ConfigurableApplicationContext applicationContext) {
    return AssertableApplicationContext.get(() -> applicationContext);
  }

  private AssertableApplicationContext getAssert(RuntimeException failure) {
    return AssertableApplicationContext.get(() -> {
      throw failure;
    });
  }

  static class Foo {

  }

  static class Bar extends Foo {

  }

  @Configuration(proxyBeanMethods = false)
  static class PrimaryFooConfig {

    @Bean
    Foo foo() {
      return new Foo();
    }

    @Bean
    @Primary
    Bar bar() {
      return new Bar();
    }

  }

}
