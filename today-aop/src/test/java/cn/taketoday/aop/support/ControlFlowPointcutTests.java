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

package cn.taketoday.aop.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.aop.testfixture.interceptor.NopInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ControlFlowPointcutTests {

  @Test
  public void testMatches() {
    TestBean target = new TestBean();
    target.setAge(27);
    NopInterceptor nop = new NopInterceptor();
    ControlFlowPointcut cflow = new ControlFlowPointcut(One.class, "getAge");
    ProxyFactory pf = new ProxyFactory(target);
    ITestBean proxied = (ITestBean) pf.getProxy();
    pf.addAdvisor(new DefaultPointcutAdvisor(cflow, nop));

    // Not advised, not under One
    assertThat(proxied.getAge()).isEqualTo(target.getAge());
    assertThat(nop.getCount()).isEqualTo(0);

    // Will be advised
    assertThat(new One().getAge(proxied)).isEqualTo(target.getAge());
    assertThat(nop.getCount()).isEqualTo(1);

    // Won't be advised
    assertThat(new One().nomatch(proxied)).isEqualTo(target.getAge());
    assertThat(nop.getCount()).isEqualTo(1);
    assertThat(cflow.getEvaluations()).isEqualTo(3);
  }

  /**
   * Check that we can use a cflow pointcut only in conjunction with
   * a static pointcut: e.g. all setter methods that are invoked under
   * a particular class. This greatly reduces the number of calls
   * to the cflow pointcut, meaning that it's not so prohibitively
   * expensive.
   */
  @Test
  public void testSelectiveApplication() {
    TestBean target = new TestBean();
    target.setAge(27);
    NopInterceptor nop = new NopInterceptor();
    ControlFlowPointcut cflow = new ControlFlowPointcut(One.class);
    Pointcut settersUnderOne = Pointcuts.intersection(Pointcuts.SETTERS, cflow);
    ProxyFactory pf = new ProxyFactory(target);
    ITestBean proxied = (ITestBean) pf.getProxy();
    pf.addAdvisor(new DefaultPointcutAdvisor(settersUnderOne, nop));

    // Not advised, not under One
    target.setAge(16);
    assertThat(nop.getCount()).isEqualTo(0);

    // Not advised; under One but not a setter
    assertThat(new One().getAge(proxied)).isEqualTo(16);
    assertThat(nop.getCount()).isEqualTo(0);

    // Won't be advised
    new One().set(proxied);
    assertThat(nop.getCount()).isEqualTo(1);

    // We saved most evaluations
    assertThat(cflow.getEvaluations()).isEqualTo(1);
  }

  @Test
  public void testEqualsAndHashCode() throws Exception {
    assertThat(new ControlFlowPointcut(One.class)).isEqualTo(new ControlFlowPointcut(One.class));
    assertThat(new ControlFlowPointcut(One.class, "getAge")).isEqualTo(new ControlFlowPointcut(One.class, "getAge"));
    assertThat(new ControlFlowPointcut(One.class, "getAge").equals(new ControlFlowPointcut(One.class))).isFalse();
    assertThat(new ControlFlowPointcut(One.class).hashCode()).isEqualTo(new ControlFlowPointcut(One.class).hashCode());
    assertThat(new ControlFlowPointcut(One.class, "getAge").hashCode()).isEqualTo(new ControlFlowPointcut(One.class, "getAge").hashCode());
    assertThat(new ControlFlowPointcut(One.class, "getAge").hashCode() == new ControlFlowPointcut(One.class).hashCode()).isFalse();
  }

  @Test
  public void testToString() {
    assertThat(new ControlFlowPointcut(One.class).toString())
            .isEqualTo(ControlFlowPointcut.class.getName() + ": class = " + One.class.getName() + "; methodName = null");
    assertThat(new ControlFlowPointcut(One.class, "getAge").toString())
            .isEqualTo(ControlFlowPointcut.class.getName() + ": class = " + One.class.getName() + "; methodName = getAge");
  }

  public class One {
    int getAge(ITestBean proxied) {
      return proxied.getAge();
    }

    int nomatch(ITestBean proxied) {
      return proxied.getAge();
    }

    void set(ITestBean proxied) {
      proxied.setAge(5);
    }
  }

}
