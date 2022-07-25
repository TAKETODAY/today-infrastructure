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

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for overloaded advice.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class OverloadedAdviceTests {

  @Test
  public void testExceptionOnConfigParsingWithMismatchedAdviceMethod() {
    try {
      new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
    }
    catch (BeanCreationException ex) {
      Throwable cause = ex.getRootCause();
      boolean condition = cause instanceof IllegalArgumentException;
      assertThat(condition).as("Should be IllegalArgumentException").isTrue();
      assertThat(cause.getMessage().contains("invalidAbsoluteTypeName")).as("invalidAbsoluteTypeName should be detected by AJ").isTrue();
    }
  }

  @Test
  public void testExceptionOnConfigParsingWithAmbiguousAdviceMethod() {
    try {
      new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-ambiguous.xml", getClass());
    }
    catch (BeanCreationException ex) {
      Throwable cause = ex.getRootCause();
      boolean condition = cause instanceof IllegalArgumentException;
      assertThat(condition).as("Should be IllegalArgumentException").isTrue();
      assertThat(cause.getMessage().contains("Cannot resolve method 'myBeforeAdvice' to a unique method")).as("Cannot resolve method 'myBeforeAdvice' to a unique method").isTrue();
    }
  }

}

class OverloadedAdviceTestAspect {

  public void myBeforeAdvice(String name) {
    // no-op
  }

  public void myBeforeAdvice(int age) {
    // no-op
  }
}

