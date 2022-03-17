/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.parsing.BeanDefinitionParsingException;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TCK-style unit tests for handling circular use of the {@link Import} annotation.
 * Explore the subclass hierarchy for specific concrete implementations.
 *
 * @author Chris Beams
 */
public abstract class AbstractCircularImportDetectionTests {

  protected abstract ConfigurationClassParser newParser();

  protected abstract String loadAsConfigurationSource(Class<?> clazz) throws Exception;

  @Test
  public void simpleCircularImportIsDetected() throws Exception {
    boolean threw = false;
    try {
      newParser().parse(loadAsConfigurationSource(A.class), "A");
    }
    catch (BeanDefinitionParsingException ex) {
      assertThat(ex.getMessage().contains(
              "Illegal attempt by @Configuration class 'AbstractCircularImportDetectionTests.B' " +
                      "to import class 'AbstractCircularImportDetectionTests.A'")).as("Wrong message. Got: " + ex.getMessage()).isTrue();
      threw = true;
    }
    assertThat(threw).isTrue();
  }

  @Test
  public void complexCircularImportIsDetected() throws Exception {
    boolean threw = false;
    try {
      newParser().parse(loadAsConfigurationSource(X.class), "X");
    }
    catch (BeanDefinitionParsingException ex) {
      assertThat(ex.getMessage().contains(
              "Illegal attempt by @Configuration class 'AbstractCircularImportDetectionTests.Z2' " +
                      "to import class 'AbstractCircularImportDetectionTests.Z'")).as("Wrong message. Got: " + ex.getMessage()).isTrue();
      threw = true;
    }
    assertThat(threw).isTrue();
  }

  @Configuration
  @Import(B.class)
  static class A {

    @Bean
    TestBean b1() {
      return new TestBean();
    }
  }

  @Configuration
  @Import(A.class)
  static class B {

    @Bean
    TestBean b2() {
      return new TestBean();
    }
  }

  @Configuration
  @Import({ Y.class, Z.class })
  class X {

    @Bean
    TestBean x() {
      return new TestBean();
    }
  }

  @Configuration
  class Y {

    @Bean
    TestBean y() {
      return new TestBean();
    }
  }

  @Configuration
  @Import({ Z1.class, Z2.class })
  class Z {

    @Bean
    TestBean z() {
      return new TestBean();
    }
  }

  @Configuration
  class Z1 {

    @Bean
    TestBean z1() {
      return new TestBean();
    }
  }

  @Configuration
  @Import(Z.class)
  class Z2 {

    @Bean
    TestBean z2() {
      return new TestBean();
    }
  }

}
