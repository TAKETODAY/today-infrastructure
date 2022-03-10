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

package cn.taketoday.aop.aspectj.autoproxy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class AtAspectJAnnotationBindingTests {

  private AnnotatedTestBean testBean;

  private ClassPathXmlApplicationContext ctx;

  @BeforeEach
  public void setup() {
    ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
    testBean = (AnnotatedTestBean) ctx.getBean("testBean");
  }

  @Test
  public void testAnnotationBindingInAroundAdvice() {
    assertThat(testBean.doThis()).isEqualTo("this value doThis");
    assertThat(testBean.doThat()).isEqualTo("that value doThat");
    assertThat(testBean.doArray().length).isEqualTo(2);
  }

  @Test
  public void testNoMatchingWithoutAnnotationPresent() {
    assertThat(testBean.doTheOther()).isEqualTo("doTheOther");
  }

  @Test
  public void testPointcutEvaluatedAgainstArray() {
    ctx.getBean("arrayFactoryBean");
  }

}

@Aspect
class AtAspectJAnnotationBindingTestAspect {

  @Around("execution(* *(..)) && @annotation(testAnn)")
  public Object doWithAnnotation(ProceedingJoinPoint pjp, TestAnnotation testAnn) throws Throwable {
    String annValue = testAnn.value();
    Object result = pjp.proceed();
    return (result instanceof String ? annValue + " " + result : result);
  }

}

class ResourceArrayFactoryBean implements FactoryBean<Object> {

  @Override
  @TestAnnotation("some value")
  public Object getObject() {
    return new Resource[0];
  }

  @Override
  @TestAnnotation("some value")
  public Class<?> getObjectType() {
    return Resource[].class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
