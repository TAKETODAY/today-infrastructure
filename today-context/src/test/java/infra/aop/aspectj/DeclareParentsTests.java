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

import infra.aop.support.AopUtils;
import infra.beans.testfixture.beans.ITestBean;
import infra.context.support.ClassPathXmlApplicationContext;
import test.mixin.Lockable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class DeclareParentsTests {

  private ITestBean testBeanProxy;

  private Object introductionObject;

  @BeforeEach
  public void setup() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
    testBeanProxy = (ITestBean) ctx.getBean("testBean");
    introductionObject = ctx.getBean("introduction");
  }

  @Test
  public void testIntroductionWasMade() {
    assertThat(AopUtils.isAopProxy(testBeanProxy)).isTrue();
    assertThat(AopUtils.isAopProxy(introductionObject)).as("Introduction should not be proxied").isFalse();
    boolean condition = testBeanProxy instanceof Lockable;
    assertThat(condition).as("Introduction must have been made").isTrue();
  }

  // TODO if you change type pattern from infra.beans..*
  // to infra..* it also matches introduction.
  // Perhaps generated advisor bean definition could be made to depend
  // on the introduction, in which case this would not be a problem.
  @Test
  public void testLockingWorks() {
    Lockable lockable = (Lockable) testBeanProxy;
    assertThat(lockable.locked()).isFalse();

    // Invoke a non-advised method
    testBeanProxy.getAge();

    testBeanProxy.setName("");
    lockable.lock();
    assertThatIllegalStateException().as("should be locked").isThrownBy(() ->
            testBeanProxy.setName(" "));
  }

}

class NonAnnotatedMakeLockable {

  public void checkNotLocked(Lockable mixin) {
    if (mixin.locked()) {
      throw new IllegalStateException("locked");
    }
  }
}
