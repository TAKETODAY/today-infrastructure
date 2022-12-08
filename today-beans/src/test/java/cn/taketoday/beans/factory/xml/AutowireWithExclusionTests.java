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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import cn.taketoday.beans.factory.config.PropertiesFactoryBean;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@Execution(ExecutionMode.SAME_THREAD)
public class AutowireWithExclusionTests {

  @Test
  public void byTypeAutowireWithAutoSelfExclusion() throws Exception {
    CountingFactory.reset();
    StandardBeanFactory beanFactory = getBeanFactory("autowire-with-exclusion.xml");
    beanFactory.preInstantiateSingletons();
    TestBean rob = (TestBean) beanFactory.getBean("rob");
    TestBean sally = (TestBean) beanFactory.getBean("sally");
    assertThat(rob.getSpouse()).isEqualTo(sally);
    assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
  }

  @Test
  public void byTypeAutowireWithExclusion() throws Exception {
    CountingFactory.reset();
    StandardBeanFactory beanFactory = getBeanFactory("autowire-with-exclusion.xml");
    beanFactory.preInstantiateSingletons();
    TestBean rob = (TestBean) beanFactory.getBean("rob");
    assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
    assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
  }

  @Test
  public void byTypeAutowireWithExclusionInParentFactory() throws Exception {
    CountingFactory.reset();
    StandardBeanFactory parent = getBeanFactory("autowire-with-exclusion.xml");
    parent.preInstantiateSingletons();
    StandardBeanFactory child = new StandardBeanFactory(parent);
    RootBeanDefinition robDef = new RootBeanDefinition(TestBean.class);
    robDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
    robDef.getPropertyValues().add("spouse", new RuntimeBeanReference("sally"));
    child.registerBeanDefinition("rob2", robDef);
    TestBean rob = (TestBean) child.getBean("rob2");
    assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
    assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
  }

  @Test
  public void byTypeAutowireWithPrimaryInParentFactory() throws Exception {
    CountingFactory.reset();
    StandardBeanFactory parent = getBeanFactory("autowire-with-exclusion.xml");
    parent.getBeanDefinition("props1").setPrimary(true);
    parent.preInstantiateSingletons();
    StandardBeanFactory child = new StandardBeanFactory(parent);
    RootBeanDefinition robDef = new RootBeanDefinition(TestBean.class);
    robDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
    robDef.getPropertyValues().add("spouse", new RuntimeBeanReference("sally"));
    child.registerBeanDefinition("rob2", robDef);
    RootBeanDefinition propsDef = new RootBeanDefinition(PropertiesFactoryBean.class);
    propsDef.getPropertyValues().add("properties", "name=props3");
    child.registerBeanDefinition("props3", propsDef);
    TestBean rob = (TestBean) child.getBean("rob2");
    assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
    assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
  }

  @Test
  public void byTypeAutowireWithPrimaryOverridingParentFactory() throws Exception {
    CountingFactory.reset();
    StandardBeanFactory parent = getBeanFactory("autowire-with-exclusion.xml");
    parent.preInstantiateSingletons();
    StandardBeanFactory child = new StandardBeanFactory(parent);
    RootBeanDefinition robDef = new RootBeanDefinition(TestBean.class);
    robDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
    robDef.getPropertyValues().add("spouse", new RuntimeBeanReference("sally"));
    child.registerBeanDefinition("rob2", robDef);
    RootBeanDefinition propsDef = new RootBeanDefinition(PropertiesFactoryBean.class);
    propsDef.getPropertyValues().add("properties", "name=props3");
    propsDef.setPrimary(true);
    child.registerBeanDefinition("props3", propsDef);
    TestBean rob = (TestBean) child.getBean("rob2");
    assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props3");
    assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
  }

  @Test
  public void byTypeAutowireWithPrimaryInParentAndChild() throws Exception {
    CountingFactory.reset();
    StandardBeanFactory parent = getBeanFactory("autowire-with-exclusion.xml");
    parent.getBeanDefinition("props1").setPrimary(true);
    parent.preInstantiateSingletons();
    StandardBeanFactory child = new StandardBeanFactory(parent);
    RootBeanDefinition robDef = new RootBeanDefinition(TestBean.class);
    robDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
    robDef.getPropertyValues().add("spouse", new RuntimeBeanReference("sally"));
    child.registerBeanDefinition("rob2", robDef);
    RootBeanDefinition propsDef = new RootBeanDefinition(PropertiesFactoryBean.class);
    propsDef.getPropertyValues().add("properties", "name=props3");
    propsDef.setPrimary(true);
    child.registerBeanDefinition("props3", propsDef);
    TestBean rob = (TestBean) child.getBean("rob2");
    assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props3");
    assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
  }

  @Test
  public void byTypeAutowireWithInclusion() throws Exception {
    CountingFactory.reset();
    StandardBeanFactory beanFactory = getBeanFactory("autowire-with-inclusion.xml");
    beanFactory.preInstantiateSingletons();
    TestBean rob = (TestBean) beanFactory.getBean("rob");
    assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
    assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
  }

  @Test
  public void byTypeAutowireWithSelectiveInclusion() throws Exception {
    CountingFactory.reset();
    StandardBeanFactory beanFactory = getBeanFactory("autowire-with-selective-inclusion.xml");
    beanFactory.preInstantiateSingletons();
    TestBean rob = (TestBean) beanFactory.getBean("rob");
    assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
    assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
  }

  @Test
  public void constructorAutowireWithAutoSelfExclusion() throws Exception {
    StandardBeanFactory beanFactory = getBeanFactory("autowire-constructor-with-exclusion.xml");
    TestBean rob = (TestBean) beanFactory.getBean("rob");
    TestBean sally = (TestBean) beanFactory.getBean("sally");
    assertThat(rob.getSpouse()).isEqualTo(sally);
    TestBean rob2 = (TestBean) beanFactory.getBean("rob");
    assertThat(rob2).isEqualTo(rob);
    assertThat(rob2).isNotSameAs(rob);
    assertThat(rob2.getSpouse()).isEqualTo(rob.getSpouse());
    assertThat(rob2.getSpouse()).isNotSameAs(rob.getSpouse());
  }

  @Test
  public void constructorAutowireWithExclusion() throws Exception {
    StandardBeanFactory beanFactory = getBeanFactory("autowire-constructor-with-exclusion.xml");
    TestBean rob = (TestBean) beanFactory.getBean("rob");
    assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
  }

  private StandardBeanFactory getBeanFactory(String configPath) {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            new ClassPathResource(configPath, getClass()));
    return bf;
  }

}
