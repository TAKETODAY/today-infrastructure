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

package cn.taketoday.aop.target;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.beans.testfixture.SerializationTestUtils;
import cn.taketoday.beans.testfixture.beans.Person;
import cn.taketoday.beans.testfixture.beans.SerializablePerson;
import cn.taketoday.beans.testfixture.beans.SideEffectBean;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for pooling invoker interceptor.
 *
 * TODO: need to make these tests stronger: it's hard to
 * make too many assumptions about a pool.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 * @author Stephane Nicoll
 */
public class CommonsPool2TargetSourceTests {

  /**
   * Initial count value set in bean factory XML
   */
  private static final int INITIAL_COUNT = 10;

  private StandardBeanFactory beanFactory;

  @BeforeEach
  void setUp() throws Exception {
    this.beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
            new ClassPathResource(getClass().getSimpleName() + "-context.xml", getClass()));
  }

  /**
   * We must simulate container shutdown, which should clear threads.
   */
  @AfterEach
  void tearDown() {
    // Will call pool.close()
    this.beanFactory.destroySingletons();
  }

  private void testFunctionality(String name) {
    SideEffectBean pooled = (SideEffectBean) beanFactory.getBean(name);
    assertThat(pooled.getCount()).isEqualTo(INITIAL_COUNT);
    pooled.doWork();
    assertThat(pooled.getCount()).isEqualTo((INITIAL_COUNT + 1));

    pooled = (SideEffectBean) beanFactory.getBean(name);
    // Just check that it works--we can't make assumptions
    // about the count
    pooled.doWork();
    //assertEquals(INITIAL_COUNT + 1, apartment.getCount());
  }

  @Test
  void testFunctionality() {
    testFunctionality("pooled");
  }

  @Test
  void testFunctionalityWithNoInterceptors() {
    testFunctionality("pooledNoInterceptors");
  }

  @Test
  void testConfigMixin() {
    SideEffectBean pooled = (SideEffectBean) beanFactory.getBean("pooledWithMixin");
    assertThat(pooled.getCount()).isEqualTo(INITIAL_COUNT);
    PoolingConfig conf = (PoolingConfig) beanFactory.getBean("pooledWithMixin");
    // TODO one invocation from setup
    //assertEquals(1, conf.getInvocations());
    pooled.doWork();
    //	assertEquals("No objects active", 0, conf.getActive());
    assertThat(conf.getMaxSize()).as("Correct target source").isEqualTo(25);
    //	assertTrue("Some free", conf.getFree() > 0);
    //assertEquals(2, conf.getInvocations());
    assertThat(conf.getMaxSize()).isEqualTo(25);
  }

  @Test
  void testTargetSourceSerializableWithoutConfigMixin() throws Exception {
    CommonsPool2TargetSource cpts = (CommonsPool2TargetSource) beanFactory.getBean("personPoolTargetSource");

    SingletonTargetSource serialized = SerializationTestUtils.serializeAndDeserialize(cpts, SingletonTargetSource.class);
    assertThat(serialized.getTarget()).isInstanceOf(Person.class);
  }

  @Test
  void testProxySerializableWithoutConfigMixin() throws Exception {
    Person pooled = (Person) beanFactory.getBean("pooledPerson");

    boolean condition1 = ((Advised) pooled).getTargetSource() instanceof CommonsPool2TargetSource;
    assertThat(condition1).isTrue();

    //((Advised) pooled).setTargetSource(new SingletonTargetSource(new SerializablePerson()));
    Person serialized = SerializationTestUtils.serializeAndDeserialize(pooled);
    boolean condition = ((Advised) serialized).getTargetSource() instanceof SingletonTargetSource;
    assertThat(condition).isTrue();
    serialized.setAge(25);
    assertThat(serialized.getAge()).isEqualTo(25);
  }

  @Test
  void testHitMaxSize() throws Exception {
    int maxSize = 10;

    CommonsPool2TargetSource targetSource = new CommonsPool2TargetSource();
    targetSource.setMaxSize(maxSize);
    targetSource.setMaxWait(1);
    prepareTargetSource(targetSource);

    Object[] pooledInstances = new Object[maxSize];

    for (int x = 0; x < maxSize; x++) {
      Object instance = targetSource.getTarget();
      assertThat(instance).isNotNull();
      pooledInstances[x] = instance;
    }

    // should be at maximum now
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
            targetSource::getTarget);

    // lets now release an object and try to acquire a new one
    targetSource.releaseTarget(pooledInstances[9]);
    pooledInstances[9] = targetSource.getTarget();

    // release all objects
    for (Object element : pooledInstances) {
      targetSource.releaseTarget(element);
    }
  }

  @Test
  void testHitMaxSizeLoadedFromContext() throws Exception {
    Advised person = (Advised) beanFactory.getBean("maxSizePooledPerson");
    CommonsPool2TargetSource targetSource = (CommonsPool2TargetSource) person.getTargetSource();

    int maxSize = targetSource.getMaxSize();
    Object[] pooledInstances = new Object[maxSize];

    for (int x = 0; x < maxSize; x++) {
      Object instance = targetSource.getTarget();
      assertThat(instance).isNotNull();
      pooledInstances[x] = instance;
    }

    // should be at maximum now
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
            targetSource::getTarget);

    // lets now release an object and try to acquire a new one
    targetSource.releaseTarget(pooledInstances[9]);
    pooledInstances[9] = targetSource.getTarget();

    // release all objects
    for (int i = 0; i < pooledInstances.length; i++) {
      System.out.println(i);
      targetSource.releaseTarget(pooledInstances[i]);
    }
  }

  @Test
  void testSetWhenExhaustedAction() {
    CommonsPool2TargetSource targetSource = new CommonsPool2TargetSource();
    targetSource.setBlockWhenExhausted(true);
    assertThat(targetSource.isBlockWhenExhausted()).isTrue();
  }

  @Test
  void referenceIdentityByDefault() throws Exception {
    CommonsPool2TargetSource targetSource = new CommonsPool2TargetSource();
    targetSource.setMaxWait(1);
    prepareTargetSource(targetSource);

    Object first = targetSource.getTarget();
    Object second = targetSource.getTarget();
    boolean condition1 = first instanceof SerializablePerson;
    assertThat(condition1).isTrue();
    boolean condition = second instanceof SerializablePerson;
    assertThat(condition).isTrue();
    assertThat(second).isEqualTo(first);

    targetSource.releaseTarget(first);
    targetSource.releaseTarget(second);
  }

  private void prepareTargetSource(CommonsPool2TargetSource targetSource) {
    String beanName = "target";

    StaticApplicationContext applicationContext = new StaticApplicationContext();
    applicationContext.registerPrototype(beanName, SerializablePerson.class);

    targetSource.setTargetBeanName(beanName);
    targetSource.setBeanFactory(applicationContext);
  }

}
