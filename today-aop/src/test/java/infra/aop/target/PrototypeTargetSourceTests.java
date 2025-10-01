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

package infra.aop.target;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.SideEffectBean;

import static infra.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class PrototypeTargetSourceTests {

  /** Initial count value set in bean factory XML */
  private static final int INITIAL_COUNT = 10;

  private StandardBeanFactory beanFactory;

  @BeforeEach
  public void setup() {
    this.beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
            qualifiedResource(PrototypeTargetSourceTests.class, "context.xml"));
  }

  /**
   * Test that multiple invocations of the prototype bean will result
   * in no change to visible state, as a new instance is used.
   * With the singleton, there will be change.
   */
  @Test
  public void testPrototypeAndSingletonBehaveDifferently() {
    SideEffectBean singleton = (SideEffectBean) beanFactory.getBean("singleton");
    assertThat(singleton.getCount()).isEqualTo(INITIAL_COUNT);
    singleton.doWork();
    assertThat(singleton.getCount()).isEqualTo((INITIAL_COUNT + 1));

    SideEffectBean prototype = (SideEffectBean) beanFactory.getBean("prototype");
    assertThat(prototype.getCount()).isEqualTo(INITIAL_COUNT);
    prototype.doWork();
    assertThat(prototype.getCount()).isEqualTo(INITIAL_COUNT);
  }

}
