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

package infra.aop.aspectj.autoproxy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import infra.aop.framework.Advised;
import infra.beans.testfixture.beans.ITestBean;
import infra.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for ensuring the aspects aren't advised. See SPR-3893 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class AspectImplementingInterfaceTests {

  @Test
  public void testProxyCreation() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

    ITestBean testBean = (ITestBean) ctx.getBean("testBean");
    AnInterface interfaceExtendingAspect = (AnInterface) ctx.getBean("interfaceExtendingAspect");

    boolean condition = testBean instanceof Advised;
    assertThat(condition).isTrue();
    boolean condition1 = interfaceExtendingAspect instanceof Advised;
    assertThat(condition1).isFalse();
  }

}

interface AnInterface {
  public void interfaceMethod();
}

class InterfaceExtendingAspect implements AnInterface {
  public void increment(ProceedingJoinPoint pjp) throws Throwable {
    pjp.proceed();
  }

  @Override
  public void interfaceMethod() {
  }
}
