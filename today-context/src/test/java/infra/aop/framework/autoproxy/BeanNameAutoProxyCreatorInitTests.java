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

package infra.aop.framework.autoproxy;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import infra.aop.MethodBeforeAdvice;
import infra.beans.testfixture.beans.TestBean;
import infra.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 */
public class BeanNameAutoProxyCreatorInitTests {

  @Test
  public void testIgnoreAdvisorThatIsCurrentlyInCreation() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
    TestBean bean = (TestBean) ctx.getBean("bean");
    bean.setName("foo");
    assertThat(bean.getName()).isEqualTo("foo");
    assertThatIllegalArgumentException().isThrownBy(() ->
            bean.setName(null));
  }

}

class NullChecker implements MethodBeforeAdvice {
  @Override
  public void before(MethodInvocation invocation) throws Throwable {
    check(invocation.getArguments());
  }

  private void check(Object[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i] == null) {
        throw new IllegalArgumentException("Null argument at position " + i);
      }
    }
  }

}
