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

package infra.beans.factory.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import infra.beans.factory.config.PropertiesFactoryBean;
import infra.beans.factory.config.RuntimeBeanReference;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.core.io.ClassPathResource;

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
