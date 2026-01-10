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

package infra.aop.support.interceptor;

import org.junit.jupiter.api.Test;

import infra.aop.framework.Advised;
import infra.aop.framework.ProxyFactory;
import infra.aop.interceptor.ConcurrencyThrottleInterceptor;
import infra.beans.testfixture.beans.DerivedTestBean;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.core.testfixture.io.SerializationTestUtils;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/9/11 17:21
 */
class ConcurrencyThrottleInterceptorTests {
  static final Logger logger = LoggerFactory.getLogger(ConcurrencyThrottleInterceptorTests.class);

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
      threads[i] = new ConcurrencyThread(
              proxy, i % 2 == 0 ? new OutOfMemoryError() : new IllegalStateException());
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
        catch (RuntimeException ex) {
          if (ex == this.ex) {
            logger.debug("Expected exception thrown", ex);
          }
          else {
            // should never happen
            ex.printStackTrace();
          }
        }
        catch (Error err) {
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
