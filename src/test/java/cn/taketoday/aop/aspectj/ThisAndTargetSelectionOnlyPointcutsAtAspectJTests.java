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

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class ThisAndTargetSelectionOnlyPointcutsAtAspectJTests {

  private TestInterface testBean;

  private TestInterface testAnnotatedClassBean;

  private TestInterface testAnnotatedMethodBean;

  private Counter counter;

  @org.junit.jupiter.api.BeforeEach
  public void setup() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
    testBean = (TestInterface) ctx.getBean("testBean");
    testAnnotatedClassBean = (TestInterface) ctx.getBean("testAnnotatedClassBean");
    testAnnotatedMethodBean = (TestInterface) ctx.getBean("testAnnotatedMethodBean");
    counter = (Counter) ctx.getBean("counter");
    counter.reset();
  }

  @Test
  public void thisAsClassDoesNotMatch() {
    testBean.doIt();
    assertThat(counter.thisAsClassCounter).isEqualTo(0);
  }

  @Test
  public void thisAsInterfaceMatch() {
    testBean.doIt();
    assertThat(counter.thisAsInterfaceCounter).isEqualTo(1);
  }

  @Test
  public void targetAsClassDoesMatch() {
    testBean.doIt();
    assertThat(counter.targetAsClassCounter).isEqualTo(1);
  }

  @Test
  public void targetAsInterfaceMatch() {
    testBean.doIt();
    assertThat(counter.targetAsInterfaceCounter).isEqualTo(1);
  }

  @Test
  public void thisAsClassAndTargetAsClassCounterNotMatch() {
    testBean.doIt();
    assertThat(counter.thisAsClassAndTargetAsClassCounter).isEqualTo(0);
  }

  @Test
  public void thisAsInterfaceAndTargetAsInterfaceCounterMatch() {
    testBean.doIt();
    assertThat(counter.thisAsInterfaceAndTargetAsInterfaceCounter).isEqualTo(1);
  }

  @Test
  public void thisAsInterfaceAndTargetAsClassCounterMatch() {
    testBean.doIt();
    assertThat(counter.thisAsInterfaceAndTargetAsInterfaceCounter).isEqualTo(1);
  }

  @Test
  public void atTargetClassAnnotationMatch() {
    testAnnotatedClassBean.doIt();
    assertThat(counter.atTargetClassAnnotationCounter).isEqualTo(1);
  }

  @Test
  public void atAnnotationMethodAnnotationMatch() {
    testAnnotatedMethodBean.doIt();
    assertThat(counter.atAnnotationMethodAnnotationCounter).isEqualTo(1);
  }

  public static interface TestInterface {
    public void doIt();
  }

  public static class TestImpl implements TestInterface {
    @Override
    public void doIt() {
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface TestAnnotation {

  }

  @TestAnnotation
  public static class AnnotatedClassTestImpl implements TestInterface {
    @Override
    public void doIt() {
    }
  }

  public static class AnnotatedMethodTestImpl implements TestInterface {
    @Override
    @TestAnnotation
    public void doIt() {
    }
  }

  @Aspect
  public static class Counter {
    int thisAsClassCounter;
    int thisAsInterfaceCounter;
    int targetAsClassCounter;
    int targetAsInterfaceCounter;
    int thisAsClassAndTargetAsClassCounter;
    int thisAsInterfaceAndTargetAsInterfaceCounter;
    int thisAsInterfaceAndTargetAsClassCounter;
    int atTargetClassAnnotationCounter;
    int atAnnotationMethodAnnotationCounter;

    public void reset() {
      thisAsClassCounter = 0;
      thisAsInterfaceCounter = 0;
      targetAsClassCounter = 0;
      targetAsInterfaceCounter = 0;
      thisAsClassAndTargetAsClassCounter = 0;
      thisAsInterfaceAndTargetAsInterfaceCounter = 0;
      thisAsInterfaceAndTargetAsClassCounter = 0;
      atTargetClassAnnotationCounter = 0;
      atAnnotationMethodAnnotationCounter = 0;
    }

    @Before("this(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
    public void incrementThisAsClassCounter() {
      thisAsClassCounter++;
    }

    @Before("this(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface)")
    public void incrementThisAsInterfaceCounter() {
      thisAsInterfaceCounter++;
    }

    @Before("target(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
    public void incrementTargetAsClassCounter() {
      targetAsClassCounter++;
    }

    @Before("target(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface)")
    public void incrementTargetAsInterfaceCounter() {
      targetAsInterfaceCounter++;
    }

    @Before("this(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl) " +
            "&& target(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
    public void incrementThisAsClassAndTargetAsClassCounter() {
      thisAsClassAndTargetAsClassCounter++;
    }

    @Before("this(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface) " +
            "&& target(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface)")
    public void incrementThisAsInterfaceAndTargetAsInterfaceCounter() {
      thisAsInterfaceAndTargetAsInterfaceCounter++;
    }

    @Before("this(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface) " +
            "&& target(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
    public void incrementThisAsInterfaceAndTargetAsClassCounter() {
      thisAsInterfaceAndTargetAsClassCounter++;
    }

    @Before("@target(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestAnnotation)")
    public void incrementAtTargetClassAnnotationCounter() {
      atTargetClassAnnotationCounter++;
    }

    @Before("@annotation(cn.taketoday.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestAnnotation)")
    public void incrementAtAnnotationMethodAnnotationCounter() {
      atAnnotationMethodAnnotationCounter++;
    }

  }
}
