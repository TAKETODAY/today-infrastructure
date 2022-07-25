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

package cn.taketoday.transaction.config;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.testfixture.CallCountingTransactionManager;
import cn.taketoday.transaction.testfixture.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class AnnotationDrivenTests {

  @Test
  public void withProxyTargetClass() throws Exception {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("annotationDrivenProxyTargetClassTests.xml", getClass());
    doTestWithMultipleTransactionManagers(context);
  }

  @Test
  public void withConfigurationClass() throws Exception {
    ApplicationContext parent = new AnnotationConfigApplicationContext(TransactionManagerConfiguration.class);
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "annotationDrivenConfigurationClassTests.xml" }, getClass(), parent);
    doTestWithMultipleTransactionManagers(context);
  }

  @Test
  public void withAnnotatedTransactionManagers() throws Exception {
    AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
    parent.registerBeanDefinition("transactionManager1", new RootBeanDefinition(SynchTransactionManager.class));
    parent.registerBeanDefinition("transactionManager2", new RootBeanDefinition(NoSynchTransactionManager.class));
    parent.refresh();
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "annotationDrivenConfigurationClassTests.xml" }, getClass(), parent);
    doTestWithMultipleTransactionManagers(context);
  }

  private void doTestWithMultipleTransactionManagers(ApplicationContext context) {
    CallCountingTransactionManager tm1 = context.getBean("transactionManager1", CallCountingTransactionManager.class);
    CallCountingTransactionManager tm2 = context.getBean("transactionManager2", CallCountingTransactionManager.class);
    TransactionalService service = context.getBean("service", TransactionalService.class);
    assertThat(AopUtils.isCglibProxy(service)).isTrue();
    service.setSomething("someName");
    assertThat(tm1.commits).isEqualTo(1);
    assertThat(tm2.commits).isEqualTo(0);
    service.doSomething();
    assertThat(tm1.commits).isEqualTo(1);
    assertThat(tm2.commits).isEqualTo(1);
    service.setSomething("someName");
    assertThat(tm1.commits).isEqualTo(2);
    assertThat(tm2.commits).isEqualTo(1);
    service.doSomething();
    assertThat(tm1.commits).isEqualTo(2);
    assertThat(tm2.commits).isEqualTo(2);
  }

  @Test
  @SuppressWarnings("resource")
  public void serializableWithPreviousUsage() throws Exception {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("annotationDrivenProxyTargetClassTests.xml", getClass());
    TransactionalService service = context.getBean("service", TransactionalService.class);
    service.setSomething("someName");
    service = SerializationTestUtils.serializeAndDeserialize(service);
    service.setSomething("someName");
  }

  @Test
  @SuppressWarnings("resource")
  public void serializableWithoutPreviousUsage() throws Exception {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("annotationDrivenProxyTargetClassTests.xml", getClass());
    TransactionalService service = context.getBean("service", TransactionalService.class);
    service = SerializationTestUtils.serializeAndDeserialize(service);
    service.setSomething("someName");
  }

  @SuppressWarnings("serial")
  public static class TransactionCheckingInterceptor implements MethodInterceptor, Serializable {

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
      if (methodInvocation.getMethod().getName().equals("setSomething")) {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      }
      else {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
      }
      return methodInvocation.proceed();
    }
  }

}
