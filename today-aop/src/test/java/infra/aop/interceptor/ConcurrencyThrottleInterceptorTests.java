/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.aop.interceptor;

import org.junit.jupiter.api.Test;

import infra.aop.framework.Advised;
import infra.aop.framework.ProxyFactory;
import infra.beans.testfixture.beans.DerivedTestBean;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.core.testfixture.io.SerializationTestUtils;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 06.04.2004
 */
public class ConcurrencyThrottleInterceptorTests {

  protected static final Logger logger = LoggerFactory.getLogger(ConcurrencyThrottleInterceptorTests.class);

  public static final int NR_OF_THREADS = 100;

  public static final int NR_OF_ITERATIONS = 1000;

  @Test
  public void testSerializable() throws Exception {
    DerivedTestBean tb = new DerivedTestBean();
    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.setInterfaces(ITestBean.class);
    ConcurrencyThrottleInterceptor cti = new ConcurrencyThrottleInterceptor();
    proxyFactory.addAdvice(cti);
    proxyFactory.setTarget(tb);
    ITestBean proxy = (ITestBean) proxyFactory.getProxy();
    proxy.getAge();

    ITestBean serializedProxy = SerializationTestUtils.serializeAndDeserialize(proxy);
    Advised advised = (Advised) serializedProxy;
    ConcurrencyThrottleInterceptor serializedCti =
            (ConcurrencyThrottleInterceptor) advised.getAdvisors()[0].getAdvice();
    assertThat(serializedCti.getConcurrencyLimit()).isEqualTo(cti.getConcurrencyLimit());
    serializedProxy.getAge();
  }

  @Test
  public void testMultipleThreadsWithLimit1() {
    testMultipleThreads(1);
  }

  @Test
  public void testMultipleThreadsWithLimit10() {
    testMultipleThreads(10);
  }

  private void testMultipleThreads(int concurrencyLimit) {
    TestBean tb = new TestBean();
    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.setInterfaces(ITestBean.class);
    ConcurrencyThrottleInterceptor cti = new ConcurrencyThrottleInterceptor();
    cti.setConcurrencyLimit(concurrencyLimit);
    proxyFactory.addAdvice(cti);
    proxyFactory.setTarget(tb);
    ITestBean proxy = (ITestBean) proxyFactory.getProxy();

    Thread[] threads = new Thread[NR_OF_THREADS];
    for (int i = 0; i < NR_OF_THREADS; i++) {
      threads[i] = new ConcurrencyThread(proxy, null);
      threads[i].start();
    }
    for (int i = 0; i < NR_OF_THREADS / 10; i++) {
      try {
        Thread.sleep(5);
      }
      catch (InterruptedException ex) {
        ex.printStackTrace();
      }
      threads[i] = new ConcurrencyThread(proxy,
              i % 2 == 0 ? new OutOfMemoryError() : new IllegalStateException());
      threads[i].start();
    }
    for (int i = 0; i < NR_OF_THREADS; i++) {
      try {
        threads[i].join();
      }
      catch (InterruptedException ex) {
        ex.printStackTrace();
      }
    }
  }

  private static class ConcurrencyThread extends Thread {

    private ITestBean proxy;
    private Throwable ex;

    public ConcurrencyThread(ITestBean proxy, Throwable ex) {
      this.proxy = proxy;
      this.ex = ex;
    }

    @Override
    public void run() {
      if (this.ex != null) {
        try {
          this.proxy.exceptional(this.ex);
        }
        catch (RuntimeException | Error err) {
          if (err == this.ex) {
            logger.debug("Expected exception thrown", err);
          }
          else {
            // should never happen
            ex.printStackTrace();
          }
        }
        catch (Throwable ex) {
          // should never happen
          ex.printStackTrace();
        }
      }
      else {
        for (int i = 0; i < NR_OF_ITERATIONS; i++) {
          this.proxy.getName();
        }
      }
      logger.debug("finished");
    }
  }

}
