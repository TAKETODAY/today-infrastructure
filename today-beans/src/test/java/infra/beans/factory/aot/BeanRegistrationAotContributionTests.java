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

package infra.beans.factory.aot;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import infra.aot.test.generate.TestGenerationContext;
import infra.beans.testfixture.beans.factory.aot.MockBeanRegistrationCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/21 20:59
 */
class BeanRegistrationAotContributionTests {

  @Test
  void concatWithBothNullReturnsNull() {
    assertThat(BeanRegistrationAotContribution.concat(null, null)).isNull();
  }

  @Test
  void concatWithFirstNullReturnsSecondAsIs() {
    BeanRegistrationAotContribution contribution = mock(BeanRegistrationAotContribution.class);
    assertThat(BeanRegistrationAotContribution.concat(null, contribution)).isSameAs(contribution);
    verifyNoInteractions(contribution);
  }

  @Test
  void concatWithSecondNullReturnsFirstAsIs() {
    BeanRegistrationAotContribution contribution = mock(BeanRegistrationAotContribution.class);
    assertThat(BeanRegistrationAotContribution.concat(contribution, null)).isSameAs(contribution);
    verifyNoInteractions(contribution);
  }

  @Test
  void concatApplyContributionsInOrder() {
    BeanRegistrationAotContribution first = mock(BeanRegistrationAotContribution.class);
    BeanRegistrationAotContribution second = mock(BeanRegistrationAotContribution.class);
    BeanRegistrationAotContribution combined = BeanRegistrationAotContribution.concat(first, second);
    assertThat(combined).isNotNull();
    TestGenerationContext generationContext = new TestGenerationContext();
    BeanRegistrationCode beanRegistrationCode = new MockBeanRegistrationCode(generationContext);
    combined.applyTo(generationContext, beanRegistrationCode);
    InOrder ordered = inOrder(first, second);
    ordered.verify(first).applyTo(generationContext, beanRegistrationCode);
    ordered.verify(second).applyTo(generationContext, beanRegistrationCode);
  }

}