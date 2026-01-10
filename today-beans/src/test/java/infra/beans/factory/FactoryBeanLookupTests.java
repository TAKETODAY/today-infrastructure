/*
 * Copyright 2002-present the original author or authors.
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

