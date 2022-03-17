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

package cn.taketoday.aop.aspectj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aop.aspectj.AdviceBindingTestAspect.AdviceBindingCollaborator;
import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Chris Beams
 */
public class AfterAdviceBindingTests {

  private AdviceBindingCollaborator mockCollaborator;

  private ITestBean testBeanProxy;

  private TestBean testBeanTarget;

  @BeforeEach
  public void setup() throws Exception {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
    AdviceBindingTestAspect afterAdviceAspect = (AdviceBindingTestAspect) ctx.getBean("testAspect");

    testBeanProxy = (ITestBean) ctx.getBean("testBean");
    assertThat(AopUtils.isAopProxy(testBeanProxy)).isTrue();

    // we need the real target too, not just the proxy...
    testBeanTarget = (TestBean) ((Advised) testBeanProxy).getTargetSource().getTarget();

    mockCollaborator = mock(AdviceBindingCollaborator.class);
    afterAdviceAspect.setCollaborator(mockCollaborator);
  }

  @Test
  public void testOneIntArg() {
    testBeanProxy.setAge(5);
    verify(mockCollaborator).oneIntArg(5);
  }

  @Test
  public void testOneObjectArgBindingProxyWithThis() {
    testBeanProxy.getAge();
    verify(mockCollaborator).oneObjectArg(this.testBeanProxy);
  }

  @Test
  public void testOneObjectArgBindingTarget() {
    testBeanProxy.getDoctor();
    verify(mockCollaborator).oneObjectArg(this.testBeanTarget);
  }

  @Test
  public void testOneIntAndOneObjectArgs() {
    testBeanProxy.setAge(5);
    verify(mockCollaborator).oneIntAndOneObject(5, this.testBeanProxy);
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
