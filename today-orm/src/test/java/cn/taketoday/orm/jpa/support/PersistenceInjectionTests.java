/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.orm.jpa.support;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.testfixture.SimpleMapScope;
import cn.taketoday.context.testfixture.jndi.ExpectedLookupTemplate;
import cn.taketoday.core.testfixture.io.SerializationTestUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.jpa.AbstractEntityManagerFactoryBeanTests;
import cn.taketoday.orm.jpa.DefaultJpaDialect;
import cn.taketoday.orm.jpa.EntityManagerFactoryInfo;
import cn.taketoday.orm.jpa.EntityManagerHolder;
import cn.taketoday.stereotype.Repository;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.PersistenceProperty;
import jakarta.persistence.PersistenceUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for persistence context and persistence unit injection.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
public class PersistenceInjectionTests extends AbstractEntityManagerFactoryBeanTests {

  @Test
  public void testPrivatePersistenceContextField() throws Exception {
    mockEmf = mock(EntityManagerFactory.class, withSettings().serializable());
    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    gac.registerBeanDefinition(DefaultPrivatePersistenceContextField.class.getName(),
            new RootBeanDefinition(DefaultPrivatePersistenceContextField.class));
    gac.registerBeanDefinition(FactoryBeanWithPersistenceContextField.class.getName(),
            new RootBeanDefinition(FactoryBeanWithPersistenceContextField.class));
    gac.refresh();

    DefaultPrivatePersistenceContextField bean = (DefaultPrivatePersistenceContextField) gac.getBean(
            DefaultPrivatePersistenceContextField.class.getName());
    FactoryBeanWithPersistenceContextField bean2 = (FactoryBeanWithPersistenceContextField) gac.getBean(
            "&" + FactoryBeanWithPersistenceContextField.class.getName());
    assertThat(bean.em).isNotNull();
    assertThat(bean2.em).isNotNull();

    assertThat(SerializationTestUtils.serializeAndDeserialize(bean.em)).isNotNull();
    assertThat(SerializationTestUtils.serializeAndDeserialize(bean2.em)).isNotNull();
  }

  @Test
  public void testPrivateVendorSpecificPersistenceContextField() {
    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    gac.registerBeanDefinition(DefaultVendorSpecificPrivatePersistenceContextField.class.getName(),
            new RootBeanDefinition(DefaultVendorSpecificPrivatePersistenceContextField.class));
    gac.refresh();

    DefaultVendorSpecificPrivatePersistenceContextField bean = (DefaultVendorSpecificPrivatePersistenceContextField)
            gac.getBean(DefaultVendorSpecificPrivatePersistenceContextField.class.getName());
    assertThat(bean.em).isNotNull();
  }

  @Test
  public void testPublicExtendedPersistenceContextSetter() {
    EntityManager mockEm = mock(EntityManager.class);
    given(mockEmf.createEntityManager()).willReturn(mockEm);

    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    gac.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));
    gac.refresh();

    DefaultPublicPersistenceContextSetter bean = (DefaultPublicPersistenceContextSetter) gac.getBean(
            DefaultPublicPersistenceContextSetter.class.getName());
    assertThat(bean.em).isNotNull();
  }

  @Test
  public void testPublicSpecificExtendedPersistenceContextSetter() {
    EntityManagerFactory mockEmf2 = mock(EntityManagerFactory.class);
    EntityManager mockEm2 = mock(EntityManager.class);
    given(mockEmf2.createEntityManager()).willReturn(mockEm2);

    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.getBeanFactory().registerSingleton("unit2", mockEmf2);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    gac.registerBeanDefinition(SpecificPublicPersistenceContextSetter.class.getName(),
            new RootBeanDefinition(SpecificPublicPersistenceContextSetter.class));
    gac.refresh();

    SpecificPublicPersistenceContextSetter bean = (SpecificPublicPersistenceContextSetter) gac.getBean(
            SpecificPublicPersistenceContextSetter.class.getName());
    assertThat(bean.getEntityManager()).isNotNull();
    bean.getEntityManager().flush();
    verify(mockEm2).getTransaction();
    verify(mockEm2).flush();
  }

  @Test
  public void testInjectionIntoExistingObjects() {
    EntityManager mockEm = mock(EntityManager.class);
    given(mockEmf.createEntityManager()).willReturn(mockEm);

    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    gac.refresh();

    DefaultPrivatePersistenceContextField existingBean1 = new DefaultPrivatePersistenceContextField();
    gac.getAutowireCapableBeanFactory().autowireBean(existingBean1);
    assertThat(existingBean1.em).isNotNull();

    DefaultPublicPersistenceContextSetter existingBean2 = new DefaultPublicPersistenceContextSetter();
    gac.getAutowireCapableBeanFactory().autowireBean(existingBean2);
    assertThat(existingBean2.em).isNotNull();
  }

  @Test
  public void testPublicExtendedPersistenceContextSetterWithSerialization() throws Exception {
    DummyInvocationHandler ih = new DummyInvocationHandler();
    Object mockEm = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { EntityManager.class }, ih);
    given(mockEmf.createEntityManager()).willReturn((EntityManager) mockEm);

    GenericApplicationContext gac = new GenericApplicationContext();
    SimpleMapScope myScope = new SimpleMapScope();
    gac.getBeanFactory().registerScope("myScope", myScope);
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    RootBeanDefinition bd = new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class);
    bd.setScope("myScope");
    gac.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(), bd);
    gac.refresh();

    DefaultPublicPersistenceContextSetter bean = (DefaultPublicPersistenceContextSetter) gac.getBean(
            DefaultPublicPersistenceContextSetter.class.getName());
    assertThat(bean.em).isNotNull();
    assertThat(SerializationTestUtils.serializeAndDeserialize(bean.em)).isNotNull();

    SimpleMapScope serialized = SerializationTestUtils.serializeAndDeserialize(myScope);
    serialized.close();
    assertThat(DummyInvocationHandler.closed).isTrue();
    DummyInvocationHandler.closed = false;
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testPublicExtendedPersistenceContextSetterWithEntityManagerInfoAndSerialization() throws Exception {
    EntityManager mockEm = mock(EntityManager.class, withSettings().serializable());
    given(mockEm.isOpen()).willReturn(true);
    EntityManagerFactoryWithInfo mockEmf = mock(EntityManagerFactoryWithInfo.class);
    given(mockEmf.getJpaDialect()).willReturn(new DefaultJpaDialect());
    given(mockEmf.getEntityManagerInterface()).willReturn((Class) EntityManager.class);
    given(mockEmf.getBeanClassLoader()).willReturn(getClass().getClassLoader());
    given(mockEmf.createNativeEntityManager(null)).willReturn(mockEm);

    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    gac.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));
    gac.refresh();

    DefaultPublicPersistenceContextSetter bean = (DefaultPublicPersistenceContextSetter) gac.getBean(
            DefaultPublicPersistenceContextSetter.class.getName());
    assertThat(bean.em).isNotNull();
    assertThat(SerializationTestUtils.serializeAndDeserialize(bean.em)).isNotNull();
  }

  @Test
  public void testPublicExtendedPersistenceContextSetterWithOverriding() {
    EntityManager mockEm2 = mock(EntityManager.class);

    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    RootBeanDefinition bd = new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class);
    bd.getPropertyValues().add("entityManager", mockEm2);
    gac.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(), bd);
    gac.refresh();

    DefaultPublicPersistenceContextSetter bean = (DefaultPublicPersistenceContextSetter) gac.getBean(
            DefaultPublicPersistenceContextSetter.class.getName());
    assertThat(bean.em).isSameAs(mockEm2);
  }

  @Test
  public void testPrivatePersistenceUnitField() {
    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    gac.registerBeanDefinition(DefaultPrivatePersistenceUnitField.class.getName(),
            new RootBeanDefinition(DefaultPrivatePersistenceUnitField.class));
    gac.refresh();

    DefaultPrivatePersistenceUnitField bean = (DefaultPrivatePersistenceUnitField) gac.getBean(
            DefaultPrivatePersistenceUnitField.class.getName());
    assertThat(bean.emf).isSameAs(mockEmf);
  }

  @Test
  public void testPublicPersistenceUnitSetter() {
    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
    gac.refresh();

    DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter) gac.getBean(
            DefaultPublicPersistenceUnitSetter.class.getName());
    assertThat(bean.emf).isSameAs(mockEmf);
  }

  @Test
  public void testPublicPersistenceUnitSetterWithOverriding() {
    EntityManagerFactory mockEmf2 = mock(EntityManagerFactory.class);

    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.registerBeanDefinition("annotationProcessor",
            new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class));
    RootBeanDefinition bd = new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class);
    bd.getPropertyValues().add("emf", mockEmf2);
    gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(), bd);
    gac.refresh();

    DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter) gac.getBean(
            DefaultPublicPersistenceUnitSetter.class.getName());
    assertThat(bean.emf).isSameAs(mockEmf2);
  }

  @Test
  public void testPublicPersistenceUnitSetterWithUnitIdentifiedThroughBeanName() {
    EntityManagerFactory mockEmf2 = mock(EntityManagerFactory.class);

    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.getBeanFactory().registerSingleton("entityManagerFactory2", mockEmf2);
    gac.registerAlias("entityManagerFactory2", "Person");
    RootBeanDefinition processorDef = new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class);
    processorDef.getPropertyValues().add("defaultPersistenceUnitName", "entityManagerFactory");
    gac.registerBeanDefinition("annotationProcessor", processorDef);
    gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
    gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class));
    gac.refresh();

    DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter)
            gac.getBean(DefaultPublicPersistenceUnitSetter.class.getName());
    DefaultPublicPersistenceUnitSetterNamedPerson bean2 = (DefaultPublicPersistenceUnitSetterNamedPerson)
            gac.getBean(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName());
    assertThat(bean.emf).isSameAs(mockEmf);
    assertThat(bean2.emf).isSameAs(mockEmf2);
  }

  @Test
  public void testPublicPersistenceUnitSetterWithMultipleUnitsIdentifiedThroughUnitName() {
    EntityManagerFactoryWithInfo mockEmf2 = mock(EntityManagerFactoryWithInfo.class);
    given(mockEmf2.getPersistenceUnitName()).willReturn("Person");

    GenericApplicationContext gac = new GenericApplicationContext();
    gac.getBeanFactory().registerSingleton("entityManagerFactory", mockEmf);
    gac.getBeanFactory().registerSingleton("entityManagerFactory2", mockEmf2);
    RootBeanDefinition processorDef = new RootBeanDefinition(PersistenceAnnotationBeanPostProcessor.class);
    processorDef.getPropertyValues().add("defaultPersistenceUnitName", "entityManagerFactory");
    gac.registerBeanDefinition("annotationProcessor", processorDef);
    gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
    gac.registerBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class));
    gac.refresh();

    DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter)
            gac.getBean(DefaultPublicPersistenceUnitSetter.class.getName());
    DefaultPublicPersistenceUnitSetterNamedPerson bean2 = (DefaultPublicPersistenceUnitSetterNamedPerson)
            gac.getBean(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName());
    assertThat(bean.emf).isSameAs(mockEmf);
    assertThat(bean2.emf).isSameAs(mockEmf2);
  }

  @Test
  public void testPersistenceUnitsFromJndi() {
    EntityManager mockEm = mock(EntityManager.class);
    given(mockEmf.createEntityManager()).willReturn(mockEm);

    EntityManagerFactoryWithInfo mockEmf2 = mock(EntityManagerFactoryWithInfo.class);

    Map<String, String> persistenceUnits = new HashMap<>();
    persistenceUnits.put("", "pu1");
    persistenceUnits.put("Person", "pu2");
    ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
    jt.addObject("java:comp/env/pu1", mockEmf);
    jt.addObject("java:comp/env/pu2", mockEmf2);

    StandardBeanFactory bf = new StandardBeanFactory();
    PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
    bpp.setPersistenceUnits(persistenceUnits);
    bpp.setJndiTemplate(jt);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
    bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class));
    bf.registerBeanDefinition(DefaultPrivatePersistenceContextField.class.getName(),
            new RootBeanDefinition(DefaultPrivatePersistenceContextField.class));
    bf.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));

    DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter)
            bf.getBean(DefaultPublicPersistenceUnitSetter.class.getName());
    DefaultPublicPersistenceUnitSetterNamedPerson bean2 = (DefaultPublicPersistenceUnitSetterNamedPerson)
            bf.getBean(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName());
    DefaultPrivatePersistenceContextField bean3 = (DefaultPrivatePersistenceContextField)
            bf.getBean(DefaultPrivatePersistenceContextField.class.getName());
    DefaultPublicPersistenceContextSetter bean4 = (DefaultPublicPersistenceContextSetter)
            bf.getBean(DefaultPublicPersistenceContextSetter.class.getName());
    assertThat(bean.emf).isSameAs(mockEmf);
    assertThat(bean2.emf).isSameAs(mockEmf2);
    assertThat(bean3.em).isNotNull();
    assertThat(bean4.em).isNotNull();
  }

  @Test
  public void testPersistenceUnitsFromJndiWithDefaultUnit() {
    EntityManagerFactoryWithInfo mockEmf2 = mock(EntityManagerFactoryWithInfo.class);

    Map<String, String> persistenceUnits = new HashMap<>();
    persistenceUnits.put("System", "pu1");
    persistenceUnits.put("Person", "pu2");
    ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
    jt.addObject("java:comp/env/pu1", mockEmf);
    jt.addObject("java:comp/env/pu2", mockEmf2);

    StandardBeanFactory bf = new StandardBeanFactory();
    PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
    bpp.setPersistenceUnits(persistenceUnits);
    bpp.setDefaultPersistenceUnitName("System");
    bpp.setJndiTemplate(jt);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
    bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class));

    DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter)
            bf.getBean(DefaultPublicPersistenceUnitSetter.class.getName());
    DefaultPublicPersistenceUnitSetterNamedPerson bean2 = (DefaultPublicPersistenceUnitSetterNamedPerson)
            bf.getBean(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName());
    assertThat(bean.emf).isSameAs(mockEmf);
    assertThat(bean2.emf).isSameAs(mockEmf2);
  }

  @Test
  public void testSinglePersistenceUnitFromJndi() {
    Map<String, String> persistenceUnits = new HashMap<>();
    persistenceUnits.put("Person", "pu1");
    ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
    jt.addObject("java:comp/env/pu1", mockEmf);

    StandardBeanFactory bf = new StandardBeanFactory();
    PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
    bpp.setPersistenceUnits(persistenceUnits);
    bpp.setJndiTemplate(jt);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetter.class));
    bf.registerBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceUnitSetterNamedPerson.class));

    DefaultPublicPersistenceUnitSetter bean = (DefaultPublicPersistenceUnitSetter)
            bf.getBean(DefaultPublicPersistenceUnitSetter.class.getName());
    DefaultPublicPersistenceUnitSetterNamedPerson bean2 = (DefaultPublicPersistenceUnitSetterNamedPerson)
            bf.getBean(DefaultPublicPersistenceUnitSetterNamedPerson.class.getName());
    assertThat(bean.emf).isSameAs(mockEmf);
    assertThat(bean2.emf).isSameAs(mockEmf);
  }

  @Test
  public void testPersistenceContextsFromJndi() {
    EntityManager mockEm = mock(EntityManager.class);
    EntityManager mockEm2 = mock(EntityManager.class);
    EntityManager mockEm3 = mock(EntityManager.class);

    Map<String, String> persistenceContexts = new HashMap<>();
    persistenceContexts.put("", "pc1");
    persistenceContexts.put("Person", "pc2");
    Map<String, String> extendedPersistenceContexts = new HashMap<>();
    extendedPersistenceContexts.put("", "pc3");
    ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
    jt.addObject("java:comp/env/pc1", mockEm);
    jt.addObject("java:comp/env/pc2", mockEm2);
    jt.addObject("java:comp/env/pc3", mockEm3);

    StandardBeanFactory bf = new StandardBeanFactory();
    PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
    bpp.setPersistenceContexts(persistenceContexts);
    bpp.setExtendedPersistenceContexts(extendedPersistenceContexts);
    bpp.setJndiTemplate(jt);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition(DefaultPrivatePersistenceContextField.class.getName(),
            new RootBeanDefinition(DefaultPrivatePersistenceContextField.class));
    bf.registerBeanDefinition(DefaultPrivatePersistenceContextFieldNamedPerson.class.getName(),
            new RootBeanDefinition(DefaultPrivatePersistenceContextFieldNamedPerson.class));
    bf.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));

    DefaultPrivatePersistenceContextField bean1 = (DefaultPrivatePersistenceContextField)
            bf.getBean(DefaultPrivatePersistenceContextField.class.getName());
    DefaultPrivatePersistenceContextFieldNamedPerson bean2 = (DefaultPrivatePersistenceContextFieldNamedPerson)
            bf.getBean(DefaultPrivatePersistenceContextFieldNamedPerson.class.getName());
    DefaultPublicPersistenceContextSetter bean3 = (DefaultPublicPersistenceContextSetter)
            bf.getBean(DefaultPublicPersistenceContextSetter.class.getName());
    assertThat(bean1.em).isSameAs(mockEm);
    assertThat(bean2.em).isSameAs(mockEm2);
    assertThat(bean3.em).isSameAs(mockEm3);
  }

  @Test
  public void testPersistenceContextsFromJndiWithDefaultUnit() {
    EntityManager mockEm = mock(EntityManager.class);
    EntityManager mockEm2 = mock(EntityManager.class);
    EntityManager mockEm3 = mock(EntityManager.class);

    Map<String, String> persistenceContexts = new HashMap<>();
    persistenceContexts.put("System", "pc1");
    persistenceContexts.put("Person", "pc2");
    Map<String, String> extendedPersistenceContexts = new HashMap<>();
    extendedPersistenceContexts.put("System", "pc3");
    ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
    jt.addObject("java:comp/env/pc1", mockEm);
    jt.addObject("java:comp/env/pc2", mockEm2);
    jt.addObject("java:comp/env/pc3", mockEm3);

    StandardBeanFactory bf = new StandardBeanFactory();
    PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
    bpp.setPersistenceContexts(persistenceContexts);
    bpp.setExtendedPersistenceContexts(extendedPersistenceContexts);
    bpp.setDefaultPersistenceUnitName("System");
    bpp.setJndiTemplate(jt);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition(DefaultPrivatePersistenceContextField.class.getName(),
            new RootBeanDefinition(DefaultPrivatePersistenceContextField.class));
    bf.registerBeanDefinition(DefaultPrivatePersistenceContextFieldNamedPerson.class.getName(),
            new RootBeanDefinition(DefaultPrivatePersistenceContextFieldNamedPerson.class));
    bf.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));

    DefaultPrivatePersistenceContextField bean1 = (DefaultPrivatePersistenceContextField)
            bf.getBean(DefaultPrivatePersistenceContextField.class.getName());
    DefaultPrivatePersistenceContextFieldNamedPerson bean2 = (DefaultPrivatePersistenceContextFieldNamedPerson)
            bf.getBean(DefaultPrivatePersistenceContextFieldNamedPerson.class.getName());
    DefaultPublicPersistenceContextSetter bean3 = (DefaultPublicPersistenceContextSetter)
            bf.getBean(DefaultPublicPersistenceContextSetter.class.getName());
    assertThat(bean1.em).isSameAs(mockEm);
    assertThat(bean2.em).isSameAs(mockEm2);
    assertThat(bean3.em).isSameAs(mockEm3);
  }

  @Test
  public void testSinglePersistenceContextFromJndi() {
    EntityManager mockEm = mock(EntityManager.class);
    EntityManager mockEm2 = mock(EntityManager.class);

    Map<String, String> persistenceContexts = new HashMap<>();
    persistenceContexts.put("System", "pc1");
    Map<String, String> extendedPersistenceContexts = new HashMap<>();
    extendedPersistenceContexts.put("System", "pc2");
    ExpectedLookupTemplate jt = new ExpectedLookupTemplate();
    jt.addObject("java:comp/env/pc1", mockEm);
    jt.addObject("java:comp/env/pc2", mockEm2);

    StandardBeanFactory bf = new StandardBeanFactory();
    PersistenceAnnotationBeanPostProcessor bpp = new PersistenceAnnotationBeanPostProcessor();
    bpp.setPersistenceContexts(persistenceContexts);
    bpp.setExtendedPersistenceContexts(extendedPersistenceContexts);
    bpp.setJndiTemplate(jt);
    bf.addBeanPostProcessor(bpp);
    bf.registerBeanDefinition(DefaultPrivatePersistenceContextField.class.getName(),
            new RootBeanDefinition(DefaultPrivatePersistenceContextField.class));
    bf.registerBeanDefinition(DefaultPublicPersistenceContextSetter.class.getName(),
            new RootBeanDefinition(DefaultPublicPersistenceContextSetter.class));

    DefaultPrivatePersistenceContextField bean1 = (DefaultPrivatePersistenceContextField)
            bf.getBean(DefaultPrivatePersistenceContextField.class.getName());
    DefaultPublicPersistenceContextSetter bean2 = (DefaultPublicPersistenceContextSetter)
            bf.getBean(DefaultPublicPersistenceContextSetter.class.getName());
    assertThat(bean1.em).isSameAs(mockEm);
    assertThat(bean2.em).isSameAs(mockEm2);
  }

  @Test
  public void testFieldOfWrongTypeAnnotatedWithPersistenceUnit() {
    PersistenceAnnotationBeanPostProcessor pabpp = new PersistenceAnnotationBeanPostProcessor();
    assertThatIllegalStateException().isThrownBy(() ->
            pabpp.processDependencies(null, new FieldOfWrongTypeAnnotatedWithPersistenceUnit(), "bean"));
  }

  @Test
  public void testSetterOfWrongTypeAnnotatedWithPersistenceUnit() {
    PersistenceAnnotationBeanPostProcessor pabpp = new PersistenceAnnotationBeanPostProcessor();
    assertThatIllegalStateException().isThrownBy(() ->
            pabpp.processDependencies(null, new SetterOfWrongTypeAnnotatedWithPersistenceUnit(), "bean"));
  }

  @Test
  public void testSetterWithNoArgs() {
    PersistenceAnnotationBeanPostProcessor pabpp = new PersistenceAnnotationBeanPostProcessor();
    assertThatIllegalStateException().isThrownBy(() ->
            pabpp.processDependencies(null, new SetterWithNoArgs(), "bean"));
  }

  @Test
  public void testNoPropertiesPassedIn() {
    EntityManager mockEm = mock(EntityManager.class);
    given(mockEmf.createEntityManager()).willReturn(mockEm);

    PersistenceAnnotationBeanPostProcessor pabpp = new MockPersistenceAnnotationBeanPostProcessor();
    DefaultPrivatePersistenceContextFieldExtended dppcf = new DefaultPrivatePersistenceContextFieldExtended();
    pabpp.processDependencies(null, dppcf, "bean");
    assertThat(dppcf.em).isNotNull();
  }

  @Test
  public void testPropertiesPassedIn() {
    Properties props = new Properties();
    props.put("foo", "bar");
    EntityManager mockEm = mock(EntityManager.class);
    given(mockEmf.createEntityManager(props)).willReturn(mockEm);

    PersistenceAnnotationBeanPostProcessor pabpp = new MockPersistenceAnnotationBeanPostProcessor();
    DefaultPrivatePersistenceContextFieldExtendedWithProps dppcf =
            new DefaultPrivatePersistenceContextFieldExtendedWithProps();
    pabpp.processDependencies(null, dppcf, "bean");
    assertThat(dppcf.em).isNotNull();
  }

  @Test
  public void testPropertiesForTransactionalEntityManager() {
    Properties props = new Properties();
    props.put("foo", "bar");
    EntityManager em = mock(EntityManager.class);
    given(mockEmf.createEntityManager(props)).willReturn(em);
    given(em.getDelegate()).willReturn(new Object());
    given(em.isOpen()).willReturn(true);

    PersistenceAnnotationBeanPostProcessor pabpp = new MockPersistenceAnnotationBeanPostProcessor();
    DefaultPrivatePersistenceContextFieldWithProperties transactionalField =
            new DefaultPrivatePersistenceContextFieldWithProperties();
    pabpp.processDependencies(null, transactionalField, "bean");

    assertThat(transactionalField.em).isNotNull();
    assertThat(transactionalField.em.getDelegate()).isNotNull();

    verify(em).close();
  }

  /**
   * Binds an EMF to the thread and tests if EM with different properties
   * generate new EMs or not.
   */
  @Test
  public void testPropertiesForSharedEntityManager1() {
    Properties props = new Properties();
    props.put("foo", "bar");
    EntityManager em = mock(EntityManager.class);
    // only one call made  - the first EM definition wins (in this case the one w/ the properties)
    given(mockEmf.createEntityManager(props)).willReturn(em);
    given(em.getDelegate()).willReturn(new Object());
    given(em.isOpen()).willReturn(true);

    PersistenceAnnotationBeanPostProcessor pabpp = new MockPersistenceAnnotationBeanPostProcessor();
    DefaultPrivatePersistenceContextFieldWithProperties transactionalFieldWithProperties =
            new DefaultPrivatePersistenceContextFieldWithProperties();
    DefaultPrivatePersistenceContextField transactionalField = new DefaultPrivatePersistenceContextField();

    pabpp.processDependencies(null, transactionalFieldWithProperties, "bean1");
    pabpp.processDependencies(null, transactionalField, "bean2");

    assertThat(transactionalFieldWithProperties.em).isNotNull();
    assertThat(transactionalField.em).isNotNull();
    // the EM w/ properties will be created
    assertThat(transactionalFieldWithProperties.em.getDelegate()).isNotNull();
    // bind em to the thread now since it's created
    try {
      TransactionSynchronizationManager.bindResource(mockEmf, new EntityManagerHolder(em));
      assertThat(transactionalField.em.getDelegate()).isNotNull();
      verify(em).close();
    }
    finally {
      TransactionSynchronizationManager.unbindResource(mockEmf);
    }
  }

  @Test
  public void testPropertiesForSharedEntityManager2() {
    Properties props = new Properties();
    props.put("foo", "bar");
    EntityManager em = mock(EntityManager.class);
    // only one call made  - the first EM definition wins (in this case the one w/o the properties)
    given(mockEmf.createEntityManager()).willReturn(em);
    given(em.getDelegate()).willReturn(new Object(), 2);
    given(em.isOpen()).willReturn(true);

    PersistenceAnnotationBeanPostProcessor pabpp = new MockPersistenceAnnotationBeanPostProcessor();
    DefaultPrivatePersistenceContextFieldWithProperties transactionalFieldWithProperties =
            new DefaultPrivatePersistenceContextFieldWithProperties();
    DefaultPrivatePersistenceContextField transactionalField = new DefaultPrivatePersistenceContextField();

    pabpp.processDependencies(null, transactionalFieldWithProperties, "bean1");
    pabpp.processDependencies(null, transactionalField, "bean2");

    assertThat(transactionalFieldWithProperties.em).isNotNull();
    assertThat(transactionalField.em).isNotNull();
    // the EM w/o properties will be created
    assertThat(transactionalField.em.getDelegate()).isNotNull();
    // bind em to the thread now since it's created
    try {
      TransactionSynchronizationManager.bindResource(mockEmf, new EntityManagerHolder(em));
      assertThat(transactionalFieldWithProperties.em.getDelegate()).isNotNull();
      verify(em).close();
    }
    finally {
      TransactionSynchronizationManager.unbindResource(mockEmf);
    }
  }

  @SuppressWarnings("serial")
  private static class MockPersistenceAnnotationBeanPostProcessor extends PersistenceAnnotationBeanPostProcessor {

    @Override
    protected EntityManagerFactory findEntityManagerFactory(@Nullable String emfName, String requestingBeanName) {
      return mockEmf;
    }
  }

  public static class DefaultPrivatePersistenceContextField {

    @PersistenceContext
    private EntityManager em;
  }

  public static class DefaultVendorSpecificPrivatePersistenceContextField {

    @PersistenceContext
    @SuppressWarnings("deprecation")
    private org.hibernate.ejb.HibernateEntityManager em;
  }

  @SuppressWarnings("rawtypes")
  public static class FactoryBeanWithPersistenceContextField implements FactoryBean {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Object getObject() throws Exception {
      return null;
    }

    @Override
    public Class getObjectType() {
      return null;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }
  }

  public static class DefaultPrivatePersistenceContextFieldNamedPerson {

    @PersistenceContext(unitName = "Person")
    private EntityManager em;
  }

  public static class DefaultPrivatePersistenceContextFieldWithProperties {

    @PersistenceContext(properties = { @PersistenceProperty(name = "foo", value = "bar") })
    private EntityManager em;
  }

  @Repository
  @SuppressWarnings("serial")
  public static class DefaultPublicPersistenceContextSetter implements Serializable {

    private EntityManager em;

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    public void setEntityManager(EntityManager em) {
      if (this.em != null) {
        throw new IllegalStateException("Already called");
      }
      this.em = em;
    }

    public EntityManager getEntityManager() {
      return em;
    }
  }

  @SuppressWarnings("serial")
  static class PublicPersistenceContextSetterOnNonPublicClass extends DefaultPublicPersistenceContextSetter {

    @Override
    @PersistenceContext(unitName = "unit2", type = PersistenceContextType.EXTENDED)
    public void setEntityManager(EntityManager em) {
      super.setEntityManager(em);
    }
  }

  @SuppressWarnings("serial")
  public static class SpecificPublicPersistenceContextSetter extends PublicPersistenceContextSetterOnNonPublicClass {
  }

  public static class DefaultPrivatePersistenceUnitField {

    @PersistenceUnit
    private EntityManagerFactory emf;
  }

  public static class DefaultPublicPersistenceUnitSetter {

    private EntityManagerFactory emf;

    @PersistenceUnit
    public void setEmf(EntityManagerFactory emf) {
      if (this.emf != null) {
        throw new IllegalStateException("Already called");
      }
      this.emf = emf;
    }

    public EntityManagerFactory getEmf() {
      return emf;
    }
  }

  @Repository
  public static class DefaultPublicPersistenceUnitSetterNamedPerson {

    private EntityManagerFactory emf;

    @PersistenceUnit(unitName = "Person")
    public void setEmf(EntityManagerFactory emf) {
      this.emf = emf;
    }

    public EntityManagerFactory getEntityManagerFactory() {
      return emf;
    }
  }

  public static class FieldOfWrongTypeAnnotatedWithPersistenceUnit {

    @PersistenceUnit
    public String thisFieldIsOfTheWrongType;
  }

  public static class SetterOfWrongTypeAnnotatedWithPersistenceUnit {

    @PersistenceUnit
    @SuppressWarnings("rawtypes")
    public void setSomething(Comparable c) {
    }
  }

  public static class SetterWithNoArgs {

    @PersistenceUnit
    public void setSomething() {
    }
  }

  public static class DefaultPrivatePersistenceContextFieldExtended {

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager em;
  }

  public static class DefaultPrivatePersistenceContextFieldExtendedWithProps {

    @PersistenceContext(type = PersistenceContextType.EXTENDED, properties = { @PersistenceProperty(name = "foo", value = "bar") })
    private EntityManager em;
  }

  private interface EntityManagerFactoryWithInfo extends EntityManagerFactory, EntityManagerFactoryInfo {

  }

  @SuppressWarnings("serial")
  private static class DummyInvocationHandler implements InvocationHandler, Serializable {

    public static boolean closed;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("isOpen".equals(method.getName())) {
        return true;
      }
      if ("close".equals(method.getName())) {
        closed = true;
        return null;
      }
      if ("toString".equals(method.getName())) {
        return "";
      }
      throw new IllegalStateException();
    }
  }

}
