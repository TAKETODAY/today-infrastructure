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

package cn.taketoday.aop.framework;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.support.DelegatingIntroductionInterceptor;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.util.StopWatch;

/**
 * Benchmarks for introductions.
 *
 * NOTE: No assertions!
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @since 2.0
 */
public class IntroductionBenchmarkTests {

  private static final int EXPECTED_COMPARE = 13;

  /** Increase this if you want meaningful results! */
  private static final int INVOCATIONS = 100000;

  @SuppressWarnings("serial")
  public static class SimpleCounterIntroduction extends DelegatingIntroductionInterceptor implements Counter {
    @Override
    public int getCount() {
      return EXPECTED_COMPARE;
    }
  }

  public static interface Counter {
    int getCount();
  }

  @Test
  public void timeManyInvocations() {
    StopWatch sw = new StopWatch();

    TestBean target = new TestBean();
    ProxyFactory pf = new ProxyFactory(target);
    pf.setProxyTargetClass(false);
    pf.addAdvice(new SimpleCounterIntroduction());
    ITestBean proxy = (ITestBean) pf.getProxy();

    Counter counter = (Counter) proxy;

    sw.start(INVOCATIONS + " invocations on proxy, not hitting introduction");
    for (int i = 0; i < INVOCATIONS; i++) {
      proxy.getAge();
    }
    sw.stop();

    sw.start(INVOCATIONS + " invocations on proxy, hitting introduction");
    for (int i = 0; i < INVOCATIONS; i++) {
      counter.getCount();
    }
    sw.stop();

    sw.start(INVOCATIONS + " invocations on target");
    for (int i = 0; i < INVOCATIONS; i++) {
      target.getAge();
    }
    sw.stop();

    System.out.println(sw.prettyPrint());
  }
}
