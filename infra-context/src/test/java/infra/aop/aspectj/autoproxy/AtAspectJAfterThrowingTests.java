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

package infra.aop.aspectj.autoproxy;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.aop.support.AopUtils;
import infra.beans.testfixture.beans.ITestBean;
import infra.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
public class AtAspectJAfterThrowingTests {

  @Test
  public void testAccessThrowable() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

    ITestBean bean = (ITestBean) ctx.getBean("testBean");
    ExceptionHandlingAspect aspect = (ExceptionHandlingAspect) ctx.getBean("aspect");

    assertThat(AopUtils.isAopProxy(bean)).isTrue();
    IOException exceptionThrown = null;
    try {
      bean.unreliableFileOperation();
    }
    catch (IOException ex) {
      exceptionThrown = ex;
    }

    assertThat(aspect.handled).isEqualTo(1);
    assertThat(aspect.lastException).isSameAs(exceptionThrown);
  }

}

@Aspect
class ExceptionHandlingAspect {

  public int handled;

  public IOException lastException;

  @AfterThrowing(pointcut = "within(infra.beans.testfixture.beans.ITestBean+)", throwing = "ex")
  public void handleIOException(IOException ex) {
    handled++;
    lastException = ex;
  }

}
