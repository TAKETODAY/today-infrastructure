/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.aot;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.testfixture.beans.factory.aot.MockBeanRegistrationCode;

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