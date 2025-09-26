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

package infra.context.annotation.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import infra.beans.factory.annotation.Value;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.support.PropertySourcesPlaceholderConfigurer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * A configuration class that registers a non-static placeholder configurer {@code @Component}
 * method cannot also have {@code @Value} fields. Logically, the config class must be
 * instantiated in order to invoke the placeholder configurer bean method, and it is a
 * chicken-and-egg problem to process the {@code @Value} field.
 *
 * <p>Therefore, placeholder configurer bean methods should either be {@code static} or
 * put in separate configuration classes as has been done in the tests below. Simply said,
 * placeholder configurer {@code @Component} methods and {@code @Value} fields in the same
 * configuration class are mutually exclusive unless the placeholder configurer
 * {@code @Component} method is {@code static}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@Execution(ExecutionMode.SAME_THREAD)
public class ConfigurationClassWithPlaceholderConfigurerBeanTests {

  /**
   * Test which proves that a non-static property placeholder bean cannot be declared
   * in the same configuration class that has a {@code @Value} field in need of
   * placeholder replacement. It's an obvious chicken-and-egg issue.
   *
   * <p>One solution is to do as {@link #valueFieldsAreProcessedWhenPlaceholderConfigurerIsSegregated()}
   * does and segregate the two bean definitions across configuration classes.
   *
   * <p>Another solution is to simply make the {@code @Component} method for the property
   * placeholder {@code static} as in
   * {@link #valueFieldsAreProcessedWhenStaticPlaceholderConfigurerIsIntegrated()}.
   */
  @Test
  public void valueFieldsAreNotProcessedWhenPlaceholderConfigurerIsIntegrated() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithValueFieldAndPlaceholderConfigurer.class);
    System.setProperty("test.name", "foo");
    ctx.refresh();
    System.clearProperty("test.name");

    TestBean testBean = ctx.getBean(TestBean.class);
    // Proof that the @Value field did not get set:
    assertThat(testBean.getName()).isNull();
  }

  @Test
  public void valueFieldsAreProcessedWhenStaticPlaceholderConfigurerIsIntegrated() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithValueFieldAndStaticPlaceholderConfigurer.class);
    System.setProperty("test.name", "foo");
    ctx.refresh();
    System.clearProperty("test.name");

    TestBean testBean = ctx.getBean(TestBean.class);
    assertThat(testBean.getName()).isEqualTo("foo");
  }

  @Test
  public void valueFieldsAreProcessedWhenPlaceholderConfigurerIsSegregated() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithValueField.class);
    ctx.register(ConfigWithPlaceholderConfigurer.class);
    System.setProperty("test.name", "foo");
    ctx.refresh();
    System.clearProperty("test.name");

    TestBean testBean = ctx.getBean(TestBean.class);
    assertThat(testBean.getName()).isEqualTo("foo");
  }

  @Test
  public void valueFieldsResolveToPlaceholderSpecifiedDefaultValuesWithPlaceholderConfigurer() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithValueField.class);
    ctx.register(ConfigWithPlaceholderConfigurer.class);
    ctx.refresh();

    TestBean testBean = ctx.getBean(TestBean.class);
    assertThat(testBean.getName()).isEqualTo("bar");
  }

  @Test
  public void valueFieldsResolveToPlaceholderSpecifiedDefaultValuesWithoutPlaceholderConfigurer() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ConfigWithValueField.class);
    // ctx.register(ConfigWithPlaceholderConfigurer.class);
    ctx.refresh();

    TestBean testBean = ctx.getBean(TestBean.class);
    assertThat(testBean.getName()).isEqualTo("bar");
  }

  @Configuration
  static class ConfigWithValueField {

    @Value("${test.name:bar}")
    private String name;

    @Bean
    public ITestBean testBean() {
      return new TestBean(this.name);
    }
  }

  @Configuration
  static class ConfigWithPlaceholderConfigurer {

    @Bean
    public PropertySourcesPlaceholderConfigurer ppc() {
      return new PropertySourcesPlaceholderConfigurer();
    }
  }

  @Configuration
  static class ConfigWithValueFieldAndPlaceholderConfigurer {

    @Value("${test.name}")
    private String name;

    @Bean
    public ITestBean testBean() {
      return new TestBean(this.name);
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer ppc() {
      return new PropertySourcesPlaceholderConfigurer();
    }
  }

  @Configuration
  static class ConfigWithValueFieldAndStaticPlaceholderConfigurer {

    @Value("${test.name}")
    private String name;

    @Bean
    public ITestBean testBean() {
      return new TestBean(this.name);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer ppc() {
      return new PropertySourcesPlaceholderConfigurer();
    }
  }

}
