/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;

import static cn.taketoday.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Today <br>
 *
 * 2018-12-25 19:09
 */
class FactoryBeanTests {

  private static final Class<?> CLASS = FactoryBeanTests.class;
  private static final Resource RETURNS_NULL_CONTEXT = qualifiedResource(CLASS, "returnsNull.xml");
  private static final Resource WITH_AUTOWIRING_CONTEXT = qualifiedResource(CLASS, "withAutowiring.xml");
  private static final Resource ABSTRACT_CONTEXT = qualifiedResource(CLASS, "abstract.xml");
  private static final Resource CIRCULAR_CONTEXT = qualifiedResource(CLASS, "circular.xml");

  @Test
  public void testFactoryBeanReturnsNull() throws Exception {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(RETURNS_NULL_CONTEXT);

    assertThat(factory.getBean("factoryBean")).isNull();
  }

  @Test
  public void testFactoryBeansWithAutowiring() throws Exception {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(WITH_AUTOWIRING_CONTEXT);

    BeanFactoryPostProcessor ppc = (BeanFactoryPostProcessor) factory.getBean("propertyPlaceholderConfigurer");
    ppc.postProcessBeanFactory(factory);

    assertThat(factory.getType("betaFactory")).isNull();

    Alpha alpha = (Alpha) factory.getBean("alpha");
    Beta beta = (Beta) factory.getBean("beta");
    Gamma gamma = (Gamma) factory.getBean("gamma");
    Gamma gamma2 = (Gamma) factory.getBean("gammaFactory");

    assertThat(alpha.getBeta()).isSameAs(beta);
    assertThat(beta.getGamma()).isSameAs(gamma);
    assertThat(beta.getGamma()).isSameAs(gamma2);
    assertThat(beta.getName()).isEqualTo("yourName");
  }

  @Test
  public void testFactoryBeansWithIntermediateFactoryBeanAutowiringFailure() throws Exception {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(WITH_AUTOWIRING_CONTEXT);

    BeanFactoryPostProcessor ppc = (BeanFactoryPostProcessor) factory.getBean("propertyPlaceholderConfigurer");
    ppc.postProcessBeanFactory(factory);

    Beta beta = (Beta) factory.getBean("beta");
    Alpha alpha = (Alpha) factory.getBean("alpha");
    Gamma gamma = (Gamma) factory.getBean("gamma");
    assertThat(alpha.getBeta()).isSameAs(beta);
    assertThat(beta.getGamma()).isSameAs(gamma);
  }

  @Test
  public void testAbstractFactoryBeanViaAnnotation() throws Exception {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(ABSTRACT_CONTEXT);
    factory.getBeansWithAnnotation(Component.class);
  }

  @Test
  public void testAbstractFactoryBeanViaType() throws Exception {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(ABSTRACT_CONTEXT);
    factory.getBeansOfType(AbstractFactoryBean.class);
  }

  @Test
  public void testCircularReferenceWithPostProcessor() {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(CIRCULAR_CONTEXT);

    CountingPostProcessor counter = new CountingPostProcessor();
    factory.addBeanPostProcessor(counter);

    BeanImpl1 impl1 = factory.getBean(BeanImpl1.class);
    assertThat(impl1).isNotNull();
    assertThat(impl1.getImpl2()).isNotNull();
    assertThat(impl1.getImpl2()).isNotNull();
    assertThat(impl1.getImpl2().getImpl1()).isSameAs(impl1);
    assertThat(counter.getCount("bean1")).isEqualTo(1);
    assertThat(counter.getCount("bean2")).isEqualTo(1);
  }

  public static class NullReturningFactoryBean implements FactoryBean<Object> {

    @Override
    public Object getObject() {
      return null;
    }

    @Override
    public Class<?> getObjectType() {
      return null;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

  public static class Alpha implements InitializingBean {

    private Beta beta;

    public void setBeta(Beta beta) {
      this.beta = beta;
    }

    public Beta getBeta() {
      return beta;
    }

    @Override
    public void afterPropertiesSet() {
      Assert.notNull(beta, "'beta' property is required");
    }
  }

  public static class Beta implements InitializingBean {

    private Gamma gamma;

    private String name;

    public void setGamma(Gamma gamma) {
      this.gamma = gamma;
    }

    public Gamma getGamma() {
      return gamma;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    @Override
    public void afterPropertiesSet() {
      Assert.notNull(gamma, "'gamma' property is required");
    }
  }

  public static class Gamma {
  }

  @Component
  public static class BetaFactoryBean implements FactoryBean<Object> {

    public BetaFactoryBean(Alpha alpha) {
    }

    private Beta beta;

    public void setBeta(Beta beta) {
      this.beta = beta;
    }

    @Override
    public Object getObject() {
      return this.beta;
    }

    @Override
    public Class<?> getObjectType() {
      return null;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

  public abstract static class AbstractFactoryBean implements FactoryBean<Object> {
  }

  public static class PassThroughFactoryBean<T> implements FactoryBean<T>, BeanFactoryAware {

    private Class<T> type;

    private String instanceName;

    private BeanFactory beanFactory;

    private T instance;

    public PassThroughFactoryBean(Class<T> type) {
      this.type = type;
    }

    public void setInstanceName(String instanceName) {
      this.instanceName = instanceName;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    @Override
    public T getObject() {
      if (instance == null) {
        instance = beanFactory.getBean(instanceName, type);
      }
      return instance;
    }

    @Override
    public Class<?> getObjectType() {
      return type;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

  public static class CountingPostProcessor implements InitializationBeanPostProcessor {

    private final Map<String, AtomicInteger> count = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
      if (bean instanceof FactoryBean) {
        return bean;
      }
      AtomicInteger c = count.get(beanName);
      if (c == null) {
        c = new AtomicInteger();
        count.put(beanName, c);
      }
      c.incrementAndGet();
      return bean;
    }

    public int getCount(String beanName) {
      AtomicInteger c = count.get(beanName);
      if (c != null) {
        return c.intValue();
      }
      else {
        return 0;
      }
    }
  }

  public static class BeanImpl1 {

    private BeanImpl2 impl2;

    public BeanImpl2 getImpl2() {
      return impl2;
    }

    public void setImpl2(BeanImpl2 impl2) {
      this.impl2 = impl2;
    }
  }

  public static class BeanImpl2 {

    private BeanImpl1 impl1;

    public BeanImpl1 getImpl1() {
      return impl1;
    }

    public void setImpl1(BeanImpl1 impl1) {
      this.impl1 = impl1;
    }
  }

}
