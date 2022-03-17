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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Check that an aspect that depends on another bean, where the referenced bean
 * itself is advised by the same aspect, works correctly.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 */
@SuppressWarnings("resource")
public class PropertyDependentAspectTests {

  @Test
  public void propertyDependentAspectWithPropertyDeclaredBeforeAdvice()
          throws Exception {
    checkXmlAspect(getClass().getSimpleName() + "-before.xml");
  }

  @Test
  public void propertyDependentAspectWithPropertyDeclaredAfterAdvice() throws Exception {
    checkXmlAspect(getClass().getSimpleName() + "-after.xml");
  }

  @Test
  public void propertyDependentAtAspectJAspectWithPropertyDeclaredBeforeAdvice()
          throws Exception {
    checkAtAspectJAspect(getClass().getSimpleName() + "-atAspectJ-before.xml");
  }

  @Test
  public void propertyDependentAtAspectJAspectWithPropertyDeclaredAfterAdvice()
          throws Exception {
    checkAtAspectJAspect(getClass().getSimpleName() + "-atAspectJ-after.xml");
  }

  private void checkXmlAspect(String appContextFile) {
    ApplicationContext context = new ClassPathXmlApplicationContext(appContextFile, getClass());
    ICounter counter = (ICounter) context.getBean("counter");
    boolean condition = counter instanceof Advised;
    assertThat(condition).as("Proxy didn't get created").isTrue();

    counter.increment();
    JoinPointMonitorAspect callCountingAspect = (JoinPointMonitorAspect) context.getBean("monitoringAspect");
    assertThat(callCountingAspect.beforeExecutions).as("Advise didn't get executed").isEqualTo(1);
    assertThat(callCountingAspect.aroundExecutions).as("Advise didn't get executed").isEqualTo(1);
  }

  private void checkAtAspectJAspect(String appContextFile) {
    ApplicationContext context = new ClassPathXmlApplicationContext(appContextFile, getClass());
    ICounter counter = (ICounter) context.getBean("counter");
    boolean condition = counter instanceof Advised;
    assertThat(condition).as("Proxy didn't get created").isTrue();

    counter.increment();
    JoinPointMonitorAtAspectJAspect callCountingAspect = (JoinPointMonitorAtAspectJAspect) context.getBean("monitoringAspect");
    assertThat(callCountingAspect.beforeExecutions).as("Advise didn't get executed").isEqualTo(1);
    assertThat(callCountingAspect.aroundExecutions).as("Advise didn't get executed").isEqualTo(1);
  }

}

class JoinPointMonitorAspect {

  /**
   * The counter property is purposefully not used in the aspect to avoid distraction
   * from the main bug -- merely needing a dependency on an advised bean
   * is sufficient to reproduce the bug.
   */
  private ICounter counter;

  int beforeExecutions;
  int aroundExecutions;

  public void before() {
    beforeExecutions++;
  }

  public Object around(ProceedingJoinPoint pjp) throws Throwable {
    aroundExecutions++;
    return pjp.proceed();
  }

  public ICounter getCounter() {
    return counter;
  }

  public void setCounter(ICounter counter) {
    this.counter = counter;
  }

}

@Aspect
class JoinPointMonitorAtAspectJAspect {
  /* The counter property is purposefully not used in the aspect to avoid distraction
   * from the main bug -- merely needing a dependency on an advised bean
   * is sufficient to reproduce the bug.
   */
  private ICounter counter;

  int beforeExecutions;
  int aroundExecutions;

  @Before("execution(* increment*())")
  public void before() {
    beforeExecutions++;
  }

  @Around("execution(* increment*())")
  public Object around(ProceedingJoinPoint pjp) throws Throwable {
    aroundExecutions++;
    return pjp.proceed();
  }

  public ICounter getCounter() {
    return counter;
  }

  public void setCounter(ICounter counter) {
    this.counter = counter;
  }

}
