/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.transaction.support;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.support.GenericBeanDefinition;
import infra.beans.testfixture.beans.DerivedTestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.support.GenericApplicationContext;
import infra.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Juergen Hoeller
 */
class SimpleTransactionScopeTests {

  @Test
  void getFromScope() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.getBeanFactory().registerScope("tx", new SimpleTransactionScope());

    GenericBeanDefinition bd1 = new GenericBeanDefinition();
    bd1.setBeanClass(TestBean.class);
    bd1.setScope("tx");
    bd1.setPrimary(true);
    context.registerBeanDefinition("txScopedObject1", bd1);

    GenericBeanDefinition bd2 = new GenericBeanDefinition();
    bd2.setBeanClass(DerivedTestBean.class);
    bd2.setScope("tx");
    context.registerBeanDefinition("txScopedObject2", bd2);

    context.refresh();

    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> context.getBean(TestBean.class))
            .withCauseInstanceOf(IllegalStateException.class);

    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> context.getBean(DerivedTestBean.class))
            .withCauseInstanceOf(IllegalStateException.class);

    TestBean bean1;
    DerivedTestBean bean2;
    DerivedTestBean bean2a;
    DerivedTestBean bean2b;

    TransactionSynchronizationManager.initSynchronization();
    try {
      bean1 = context.getBean(TestBean.class);
      assertThat(context.getBean(TestBean.class)).isSameAs(bean1);

      bean2 = context.getBean(DerivedTestBean.class);
      assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2);
      context.getBeanFactory().destroyScopedBean("txScopedObject2");
      assertThat(TransactionSynchronizationManager.hasResource("txScopedObject2")).isFalse();
      assertThat(bean2.wasDestroyed()).isTrue();

      bean2a = context.getBean(DerivedTestBean.class);
      assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2a);
      assertThat(bean2a).isNotSameAs(bean2);
      context.getBeanFactory().getRegisteredScope("tx").remove("txScopedObject2");
      assertThat(TransactionSynchronizationManager.hasResource("txScopedObject2")).isFalse();
      assertThat(bean2a.wasDestroyed()).isFalse();

      bean2b = context.getBean(DerivedTestBean.class);
      assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2b);
      assertThat(bean2b).isNotSameAs(bean2);
      assertThat(bean2b).isNotSameAs(bean2a);
    }
    finally {
      TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
      TransactionSynchronizationManager.clearSynchronization();
    }

    assertThat(bean2a.wasDestroyed()).isFalse();
    assertThat(bean2b.wasDestroyed()).isTrue();
    assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();

    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> context.getBean(TestBean.class))
            .withCauseInstanceOf(IllegalStateException.class);

    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> context.getBean(DerivedTestBean.class))
            .withCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void getWithTransactionManager() {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      context.getBeanFactory().registerScope("tx", new SimpleTransactionScope());

      GenericBeanDefinition bd1 = new GenericBeanDefinition();
      bd1.setBeanClass(TestBean.class);
      bd1.setScope("tx");
      bd1.setPrimary(true);
      context.registerBeanDefinition("txScopedObject1", bd1);

      GenericBeanDefinition bd2 = new GenericBeanDefinition();
      bd2.setBeanClass(DerivedTestBean.class);
      bd2.setScope("tx");
      context.registerBeanDefinition("txScopedObject2", bd2);

      context.refresh();

      CallCountingTransactionManager tm = new CallCountingTransactionManager();
      TransactionTemplate tt = new TransactionTemplate(tm);
      Set<DerivedTestBean> finallyDestroy = new HashSet<>();

      tt.execute(status -> {
        TestBean bean1 = context.getBean(TestBean.class);
        assertThat(context.getBean(TestBean.class)).isSameAs(bean1);

        DerivedTestBean bean2 = context.getBean(DerivedTestBean.class);
        assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2);
        context.getBeanFactory().destroyScopedBean("txScopedObject2");
        assertThat(TransactionSynchronizationManager.hasResource("txScopedObject2")).isFalse();
        assertThat(bean2.wasDestroyed()).isTrue();

        DerivedTestBean bean2a = context.getBean(DerivedTestBean.class);
        assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2a);
        assertThat(bean2a).isNotSameAs(bean2);
        context.getBeanFactory().getRegisteredScope("tx").remove("txScopedObject2");
        assertThat(TransactionSynchronizationManager.hasResource("txScopedObject2")).isFalse();
        assertThat(bean2a.wasDestroyed()).isFalse();

        DerivedTestBean bean2b = context.getBean(DerivedTestBean.class);
        finallyDestroy.add(bean2b);
        assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2b);
        assertThat(bean2b).isNotSameAs(bean2);
        assertThat(bean2b).isNotSameAs(bean2a);

        Set<DerivedTestBean> immediatelyDestroy = new HashSet<>();
        TransactionTemplate tt2 = new TransactionTemplate(tm);
        tt2.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
        tt2.execute(status2 -> {
          DerivedTestBean bean2c = context.getBean(DerivedTestBean.class);
          immediatelyDestroy.add(bean2c);
          assertThat(context.getBean(DerivedTestBean.class)).isSameAs(bean2c);
          assertThat(bean2c).isNotSameAs(bean2);
          assertThat(bean2c).isNotSameAs(bean2a);
          assertThat(bean2c).isNotSameAs(bean2b);
          return null;
        });
        assertThat(immediatelyDestroy.iterator().next().wasDestroyed()).isTrue();
        assertThat(bean2b.wasDestroyed()).isFalse();

        return null;
      });

      assertThat(finallyDestroy.iterator().next().wasDestroyed()).isTrue();
    }
  }

  @Test
  void bindSynchronizedResource() {
    CallCountingTransactionManager tm = new CallCountingTransactionManager();
    TransactionTemplate tt = new TransactionTemplate(tm);

    tt.execute(status -> {
      TestBean tb = new TestBean();
      TransactionSynchronizationManager.bindSynchronizedResource("tb", tb);
      assertThat(TransactionSynchronizationManager.hasResource("tb")).isTrue();
      assertThat((Object) TransactionSynchronizationManager.getResource("tb")).isSameAs(tb);
      return null;
    });
    assertThat(TransactionSynchronizationManager.hasResource("tb")).isFalse();
  }

  @Test
  void bindSynchronizedResourceWithOldValue() {
    CallCountingTransactionManager tm = new CallCountingTransactionManager();
    TransactionTemplate tt = new TransactionTemplate(tm);

    TestBean oldValue = new TestBean();
    TransactionSynchronizationManager.bindResource("tb", oldValue);

    tt.execute(status -> {
      TestBean tb = new TestBean();
      TransactionSynchronizationManager.bindSynchronizedResource("tb", tb);
      assertThat(TransactionSynchronizationManager.hasResource("tb")).isTrue();
      assertThat((Object) TransactionSynchronizationManager.getResource("tb")).isSameAs(tb);
      return null;
    });
    assertThat(TransactionSynchronizationManager.hasResource("tb")).isTrue();
    assertThat((Object) TransactionSynchronizationManager.getResource("tb")).isSameAs(oldValue);
    TransactionSynchronizationManager.unbindResource("tb");
  }

  @Test
  void bindSynchronizedResourceWithoutTransaction() {
    assertThatIllegalStateException().isThrownBy(
            () -> TransactionSynchronizationManager.bindSynchronizedResource("tb", new TestBean()));
    assertThat(TransactionSynchronizationManager.hasResource("tb")).isFalse();
  }

}
