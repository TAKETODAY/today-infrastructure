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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.AbstractBeanFactoryTests;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.LifecycleBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.beans.testfixture.beans.factory.DummyFactory;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 09.11.2003
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class XmlListableBeanFactoryTests extends AbstractBeanFactoryTests {

  private StandardBeanFactory parent;

  private StandardBeanFactory factory;

  @BeforeEach
  public void setup() {
    parent = new StandardBeanFactory();

    Map map = new HashMap();
    map.put("name", "Albert");
    RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
    bd1.setPropertyValues(new PropertyValues(map));
    parent.registerBeanDefinition("father", bd1);

    map = new HashMap();
    map.put("name", "Roderick");
    RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
    bd2.setPropertyValues(new PropertyValues(map));
    parent.registerBeanDefinition("rod", bd2);

    this.factory = new StandardBeanFactory(parent);
    new XmlBeanDefinitionReader(this.factory).loadBeanDefinitions(new ClassPathResource("test.xml", getClass()));

    this.factory.addBeanPostProcessor(new InitializationBeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
        if (bean instanceof TestBean) {
          ((TestBean) bean).setPostProcessed(true);
        }
        if (bean instanceof DummyFactory) {
          ((DummyFactory) bean).setPostProcessed(true);
        }
        return bean;
      }

      @Override
      public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
        return bean;
      }
    });

    this.factory.addBeanPostProcessor(new LifecycleBean.PostProcessor());
    this.factory.addBeanPostProcessor(new ProtectedLifecycleBean.PostProcessor());
    // this.factory.preInstantiateSingletons();
  }

  @Override
  protected BeanFactory getBeanFactory() {
    return factory;
  }

  @Test
  public void count() {
    assertCount(24);
  }

  @Test
  public void beanCount() {
    assertTestBeanCount(13);
  }

  protected void assertTestBeanCount(int count) {
    Set<String> defNames = getBeanFactory().getBeanNamesForType(TestBean.class, true, false);
    assertThat(defNames.size() == count).as("We should have " + count + " beans for class cn.taketoday.beans.testfixture.beans.TestBean, not " +
            defNames.size()).isTrue();

    int countIncludingFactoryBeans = count + 2;
    Set<String> names = getBeanFactory().getBeanNamesForType(TestBean.class, true, true);
    assertThat(names.size() == countIncludingFactoryBeans).as("We should have " + countIncludingFactoryBeans +
            " beans for class cn.taketoday.beans.testfixture.beans.TestBean, not " + names.size()).isTrue();
  }

  @Test
  public void lifecycleMethods() {
    LifecycleBean bean = (LifecycleBean) getBeanFactory().getBean("lifecycle");
    bean.businessMethod();
  }

  @Test
  public void protectedLifecycleMethods() {
    ProtectedLifecycleBean bean = (ProtectedLifecycleBean) getBeanFactory().getBean("protectedLifecycle");
    bean.businessMethod();
  }

  @Test
  public void descriptionButNoProperties() {
    TestBean validEmpty = (TestBean) getBeanFactory().getBean("validEmptyWithDescription");
    assertThat(validEmpty.getAge()).isEqualTo(0);
  }

  /**
   * Test that properties with name as well as id creating an alias up front.
   */
  @Test
  public void autoAliasing() {
    List beanNames = Arrays.asList(getBeanFactory().getBeanDefinitionNames());

    TestBean tb1 = (TestBean) getBeanFactory().getBean("aliased");
    TestBean alias1 = (TestBean) getBeanFactory().getBean("myalias");
    assertThat(tb1 == alias1).isTrue();
    List tb1Aliases = Arrays.asList(getBeanFactory().getAliases("aliased"));
    assertThat(tb1Aliases.size()).isEqualTo(2);
    assertThat(tb1Aliases.contains("myalias")).isTrue();
    assertThat(tb1Aliases.contains("youralias")).isTrue();
    assertThat(beanNames.contains("aliased")).isTrue();
    assertThat(beanNames.contains("myalias")).isFalse();
    assertThat(beanNames.contains("youralias")).isFalse();

    TestBean tb2 = (TestBean) getBeanFactory().getBean("multiAliased");
    TestBean alias2 = (TestBean) getBeanFactory().getBean("alias1");
    TestBean alias3 = (TestBean) getBeanFactory().getBean("alias2");
    TestBean alias3a = (TestBean) getBeanFactory().getBean("alias3");
    TestBean alias3b = (TestBean) getBeanFactory().getBean("alias4");
    assertThat(tb2 == alias2).isTrue();
    assertThat(tb2 == alias3).isTrue();
    assertThat(tb2 == alias3a).isTrue();
    assertThat(tb2 == alias3b).isTrue();

    List tb2Aliases = Arrays.asList(getBeanFactory().getAliases("multiAliased"));
    assertThat(tb2Aliases.size()).isEqualTo(4);
    assertThat(tb2Aliases.contains("alias1")).isTrue();
    assertThat(tb2Aliases.contains("alias2")).isTrue();
    assertThat(tb2Aliases.contains("alias3")).isTrue();
    assertThat(tb2Aliases.contains("alias4")).isTrue();
    assertThat(beanNames.contains("multiAliased")).isTrue();
    assertThat(beanNames.contains("alias1")).isFalse();
    assertThat(beanNames.contains("alias2")).isFalse();
    assertThat(beanNames.contains("alias3")).isFalse();
    assertThat(beanNames.contains("alias4")).isFalse();

    TestBean tb3 = (TestBean) getBeanFactory().getBean("aliasWithoutId1");
    TestBean alias4 = (TestBean) getBeanFactory().getBean("aliasWithoutId2");
    TestBean alias5 = (TestBean) getBeanFactory().getBean("aliasWithoutId3");
    assertThat(tb3 == alias4).isTrue();
    assertThat(tb3 == alias5).isTrue();
    List tb3Aliases = Arrays.asList(getBeanFactory().getAliases("aliasWithoutId1"));
    assertThat(tb3Aliases.size()).isEqualTo(2);
    assertThat(tb3Aliases.contains("aliasWithoutId2")).isTrue();
    assertThat(tb3Aliases.contains("aliasWithoutId3")).isTrue();
    assertThat(beanNames.contains("aliasWithoutId1")).isTrue();
    assertThat(beanNames.contains("aliasWithoutId2")).isFalse();
    assertThat(beanNames.contains("aliasWithoutId3")).isFalse();

    TestBean tb4 = (TestBean) getBeanFactory().getBean(TestBean.class.getName() + "#0");
    assertThat(tb4.getName()).isNull();

    Map drs = getBeanFactory().getBeansOfType(DummyReferencer.class, false, false);
    assertThat(drs.size()).isEqualTo(5);
    assertThat(drs.containsKey(DummyReferencer.class.getName() + "#0")).isTrue();
    assertThat(drs.containsKey(DummyReferencer.class.getName() + "#1")).isTrue();
    assertThat(drs.containsKey(DummyReferencer.class.getName() + "#2")).isTrue();
  }

  @Test
  public void factoryNesting() {
    ITestBean father = (ITestBean) getBeanFactory().getBean("father");
    assertThat(father != null).as("Bean from root context").isTrue();

    TestBean rod = (TestBean) getBeanFactory().getBean("rod");
    assertThat("Rod".equals(rod.getName())).as("Bean from child context").isTrue();
    assertThat(rod.getSpouse() == father).as("Bean has external reference").isTrue();

    rod = (TestBean) parent.getBean("rod");
    assertThat("Roderick".equals(rod.getName())).as("Bean from root context").isTrue();
  }

  @Test
  public void factoryReferences() {
    DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");

    DummyReferencer ref = (DummyReferencer) getBeanFactory().getBean("factoryReferencer");
    assertThat(ref.getTestBean1() == ref.getTestBean2()).isTrue();
    assertThat(ref.getDummyFactory() == factory).isTrue();

    DummyReferencer ref2 = (DummyReferencer) getBeanFactory().getBean("factoryReferencerWithConstructor");
    assertThat(ref2.getTestBean1() == ref2.getTestBean2()).isTrue();
    assertThat(ref2.getDummyFactory() == factory).isTrue();
  }

  @Test
  public void prototypeReferences() {
    // check that not broken by circular reference resolution mechanism
    DummyReferencer ref1 = (DummyReferencer) getBeanFactory().getBean("prototypeReferencer");
    assertThat(ref1.getTestBean1() != ref1.getTestBean2()).as("Not referencing same bean twice").isTrue();
    DummyReferencer ref2 = (DummyReferencer) getBeanFactory().getBean("prototypeReferencer");
    assertThat(ref1 != ref2).as("Not the same referencer").isTrue();
    assertThat(ref2.getTestBean1() != ref2.getTestBean2()).as("Not referencing same bean twice").isTrue();
    assertThat(ref1.getTestBean1() != ref2.getTestBean1()).as("Not referencing same bean twice").isTrue();
    assertThat(ref1.getTestBean2() != ref2.getTestBean2()).as("Not referencing same bean twice").isTrue();
    assertThat(ref1.getTestBean1() != ref2.getTestBean2()).as("Not referencing same bean twice").isTrue();
  }

  @Test
  public void beanPostProcessor() {
    TestBean kerry = (TestBean) getBeanFactory().getBean("kerry");
    TestBean kathy = (TestBean) getBeanFactory().getBean("kathy");
    DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
    TestBean factoryCreated = (TestBean) getBeanFactory().getBean("singletonFactory");
    assertThat(kerry.isPostProcessed()).isTrue();
    assertThat(kathy.isPostProcessed()).isTrue();
    assertThat(factory.isPostProcessed()).isTrue();
    assertThat(factoryCreated.isPostProcessed()).isTrue();
  }

  @Test
  public void emptyValues() {
    TestBean rod = (TestBean) getBeanFactory().getBean("rod");
    TestBean kerry = (TestBean) getBeanFactory().getBean("kerry");
    assertThat("".equals(rod.getTouchy())).as("Touchy is empty").isTrue();
    assertThat("".equals(kerry.getTouchy())).as("Touchy is empty").isTrue();
  }

  @Test
  public void commentsAndCdataInValue() {
    TestBean bean = (TestBean) getBeanFactory().getBean("commentsInValue");
    assertThat(bean.getName()).as("Failed to handle comments and CDATA properly").isEqualTo("this is a <!--comment-->");
  }

  /**
   * Simple test of BeanFactory initialization and lifecycle callbacks.
   *
   * @author Rod Johnson
   * @author Juergen Hoeller
   */
  class ProtectedLifecycleBean implements BeanNameAware, BeanFactoryAware, InitializingBean, DisposableBean {

    protected boolean initMethodDeclared = false;

    protected String beanName;

    protected BeanFactory owningFactory;

    protected boolean postProcessedBeforeInit;

    protected boolean inited;

    protected boolean initedViaDeclaredInitMethod;

    protected boolean postProcessedAfterInit;

    protected boolean destroyed;

    public void setInitMethodDeclared(boolean initMethodDeclared) {
      this.initMethodDeclared = initMethodDeclared;
    }

    public boolean isInitMethodDeclared() {
      return initMethodDeclared;
    }

    @Override
    public void setBeanName(String name) {
      this.beanName = name;
    }

    public String getBeanName() {
      return beanName;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
      this.owningFactory = beanFactory;
    }

    public void postProcessBeforeInit() {
      if (this.inited || this.initedViaDeclaredInitMethod) {
        throw new RuntimeException("Factory called postProcessBeforeInit after afterPropertiesSet");
      }
      if (this.postProcessedBeforeInit) {
        throw new RuntimeException("Factory called postProcessBeforeInit twice");
      }
      this.postProcessedBeforeInit = true;
    }

    @Override
    public void afterPropertiesSet() {
      if (this.owningFactory == null) {
        throw new RuntimeException("Factory didn't call setBeanFactory before afterPropertiesSet on lifecycle bean");
      }
      if (!this.postProcessedBeforeInit) {
        throw new RuntimeException("Factory didn't call postProcessBeforeInit before afterPropertiesSet on lifecycle bean");
      }
      if (this.initedViaDeclaredInitMethod) {
        throw new RuntimeException("Factory initialized via declared init method before initializing via afterPropertiesSet");
      }
      if (this.inited) {
        throw new RuntimeException("Factory called afterPropertiesSet twice");
      }
      this.inited = true;
    }

    public void declaredInitMethod() {
      if (!this.inited) {
        throw new RuntimeException("Factory didn't call afterPropertiesSet before declared init method");
      }

      if (this.initedViaDeclaredInitMethod) {
        throw new RuntimeException("Factory called declared init method twice");
      }
      this.initedViaDeclaredInitMethod = true;
    }

    public void postProcessAfterInit() {
      if (!this.inited) {
        throw new RuntimeException("Factory called postProcessAfterInit before afterPropertiesSet");
      }
      if (this.initMethodDeclared && !this.initedViaDeclaredInitMethod) {
        throw new RuntimeException("Factory called postProcessAfterInit before calling declared init method");
      }
      if (this.postProcessedAfterInit) {
        throw new RuntimeException("Factory called postProcessAfterInit twice");
      }
      this.postProcessedAfterInit = true;
    }

    /**
     * Dummy business method that will fail unless the factory
     * managed the bean's lifecycle correctly
     */
    public void businessMethod() {
      if (!this.inited || (this.initMethodDeclared && !this.initedViaDeclaredInitMethod) ||
              !this.postProcessedAfterInit) {
        throw new RuntimeException("Factory didn't initialize lifecycle object correctly");
      }
    }

    @Override
    public void destroy() {
      if (this.destroyed) {
        throw new IllegalStateException("Already destroyed");
      }
      this.destroyed = true;
    }

    public boolean isDestroyed() {
      return destroyed;
    }

    public static class PostProcessor implements InitializationBeanPostProcessor {

      @Override
      public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
        if (bean instanceof ProtectedLifecycleBean) {
          ((ProtectedLifecycleBean) bean).postProcessBeforeInit();
        }
        return bean;
      }

      @Override
      public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
        if (bean instanceof ProtectedLifecycleBean) {
          ((ProtectedLifecycleBean) bean).postProcessAfterInit();
        }
        return bean;
      }
    }
  }

  /**
   * @author Juergen Hoeller
   */
  class DummyReferencer {

    private TestBean testBean1;

    private TestBean testBean2;

    private DummyFactory dummyFactory;

    public DummyReferencer() {
    }

    public DummyReferencer(DummyFactory dummyFactory) {
      this.dummyFactory = dummyFactory;
    }

    public void setDummyFactory(DummyFactory dummyFactory) {
      this.dummyFactory = dummyFactory;
    }

    public DummyFactory getDummyFactory() {
      return dummyFactory;
    }

    public void setTestBean1(TestBean testBean1) {
      this.testBean1 = testBean1;
    }

    public TestBean getTestBean1() {
      return testBean1;
    }

    public void setTestBean2(TestBean testBean2) {
      this.testBean2 = testBean2;
    }

    public TestBean getTestBean2() {
      return testBean2;
    }
  }

}
