/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
