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
