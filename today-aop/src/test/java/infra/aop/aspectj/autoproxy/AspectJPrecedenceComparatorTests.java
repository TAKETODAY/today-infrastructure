/*
 * Copyright 2017 - 2026 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.aop.Advisor;
import infra.aop.AfterReturningAdvice;
import infra.aop.BeforeAdvice;
import infra.aop.aspectj.AbstractAspectJAdvice;
import infra.aop.aspectj.AspectJAfterAdvice;
import infra.aop.aspectj.AspectJAfterReturningAdvice;
import infra.aop.aspectj.AspectJAfterThrowingAdvice;
import infra.aop.aspectj.AspectJAroundAdvice;
import infra.aop.aspectj.AspectJExpressionPointcut;
import infra.aop.aspectj.AspectJMethodBeforeAdvice;
import infra.aop.aspectj.AspectJPointcutAdvisor;
import infra.aop.support.DefaultPointcutAdvisor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class AspectJPrecedenceComparatorTests {

  private static final int HIGH_PRECEDENCE_ADVISOR_ORDER = 100;
  private static final int LOW_PRECEDENCE_ADVISOR_ORDER = 200;
  private static final int EARLY_ADVICE_DECLARATION_ORDER = 5;
  private static final int LATE_ADVICE_DECLARATION_ORDER = 10;

  private AspectJPrecedenceComparator comparator;

  private Method anyOldMethod;

  private AspectJExpressionPointcut anyOldPointcut;

  @BeforeEach
  public void setUp() throws Exception {
    this.comparator = new AspectJPrecedenceComparator();
    this.anyOldMethod = getClass().getMethods()[0];
    this.anyOldPointcut = new AspectJExpressionPointcut();
    this.anyOldPointcut.setExpression("execution(* *(..))");
  }

  @Test
  public void testSameAspectNoAfterAdvice() {
    Advisor advisor1 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
    Advisor advisor2 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted before advisor2").isEqualTo(-1);

    advisor1 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
    advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor2 sorted before advisor1").isEqualTo(1);
  }

  @Test
  public void testSameAspectAfterAdvice() {
    Advisor advisor1 = createAspectJAfterAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
    Advisor advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor2 sorted before advisor1").isEqualTo(1);

    advisor1 = createAspectJAfterReturningAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
    advisor2 = createAspectJAfterThrowingAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted before advisor2").isEqualTo(-1);
  }

  @Test
  public void testSameAspectOneOfEach() {
    Advisor advisor1 = createAspectJAfterAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
    Advisor advisor2 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 and advisor2 not comparable").isEqualTo(1);
  }

  @Test
  public void testSameAdvisorPrecedenceDifferentAspectNoAfterAdvice() {
    Advisor advisor1 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
    Advisor advisor2 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("nothing to say about order here").isEqualTo(0);

    advisor1 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
    advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("nothing to say about order here").isEqualTo(0);
  }

  @Test
  public void testSameAdvisorPrecedenceDifferentAspectAfterAdvice() {
    Advisor advisor1 = createAspectJAfterAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
    Advisor advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("nothing to say about order here").isEqualTo(0);

    advisor1 = createAspectJAfterReturningAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
    advisor2 = createAspectJAfterThrowingAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("nothing to say about order here").isEqualTo(0);
  }

  @Test
  public void testHigherAdvisorPrecedenceNoAfterAdvice() {
    Advisor advisor1 = createInfraAOPBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER);
    Advisor advisor2 = createAspectJBeforeAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted before advisor2").isEqualTo(-1);

    advisor1 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
    advisor2 = createAspectJAroundAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted before advisor2").isEqualTo(-1);
  }

  @Test
  public void testHigherAdvisorPrecedenceAfterAdvice() {
    Advisor advisor1 = createAspectJAfterAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
    Advisor advisor2 = createAspectJAroundAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted before advisor2").isEqualTo(-1);

    advisor1 = createAspectJAfterReturningAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
    advisor2 = createAspectJAfterThrowingAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor2 sorted after advisor1").isEqualTo(-1);
  }

  @Test
  public void testLowerAdvisorPrecedenceNoAfterAdvice() {
    Advisor advisor1 = createAspectJBeforeAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
    Advisor advisor2 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted after advisor2").isEqualTo(1);

    advisor1 = createAspectJBeforeAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
    advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted after advisor2").isEqualTo(1);
  }

  @Test
  public void testLowerAdvisorPrecedenceAfterAdvice() {
    Advisor advisor1 = createAspectJAfterAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
    Advisor advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted after advisor2").isEqualTo(1);

    advisor1 = createInfraAOPAfterAdvice(LOW_PRECEDENCE_ADVISOR_ORDER);
    advisor2 = createAspectJAfterThrowingAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
    assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted after advisor2").isEqualTo(1);
  }

  private Advisor createAspectJBeforeAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName) {
    AspectJMethodBeforeAdvice advice = new AspectJMethodBeforeAdvice(this.anyOldMethod, this.anyOldPointcut, null);
    return createAspectJAdvice(advisorOrder, adviceDeclarationOrder, aspectName, advice);
  }

  private Advisor createAspectJAroundAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName) {
    AspectJAroundAdvice advice = new AspectJAroundAdvice(this.anyOldMethod, this.anyOldPointcut, null);
    return createAspectJAdvice(advisorOrder, adviceDeclarationOrder, aspectName, advice);
  }

  private Advisor createAspectJAfterAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName) {
    AspectJAfterAdvice advice = new AspectJAfterAdvice(this.anyOldMethod, this.anyOldPointcut, null);
    return createAspectJAdvice(advisorOrder, adviceDeclarationOrder, aspectName, advice);
  }

  private Advisor createAspectJAfterReturningAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName) {
    AspectJAfterReturningAdvice advice = new AspectJAfterReturningAdvice(this.anyOldMethod, this.anyOldPointcut, null);
    return createAspectJAdvice(advisorOrder, adviceDeclarationOrder, aspectName, advice);
  }

  private Advisor createAspectJAfterThrowingAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName) {
    AspectJAfterThrowingAdvice advice = new AspectJAfterThrowingAdvice(this.anyOldMethod, this.anyOldPointcut, null);
    return createAspectJAdvice(advisorOrder, adviceDeclarationOrder, aspectName, advice);
  }

  private Advisor createAspectJAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName, AbstractAspectJAdvice advice) {
    advice.setDeclarationOrder(adviceDeclarationOrder);
    advice.setAspectName(aspectName);
    AspectJPointcutAdvisor advisor = new AspectJPointcutAdvisor(advice);
    advisor.setOrder(advisorOrder);
    return advisor;
  }

  private Advisor createInfraAOPAfterAdvice(int order) {
    AfterReturningAdvice advice = (returnValue, method) -> {
    };
    DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(this.anyOldPointcut, advice);
    advisor.setOrder(order);
    return advisor;
  }

  private Advisor createInfraAOPBeforeAdvice(int order) {
    BeforeAdvice advice = new BeforeAdvice() {
    };
    DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(this.anyOldPointcut, advice);
    advisor.setOrder(order);
    return advisor;
  }

}
