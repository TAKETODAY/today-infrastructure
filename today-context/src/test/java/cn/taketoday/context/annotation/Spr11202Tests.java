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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Dave Syer
 */
public class Spr11202Tests {

  @Test
  public void testWithImporter() {
    ApplicationContext context = new AnnotationConfigApplicationContext(Wrapper.class);
    assertThat(context.getBean("value")).isEqualTo("foo");
  }

  @Test
  public void testWithoutImporter() {
    ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
    assertThat(context.getBean("value")).isEqualTo("foo");
  }

  @Configuration
  @Import(Selector.class)
  protected static class Wrapper {
  }

  protected static class Selector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      return new String[] { Config.class.getName() };
    }
  }

  @Configuration
  protected static class Config {

    @Bean
    public FooFactoryBean foo() {
      return new FooFactoryBean();
    }

    @Bean
    public String value() throws Exception {
      String name = foo().getObject().getName();
      Assert.state(name != null, "Name cannot be null");
      return name;
    }

    @Bean
    @Conditional(NoBarCondition.class)
    public String bar() throws Exception {
      return "bar";
    }
  }

  protected static class NoBarCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      if (context.getBeanFactory().getBeanNamesForAnnotation(Bar.class).size() > 0) {
        return false;
      }
      return true;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Target(ElementType.TYPE)
  protected @interface Bar {
  }

  protected static class FooFactoryBean implements FactoryBean<Foo>, InitializingBean {

    private Foo foo = new Foo();

    @Override
    public Foo getObject() throws Exception {
      return foo;
    }

    @Override
    public Class<?> getObjectType() {
      return Foo.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
      this.foo.name = "foo";
    }
  }

  protected static class Foo {

    private String name;

    public String getName() {
      return name;
    }
  }

}
