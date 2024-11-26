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

package infra.beans.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.beans.factory.config.AbstractFactoryBean;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/6/20 12:47
 */
public class FactoryBeanLookupTests {
  private BeanFactory beanFactory;

  @BeforeEach
  public void setUp() {
    beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader((BeanDefinitionRegistry) beanFactory).loadBeanDefinitions(
            new ClassPathResource("FactoryBeanLookupTests-context.xml", this.getClass()));
  }

  @Test
  public void factoryBeanLookupByNameDereferencing() {
    Object fooFactory = beanFactory.getBean("&fooFactory");
    assertThat(fooFactory).isInstanceOf(FooFactoryBean.class);
  }

  @Test
  public void factoryBeanLookupByType() {
    FooFactoryBean fooFactory = beanFactory.getBean(FooFactoryBean.class);
    assertThat(fooFactory).isNotNull();
  }

  @Test
  public void factoryBeanLookupByTypeAndNameDereference() {
    FooFactoryBean fooFactory = beanFactory.getBean("&fooFactory", FooFactoryBean.class);
    assertThat(fooFactory).isNotNull();
  }

  @Test
  public void factoryBeanObjectLookupByName() {
    Object fooFactory = beanFactory.getBean("fooFactory");
    assertThat(fooFactory).isInstanceOf(Foo.class);
  }

  @Test
  public void factoryBeanObjectLookupByNameAndType() {
    Foo foo = beanFactory.getBean("fooFactory", Foo.class);
    assertThat(foo).isNotNull();
  }
}

class FooFactoryBean extends AbstractFactoryBean<Foo> {

  @Override
  protected Foo createBeanInstance() throws Exception {
    return new Foo();
  }

  @Override
  public Class<?> getObjectType() {
    return Foo.class;
  }
}

class Foo { }

