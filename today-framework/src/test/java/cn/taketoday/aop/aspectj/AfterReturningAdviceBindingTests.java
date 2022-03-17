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

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class AfterReturningAdviceBindingTests {

  private AfterReturningAdviceBindingTestAspect afterAdviceAspect;

  private ITestBean testBeanProxy;

  private TestBean testBeanTarget;

  private AfterReturningAdviceBindingTestAspect.AfterReturningAdviceBindingCollaborator mockCollaborator;

  @BeforeEach
  public void setup() throws Exception {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

    afterAdviceAspect = (AfterReturningAdviceBindingTestAspect) ctx.getBean("testAspect");

    mockCollaborator = mock(AfterReturningAdviceBindingTestAspect.AfterReturningAdviceBindingCollaborator.class);
    afterAdviceAspect.setCollaborator(mockCollaborator);

    testBeanProxy = (ITestBean) ctx.getBean("testBean");
    assertThat(AopUtils.isAopProxy(testBeanProxy)).isTrue();

    // we need the real target too, not just the proxy...
    this.testBeanTarget = (TestBean) ((Advised) testBeanProxy).getTargetSource().getTarget();
  }

  @Test
  public void testOneIntArg() {
    testBeanProxy.setAge(5);
    verify(mockCollaborator).oneIntArg(5);
  }

  @Test
  public void testOneObjectArg() {
    testBeanProxy.getAge();
    verify(mockCollaborator).oneObjectArg(this.testBeanProxy);
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

  @Test
  public void testReturningString() {
    testBeanProxy.setName("adrian");
    testBeanProxy.getName();
    verify(mockCollaborator).oneString("adrian");
  }

  @Test
  public void testReturningObject() {
    testBeanProxy.returnsThis();
    verify(mockCollaborator).oneObjectArg(this.testBeanTarget);
  }

  @Test
  public void testReturningBean() {
    testBeanProxy.returnsThis();
    verify(mockCollaborator).oneTestBeanArg(this.testBeanTarget);
  }

  @Test
  public void testReturningBeanArray() {
    this.testBeanTarget.setSpouse(new TestBean());
    ITestBean[] spouses = this.testBeanTarget.getSpouses();
    testBeanProxy.getSpouses();
    verify(mockCollaborator).testBeanArrayArg(spouses);
  }

  @Test
  public void testNoInvokeWhenReturningParameterTypeDoesNotMatch() {
    testBeanProxy.setSpouse(this.testBeanProxy);
    testBeanProxy.getSpouse();
    verifyNoInteractions(mockCollaborator);
  }

  @Test
  public void testReturningByType() {
    testBeanProxy.returnsThis();
    verify(mockCollaborator).objectMatchNoArgs();
  }

  @Test
  public void testReturningPrimitive() {
    testBeanProxy.setAge(20);
    testBeanProxy.haveBirthday();
    verify(mockCollaborator).oneInt(20);
  }

}

final class AfterReturningAdviceBindingTestAspect extends AdviceBindingTestAspect {

  private AfterReturningAdviceBindingCollaborator getCollaborator() {
    return (AfterReturningAdviceBindingCollaborator) this.collaborator;
  }

  public void oneString(String name) {
    getCollaborator().oneString(name);
  }

  public void oneTestBeanArg(TestBean bean) {
    getCollaborator().oneTestBeanArg(bean);
  }

  public void testBeanArrayArg(ITestBean[] beans) {
    getCollaborator().testBeanArrayArg(beans);
  }

  public void objectMatchNoArgs() {
    getCollaborator().objectMatchNoArgs();
  }

  public void stringMatchNoArgs() {
    getCollaborator().stringMatchNoArgs();
  }

  public void oneInt(int result) {
    getCollaborator().oneInt(result);
  }

  interface AfterReturningAdviceBindingCollaborator extends AdviceBindingCollaborator {

    void oneString(String s);

    void oneTestBeanArg(TestBean b);

    void testBeanArrayArg(ITestBean[] b);

    void objectMatchNoArgs();

    void stringMatchNoArgs();

    void oneInt(int result);
  }

}
