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

package infra.aop.aspectj.autoproxy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import infra.aop.framework.Advised;
import infra.beans.testfixture.beans.ITestBean;
import infra.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
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
