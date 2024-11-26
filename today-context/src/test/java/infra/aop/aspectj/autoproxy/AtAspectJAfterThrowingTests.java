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
