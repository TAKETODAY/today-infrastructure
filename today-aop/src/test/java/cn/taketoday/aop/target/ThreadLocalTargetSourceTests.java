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

package cn.taketoday.aop.target;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.SideEffectBean;

import static cn.taketoday.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ThreadLocalTargetSourceTests {

  /** Initial count value set in bean factory XML */
  private static final int INITIAL_COUNT = 10;

  private StandardBeanFactory beanFactory;

  @BeforeEach
  public void setup() {
    this.beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
            qualifiedResource(ThreadLocalTargetSourceTests.class, "context.xml"));
  }

  /**
   * We must simulate container shutdown, which should clear threads.
   */
  protected void close() {
    this.beanFactory.destroySingletons();
  }

  /**
   * Check we can use two different ThreadLocalTargetSources
   * managing objects of different types without them interfering
   * with one another.
   */
  @Test
  public void testUseDifferentManagedInstancesInSameThread() {
    SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
    assertThat(apartment.getCount()).isEqualTo(INITIAL_COUNT);
    apartment.doWork();
    assertThat(apartment.getCount()).isEqualTo((INITIAL_COUNT + 1));

    ITestBean test = (ITestBean) beanFactory.getBean("threadLocal2");
    assertThat(test.getName()).isEqualTo("Rod");
    assertThat(test.getSpouse().getName()).isEqualTo("Kerry");
  }

  @Test
  public void testReuseInSameThread() {
    SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
    assertThat(apartment.getCount()).isEqualTo(INITIAL_COUNT);
    apartment.doWork();
    assertThat(apartment.getCount()).isEqualTo((INITIAL_COUNT + 1));

    apartment = (SideEffectBean) beanFactory.getBean("apartment");
    assertThat(apartment.getCount()).isEqualTo((INITIAL_COUNT + 1));
  }

  /**
   * Relies on introduction.
   */
  @Test
  public void testCanGetStatsViaMixin() {
    ThreadLocalTargetSourceStats stats = (ThreadLocalTargetSourceStats) beanFactory.getBean("apartment");
    // +1 because creating target for stats call counts
    assertThat(stats.getInvocationCount()).isEqualTo(1);
    SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
    apartment.doWork();
    // +1 again
    assertThat(stats.getInvocationCount()).isEqualTo(3);
    // + 1 for states call!
    assertThat(stats.getHitCount()).isEqualTo(3);
    apartment.doWork();
    assertThat(stats.getInvocationCount()).isEqualTo(6);
    assertThat(stats.getHitCount()).isEqualTo(6);
    // Only one thread so only one object can have been bound
    assertThat(stats.getObjectCount()).isEqualTo(1);
  }

  @Test
  public void testNewThreadHasOwnInstance() throws InterruptedException {
    SideEffectBean apartment = (SideEffectBean) beanFactory.getBean("apartment");
    assertThat(apartment.getCount()).isEqualTo(INITIAL_COUNT);
    apartment.doWork();
    apartment.doWork();
    apartment.doWork();
    assertThat(apartment.getCount()).isEqualTo((INITIAL_COUNT + 3));

    class Runner implements Runnable {
      public SideEffectBean mine;

      @Override
      public void run() {
        this.mine = (SideEffectBean) beanFactory.getBean("apartment");
        assertThat(mine.getCount()).isEqualTo(INITIAL_COUNT);
        mine.doWork();
        assertThat(mine.getCount()).isEqualTo((INITIAL_COUNT + 1));
      }
    }
    Runner r = new Runner();
    Thread t = new Thread(r);
    t.start();
    t.join();

    assertThat(r).isNotNull();

    // Check it didn't affect the other thread's copy
    assertThat(apartment.getCount()).isEqualTo((INITIAL_COUNT + 3));

    // When we use other thread's copy in this thread
    // it should behave like ours
    assertThat(r.mine.getCount()).isEqualTo((INITIAL_COUNT + 3));

    // Bound to two threads
    assertThat(((ThreadLocalTargetSourceStats) apartment).getObjectCount()).isEqualTo(2);
  }

  /**
   * Test for SPR-1442. Destroyed target should re-associated with thread and not throw NPE.
   */
  @Test
  public void testReuseDestroyedTarget() {
    ThreadLocalTargetSource source = (ThreadLocalTargetSource) this.beanFactory.getBean("threadLocalTs");

    // try first time
    source.getTarget();
    source.destroy();

    // try second time
    source.getTarget(); // Should not throw NPE
  }

}
