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

package infra.aop.aspectj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import infra.aop.framework.Advised;
import infra.aop.support.AopUtils;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Chris Beams
 */
public class BeforeAdviceBindingTests {

  private AdviceBindingTestAspect.AdviceBindingCollaborator mockCollaborator;

  private ITestBean testBeanProxy;

  private TestBean testBeanTarget;

  @BeforeEach
  public void setup() throws Exception {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

    testBeanProxy = (ITestBean) ctx.getBean("testBean");
    assertThat(AopUtils.isAopProxy(testBeanProxy)).isTrue();

    // we need the real target too, not just the proxy...
    testBeanTarget = (TestBean) ((Advised) testBeanProxy).getTargetSource().getTarget();

    AdviceBindingTestAspect beforeAdviceAspect = (AdviceBindingTestAspect) ctx.getBean("testAspect");

    mockCollaborator = Mockito.mock(AdviceBindingTestAspect.AdviceBindingCollaborator.class);
    beforeAdviceAspect.setCollaborator(mockCollaborator);
  }

  @Test
  public void testOneIntArg() {
    testBeanProxy.setAge(5);
    verify(mockCollaborator).oneIntArg(5);
  }

  @Test
  public void testOneObjectArgBoundToProxyUsingThis() {
    testBeanProxy.getAge();
    verify(mockCollaborator).oneObjectArg(this.testBeanProxy);
  }

  @Test
  public void testOneIntAndOneObjectArgs() {
    testBeanProxy.setAge(5);
    verify(mockCollaborator).oneIntAndOneObject(5, this.testBeanTarget);
  }

  @Test
  public void testNeedsJoinPoint() {
    testBeanProxy.getAge();
    verify(mockCollaborator).needsJoinPoint("getAge");
  }

  @Test
  public void testNeedsJoinPointStaticPart() {
    testBeanProxy.getAge();
    verify(mockCollaborator).needsJoinPointStaticPart("getAge");
  }

}

class AuthenticationLogger {

  public void logAuthenticationAttempt(String username) {
    System.out.println("User [" + username + "] attempting to authenticate");
  }

}

class SecurityManager {
  public boolean authenticate(String username, String password) {
    return false;
  }
}
