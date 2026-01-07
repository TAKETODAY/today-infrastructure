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

package infra.aop.aspectj;

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.aop.MethodBeforeAdvice;
import infra.beans.factory.BeanNameAware;
import infra.beans.testfixture.beans.ITestBean;
import infra.context.support.ClassPathXmlApplicationContext;
import infra.core.Ordered;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
class AspectAndAdvicePrecedenceTests {

  private PrecedenceTestAspect highPrecedenceAspect;

  private PrecedenceTestAspect lowPrecedenceAspect;

  private SimpleInfraBeforeAdvice highPrecedenceAdvice;

  private SimpleInfraBeforeAdvice lowPrecedenceAdvice;

  private ITestBean testBean;

  @BeforeEach
  public void setup() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
    highPrecedenceAspect = (PrecedenceTestAspect) ctx.getBean("highPrecedenceAspect");
    lowPrecedenceAspect = (PrecedenceTestAspect) ctx.getBean("lowPrecedenceAspect");
    highPrecedenceAdvice = (SimpleInfraBeforeAdvice) ctx.getBean("highPrecedenceAdvice");
    lowPrecedenceAdvice = (SimpleInfraBeforeAdvice) ctx.getBean("lowPrecedenceAdvice");
    testBean = (ITestBean) ctx.getBean("testBean");
  }

  @Test
  public void testAdviceOrder() {
    PrecedenceTestAspect.Collaborator collaborator = new PrecedenceVerifyingCollaborator();
    this.highPrecedenceAspect.setCollaborator(collaborator);
    this.lowPrecedenceAspect.setCollaborator(collaborator);
    this.highPrecedenceAdvice.setCollaborator(collaborator);
    this.lowPrecedenceAdvice.setCollaborator(collaborator);
    this.testBean.getAge();
  }

  private static class PrecedenceVerifyingCollaborator implements PrecedenceTestAspect.Collaborator {

    private static final String[] EXPECTED = {
            // this order confirmed by running the same aspects (minus the AOP advisors)
            // through AspectJ...
            "beforeAdviceOne(highPrecedenceAspect)",        // 1
            "beforeAdviceTwo(highPrecedenceAspect)",        // 2
            "aroundAdviceOne(highPrecedenceAspect)",        // 3,  before proceed
            "aroundAdviceTwo(highPrecedenceAspect)",      // 4,  before proceed
            "beforeAdviceOne(highPrecedenceAdvice)",  // 5
            "beforeAdviceOne(lowPrecedenceAdvice)",  // 6
            "beforeAdviceOne(lowPrecedenceAspect)",      // 7
            "beforeAdviceTwo(lowPrecedenceAspect)",      // 8
            "aroundAdviceOne(lowPrecedenceAspect)",      // 9,  before proceed
            "aroundAdviceTwo(lowPrecedenceAspect)",        // 10, before proceed
            "aroundAdviceTwo(lowPrecedenceAspect)",        // 11, after proceed
            "aroundAdviceOne(lowPrecedenceAspect)",      // 12, after proceed
            "afterAdviceOne(lowPrecedenceAspect)",      // 13
            "afterAdviceTwo(lowPrecedenceAspect)",      // 14
            "aroundAdviceTwo(highPrecedenceAspect)",      // 15, after proceed
            "aroundAdviceOne(highPrecedenceAspect)",        // 16, after proceed
            "afterAdviceOne(highPrecedenceAspect)",          // 17
            "afterAdviceTwo(highPrecedenceAspect)"          // 18
    };

    private int adviceInvocationNumber = 0;

    private void checkAdvice(String whatJustHappened) {
      //System.out.println("[" + adviceInvocationNumber + "] " + whatJustHappened + " ==> " + EXPECTED[adviceInvocationNumber]);
      if (adviceInvocationNumber > (EXPECTED.length - 1)) {
        throw new AssertionError("Too many advice invocations, expecting " + EXPECTED.length
                + " but had " + adviceInvocationNumber);
      }
      String expecting = EXPECTED[adviceInvocationNumber++];
      if (!whatJustHappened.equals(expecting)) {
        throw new AssertionError("Expecting '" + expecting + "' on advice invocation " + adviceInvocationNumber +
                " but got '" + whatJustHappened + "'");
      }
    }

    @Override
    public void beforeAdviceOne(String beanName) {
      checkAdvice("beforeAdviceOne(" + beanName + ")");
    }

    @Override
    public void beforeAdviceTwo(String beanName) {
      checkAdvice("beforeAdviceTwo(" + beanName + ")");
    }

    @Override
    public void aroundAdviceOne(String beanName) {
      checkAdvice("aroundAdviceOne(" + beanName + ")");
    }

    @Override
    public void aroundAdviceTwo(String beanName) {
      checkAdvice("aroundAdviceTwo(" + beanName + ")");
    }

    @Override
    public void afterAdviceOne(String beanName) {
      checkAdvice("afterAdviceOne(" + beanName + ")");
    }

    @Override
    public void afterAdviceTwo(String beanName) {
      checkAdvice("afterAdviceTwo(" + beanName + ")");
    }
  }

}

class PrecedenceTestAspect implements BeanNameAware, Ordered {

  private String name;

  private int order = Ordered.LOWEST_PRECEDENCE;

  private Collaborator collaborator;

  @Override
  public void setBeanName(String name) {
    this.name = name;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return order;
  }

  public void setCollaborator(Collaborator collaborator) {
    this.collaborator = collaborator;
  }

  public void beforeAdviceOne() {
    this.collaborator.beforeAdviceOne(this.name);
  }

  public void beforeAdviceTwo() {
    this.collaborator.beforeAdviceTwo(this.name);
  }

  public int aroundAdviceOne(ProceedingJoinPoint pjp) {
    int ret = -1;
    this.collaborator.aroundAdviceOne(this.name);
    try {
      ret = ((Integer) pjp.proceed()).intValue();
    }
    catch (Throwable t) {
      throw new RuntimeException(t);
    }
    this.collaborator.aroundAdviceOne(this.name);
    return ret;
  }

  public int aroundAdviceTwo(ProceedingJoinPoint pjp) {
    int ret = -1;
    this.collaborator.aroundAdviceTwo(this.name);
    try {
      ret = ((Integer) pjp.proceed()).intValue();
    }
    catch (Throwable t) {
      throw new RuntimeException(t);
    }
    this.collaborator.aroundAdviceTwo(this.name);
    return ret;
  }

  public void afterAdviceOne() {
    this.collaborator.afterAdviceOne(this.name);
  }

  public void afterAdviceTwo() {
    this.collaborator.afterAdviceTwo(this.name);
  }

  public interface Collaborator {

    void beforeAdviceOne(String beanName);

    void beforeAdviceTwo(String beanName);

    void aroundAdviceOne(String beanName);

    void aroundAdviceTwo(String beanName);

    void afterAdviceOne(String beanName);

    void afterAdviceTwo(String beanName);
  }

}

class SimpleInfraBeforeAdvice implements MethodBeforeAdvice, BeanNameAware {

  private PrecedenceTestAspect.Collaborator collaborator;
  private String name;

  public void setCollaborator(PrecedenceTestAspect.Collaborator collaborator) {
    this.collaborator = collaborator;
  }

  /* (non-Javadoc)
   * @see infra.beans.factory.BeanNameAware#setBeanName(java.lang.String)
   */
  @Override
  public void setBeanName(String name) {
    this.name = name;
  }

  @Override
  public void before(MethodInvocation invocation) throws Throwable {
    this.collaborator.beforeAdviceOne(this.name);

  }
}
