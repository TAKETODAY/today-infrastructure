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

package cn.taketoday.orm.jpa.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.orm.jpa.AbstractEntityManagerFactoryIntegrationTests;
import cn.taketoday.orm.jpa.hibernate.beans.BeanSource;
import cn.taketoday.orm.jpa.hibernate.beans.MultiplePrototypesInSpringContextTestBean;
import cn.taketoday.orm.jpa.hibernate.beans.NoDefinitionInSpringContextTestBean;
import cn.taketoday.orm.jpa.hibernate.beans.SinglePrototypeInSpringContextTestBean;
import jakarta.persistence.AttributeConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Hibernate-specific SpringBeanContainer integration tests.
 *
 * @author Yoann Rodiere
 * @author Juergen Hoeller
 */
public class HibernateNativeEntityManagerFactorySpringBeanContainerIntegrationTests
        extends AbstractEntityManagerFactoryIntegrationTests {

  @Autowired
  private ApplicationContext applicationContext;

  @Override
  protected String[] getConfigLocations() {
    return new String[] { "/cn/taketoday/orm/jpa/hibernate/hibernate-manager-native.xml",
            "/cn/taketoday/orm/jpa/memdb.xml", "/cn/taketoday/orm/jpa/inject.xml",
            "/cn/taketoday/orm/jpa/hibernate/inject-hibernate-spring-bean-container-tests.xml" };
  }

  private ManagedBeanRegistry getManagedBeanRegistry() {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    ServiceRegistry serviceRegistry = sessionFactory.getSessionFactoryOptions().getServiceRegistry();
    return serviceRegistry.requireService(ManagedBeanRegistry.class);
  }

  private BeanContainer getBeanContainer() {
    return getManagedBeanRegistry().getBeanContainer();
  }

  @Test
  public void testCanRetrieveBeanByTypeWithJpaCompliantOptions() {
    BeanContainer beanContainer = getBeanContainer();
    assertThat(beanContainer).isNotNull();

    ContainedBean<SinglePrototypeInSpringContextTestBean> bean = beanContainer.getBean(
            SinglePrototypeInSpringContextTestBean.class,
            JpaLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
    );

    assertThat(bean).isNotNull();
    SinglePrototypeInSpringContextTestBean instance = bean.getBeanInstance();
    assertThat(instance).isNotNull();
    assertThat(instance.getApplicationContext()).isSameAs(applicationContext);
  }

  @Test
  public void testCanRetrieveBeanByNameWithJpaCompliantOptions() {
    BeanContainer beanContainer = getBeanContainer();
    assertThat(beanContainer).isNotNull();

    ContainedBean<MultiplePrototypesInSpringContextTestBean> bean = beanContainer.getBean(
            "multiple-1", MultiplePrototypesInSpringContextTestBean.class,
            JpaLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
    );

    assertThat(bean).isNotNull();
    MultiplePrototypesInSpringContextTestBean instance = bean.getBeanInstance();
    assertThat(instance).isNotNull();
    assertThat(instance.getName()).isEqualTo("multiple-1");
    assertThat(instance.getApplicationContext()).isSameAs(applicationContext);
  }

  @Test
  public void testCanRetrieveBeanByTypeWithNativeOptions() {
    BeanContainer beanContainer = getBeanContainer();
    assertThat(beanContainer).isNotNull();

    ContainedBean<SinglePrototypeInSpringContextTestBean> bean = beanContainer.getBean(
            SinglePrototypeInSpringContextTestBean.class,
            NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
    );

    assertThat(bean).isNotNull();
    SinglePrototypeInSpringContextTestBean instance = bean.getBeanInstance();
    assertThat(instance).isNotNull();
    assertThat(instance.getName()).isEqualTo("single");
    assertThat(instance.getApplicationContext()).isSameAs(applicationContext);

    ContainedBean<SinglePrototypeInSpringContextTestBean> bean2 = beanContainer.getBean(
            SinglePrototypeInSpringContextTestBean.class,
            NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
    );

    assertThat(bean2).isNotNull();
    SinglePrototypeInSpringContextTestBean instance2 = bean2.getBeanInstance();
    assertThat(instance2).isNotNull();
    // Due to the lifecycle options, and because the bean has the "prototype" scope, we should not return the same instance
    assertThat(instance2).isNotSameAs(instance);
  }

  @Test
  public void testCanRetrieveBeanByNameWithNativeOptions() {
    BeanContainer beanContainer = getBeanContainer();
    assertThat(beanContainer).isNotNull();

    ContainedBean<MultiplePrototypesInSpringContextTestBean> bean = beanContainer.getBean(
            "multiple-1", MultiplePrototypesInSpringContextTestBean.class,
            NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
    );

    assertThat(bean).isNotNull();
    MultiplePrototypesInSpringContextTestBean instance = bean.getBeanInstance();
    assertThat(instance).isNotNull();
    assertThat(instance.getName()).isEqualTo("multiple-1");
    assertThat(instance.getApplicationContext()).isSameAs(applicationContext);

    ContainedBean<MultiplePrototypesInSpringContextTestBean> bean2 = beanContainer.getBean(
            "multiple-1", MultiplePrototypesInSpringContextTestBean.class,
            NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
    );

    assertThat(bean2).isNotNull();
    MultiplePrototypesInSpringContextTestBean instance2 = bean2.getBeanInstance();
    assertThat(instance2).isNotNull();
    // Due to the lifecycle options, and because the bean has the "prototype" scope, we should not return the same instance
    assertThat(instance2).isNotSameAs(instance);
  }

  @Test
  public void testCanRetrieveFallbackBeanByTypeWithJpaCompliantOptions() {
    BeanContainer beanContainer = getBeanContainer();
    assertThat(beanContainer).isNotNull();
    NoDefinitionInSpringContextTestBeanInstanceProducer fallbackProducer = new NoDefinitionInSpringContextTestBeanInstanceProducer();

    ContainedBean<NoDefinitionInSpringContextTestBean> bean = beanContainer.getBean(
            NoDefinitionInSpringContextTestBean.class,
            JpaLifecycleOptions.INSTANCE, fallbackProducer
    );

    assertThat(fallbackProducer.currentUnnamedInstantiationCount()).isEqualTo(1);
    assertThat(fallbackProducer.currentNamedInstantiationCount()).isEqualTo(0);

    assertThat(bean).isNotNull();
    NoDefinitionInSpringContextTestBean instance = bean.getBeanInstance();
    assertThat(instance).isNotNull();
    assertThat(instance.getSource()).isEqualTo(BeanSource.FALLBACK);
    assertThat(instance.getApplicationContext()).isNull();
  }

  @Test
  public void testCanRetrieveFallbackBeanByNameWithJpaCompliantOptions() {
    BeanContainer beanContainer = getBeanContainer();
    assertThat(beanContainer).isNotNull();
    NoDefinitionInSpringContextTestBeanInstanceProducer fallbackProducer = new NoDefinitionInSpringContextTestBeanInstanceProducer();

    ContainedBean<NoDefinitionInSpringContextTestBean> bean = beanContainer.getBean(
            "some name", NoDefinitionInSpringContextTestBean.class,
            JpaLifecycleOptions.INSTANCE, fallbackProducer
    );

    assertThat(fallbackProducer.currentUnnamedInstantiationCount()).isEqualTo(0);
    assertThat(fallbackProducer.currentNamedInstantiationCount()).isEqualTo(1);

    assertThat(bean).isNotNull();
    NoDefinitionInSpringContextTestBean instance = bean.getBeanInstance();
    assertThat(instance).isNotNull();
    assertThat(instance.getSource()).isEqualTo(BeanSource.FALLBACK);
    assertThat(instance.getName()).isEqualTo("some name");
    assertThat(instance.getApplicationContext()).isNull();
  }

  @Test
  public void testCanRetrieveFallbackBeanByTypeWithNativeOptions() {
    BeanContainer beanContainer = getBeanContainer();
    assertThat(beanContainer).isNotNull();
    NoDefinitionInSpringContextTestBeanInstanceProducer fallbackProducer = new NoDefinitionInSpringContextTestBeanInstanceProducer();

    ContainedBean<NoDefinitionInSpringContextTestBean> bean = beanContainer.getBean(
            NoDefinitionInSpringContextTestBean.class,
            NativeLifecycleOptions.INSTANCE, fallbackProducer
    );

    assertThat(fallbackProducer.currentUnnamedInstantiationCount()).isEqualTo(1);
    assertThat(fallbackProducer.currentNamedInstantiationCount()).isEqualTo(0);

    assertThat(bean).isNotNull();
    NoDefinitionInSpringContextTestBean instance = bean.getBeanInstance();
    assertThat(instance).isNotNull();
    assertThat(instance.getSource()).isEqualTo(BeanSource.FALLBACK);
    assertThat(instance.getApplicationContext()).isNull();
  }

  @Test
  public void testCanRetrieveFallbackBeanByNameWithNativeOptions() {
    BeanContainer beanContainer = getBeanContainer();
    assertThat(beanContainer).isNotNull();
    NoDefinitionInSpringContextTestBeanInstanceProducer fallbackProducer = new NoDefinitionInSpringContextTestBeanInstanceProducer();

    ContainedBean<NoDefinitionInSpringContextTestBean> bean = beanContainer.getBean(
            "some name", NoDefinitionInSpringContextTestBean.class,
            NativeLifecycleOptions.INSTANCE, fallbackProducer
    );

    assertThat(fallbackProducer.currentUnnamedInstantiationCount()).isEqualTo(0);
    assertThat(fallbackProducer.currentNamedInstantiationCount()).isEqualTo(1);

    assertThat(bean).isNotNull();
    NoDefinitionInSpringContextTestBean instance = bean.getBeanInstance();
    assertThat(instance).isNotNull();
    assertThat(instance.getSource()).isEqualTo(BeanSource.FALLBACK);
    assertThat(instance.getName()).isEqualTo("some name");
    assertThat(instance.getApplicationContext()).isNull();
  }

  @Test
  public void testFallbackExceptionInCaseOfNoSpringBeanFound() {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            getBeanContainer().getBean(NoDefinitionInSpringContextTestBean.class,
                    NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
            ));
  }

  @Test
  public void testOriginalExceptionInCaseOfFallbackProducerFailure() {
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            getBeanContainer().getBean(AttributeConverter.class,
                    NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
            ));
  }

  @Test
  public void testFallbackExceptionInCaseOfNoSpringBeanFoundByName() {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            getBeanContainer().getBean("some name", NoDefinitionInSpringContextTestBean.class,
                    NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
            ));
  }

  @Test
  public void testOriginalExceptionInCaseOfFallbackProducerFailureByName() {
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            getBeanContainer().getBean("invalid", AttributeConverter.class,
                    NativeLifecycleOptions.INSTANCE, IneffectiveBeanInstanceProducer.INSTANCE
            ));
  }

  /**
   * The lifecycle options mandated by the JPA spec and used as a default in Hibernate ORM.
   */
  private static class JpaLifecycleOptions implements BeanContainer.LifecycleOptions {

    public static final JpaLifecycleOptions INSTANCE = new JpaLifecycleOptions();

    @Override
    public boolean canUseCachedReferences() {
      return true;
    }

    @Override
    public boolean useJpaCompliantCreation() {
      return true;
    }
  }

  /**
   * The lifecycle options used by libraries integrating into Hibernate ORM
   * and that want a behavior closer to Spring's native behavior,
   * such as Hibernate Search.
   */
  private static class NativeLifecycleOptions implements BeanContainer.LifecycleOptions {

    public static final NativeLifecycleOptions INSTANCE = new NativeLifecycleOptions();

    @Override
    public boolean canUseCachedReferences() {
      return false;
    }

    @Override
    public boolean useJpaCompliantCreation() {
      return false;
    }
  }

  private static class IneffectiveBeanInstanceProducer implements BeanInstanceProducer {

    public static final IneffectiveBeanInstanceProducer INSTANCE = new IneffectiveBeanInstanceProducer();

    @Override
    public <B> B produceBeanInstance(Class<B> aClass) {
      throw new UnsupportedOperationException("should not be called");
    }

    @Override
    public <B> B produceBeanInstance(String s, Class<B> aClass) {
      throw new UnsupportedOperationException("should not be called");
    }
  }

  private static class NoDefinitionInSpringContextTestBeanInstanceProducer implements BeanInstanceProducer {

    private int unnamedInstantiationCount = 0;

    private int namedInstantiationCount = 0;

    @Override
    public <B> B produceBeanInstance(Class<B> beanType) {
      try {
        ++unnamedInstantiationCount;
        /*
         * We only expect to ever be asked to instantiate this class, so we just cut corners here.
         * A real-world implementation would obviously be different.
         */
        NoDefinitionInSpringContextTestBean instance = new NoDefinitionInSpringContextTestBean(null, BeanSource.FALLBACK);
        return beanType.cast(instance);
      }
      catch (RuntimeException e) {
        throw new AssertionError("Unexpected error instantiating a bean by type using reflection", e);
      }
    }

    @Override
    public <B> B produceBeanInstance(String name, Class<B> beanType) {
      try {
        ++namedInstantiationCount;
        /*
         * We only expect to ever be asked to instantiate this class, so we just cut corners here.
         * A real-world implementation would obviously be different.
         */
        NoDefinitionInSpringContextTestBean instance = new NoDefinitionInSpringContextTestBean(name, BeanSource.FALLBACK);
        return beanType.cast(instance);
      }
      catch (RuntimeException e) {
        throw new AssertionError("Unexpected error instantiating a bean by name using reflection", e);
      }
    }

    private int currentUnnamedInstantiationCount() {
      return unnamedInstantiationCount;
    }

    private int currentNamedInstantiationCount() {
      return namedInstantiationCount;
    }
  }

}
