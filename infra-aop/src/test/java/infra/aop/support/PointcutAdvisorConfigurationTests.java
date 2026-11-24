/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.aop.support;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import infra.aop.Pointcut;
import infra.beans.factory.BeanFactory;
import infra.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PointcutAdvisorConfigurationTests {

  private final MethodInterceptor advice = invocation -> invocation.proceed();

  @Test
  void lazilyCachesOneInstanceOfPrototypeAdvice() {
    BeanFactory beans = mock();
    given(beans.getBean("advice", Advice.class)).willReturn(advice);
    var advisor = new NamedAdvisor();
    advisor.setAdviceBeanName("advice");
    advisor.setBeanFactory(beans);

    assertThat(advisor.getAdviceBeanName()).isEqualTo("advice");
    assertThat(advisor.toString()).contains("advice bean 'advice'");
    verifyNoInteractions(beans);
    assertThat(advisor.getAdvice()).isSameAs(advice);
    assertThat(advisor.getAdvice()).isSameAs(advice);
    verify(beans).getBean("advice", Advice.class);
  }

  @Test
  void deserializedAdvisorAcceptsExplicitAdviceWithoutABeanFactory() throws Exception {
    var advisor = SerializationTestUtils.serializeAndDeserialize(new NamedAdvisor());
    advisor.setAdvice(advice);

    assertThat(advisor.getAdvice()).isSameAs(advice);
    assertThat(advisor.toString()).endsWith(": advice " + advice);
    assertThat(new NamedAdvisor()).hasSameHashCodeAs(advisor);
    var configurable = new DefaultBeanFactoryPointcutAdvisor();
    configurable.setAdviceBeanName("advice");
    assertThat(configurable.toString()).contains("pointcut", "advice bean 'advice'");
  }

  @Test
  void regexpAdvisorConstructorsPreserveAdviceAndApplyConfiguredPatterns() throws Exception {
    var advisor = new RegexpMethodPointcutAdvisor(advice);
    var single = new RegexpMethodPointcutAdvisor(".*length", advice);
    var multiple = new RegexpMethodPointcutAdvisor(new String[] { ".*length", ".*isEmpty" }, advice);

    assertThat(advisor.getAdvice()).isSameAs(advice);
    var isEmpty = String.class.getMethod("isEmpty");
    assertThat(single.getPointcut().getMethodMatcher().matches(isEmpty, String.class)).isFalse();
    assertThat(multiple.getPointcut().getMethodMatcher().matches(isEmpty, String.class)).isTrue();
    assertThat(multiple.toString()).contains("pointcut patterns", ".*length", ".*isEmpty");
  }

  @Test
  void regexpPointcutEqualityIncludesExcludedPatterns() {
    var first = new JdkRegexpMethodPointcut();
    first.setPattern(".*");
    first.setExcludedPatterns(".*length");
    var second = new JdkRegexpMethodPointcut();
    second.setPattern(".*");

    assertThat(first.getExcludedPatterns()).containsExactly(".*length");
    assertThat(first.equals(first)).isTrue();
    assertThat(first).isNotEqualTo(Pointcut.TRUE).isNotEqualTo(second);
    second.setExcludedPatterns(".*length");
    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
  }

  @Test
  void nameMatchAdvisorDelegatesClassAndMethodConfigurationToItsPointcut() throws Exception {
    var advisor = new NameMatchMethodPointcutAdvisor(advice);
    advisor.setClassFilter(type -> type == String.class);
    advisor.setMappedNames("length", "isEmpty");
    assertThat(advisor.addMethodName("trim")).isSameAs(advisor.getPointcut());
    var pointcut = advisor.getPointcut();

    assertThat(pointcut.getClassFilter().matches(String.class)).isTrue();
    assertThat(pointcut.getClassFilter().matches(StringBuilder.class)).isFalse();
    assertThat(pointcut.getMethodMatcher().matches(String.class.getMethod("trim"), String.class)).isTrue();
    assertThat(pointcut.getMethodMatcher().matches(String.class.getMethod("toString"), String.class)).isFalse();
    assertThat(advisor.toString()).contains(": advice [");
  }

  @Test
  void introductionAdvisorEqualityIncludesIntroducedInterfaces() {
    var first = new DefaultIntroductionAdvisor(advice, () -> new Class<?>[] { Runnable.class });
    var second = new DefaultIntroductionAdvisor(advice, () -> new Class<?>[] { Runnable.class });
    assertThat(first.equals(first)).isTrue();
    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second).isNotEqualTo(advice);
    assertThat(first.toString()).contains("interfaces", "java.lang.Runnable");
    second.addInterface(Comparable.class);
    assertThat(first).isNotEqualTo(second);
    assertThatIllegalArgumentException().isThrownBy(() -> new DefaultIntroductionAdvisor(advice, () -> new Class<?>[0]))
            .withMessageContaining("implements no interfaces");
  }

  private static class NamedAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    @Override
    public Pointcut getPointcut() {
      return Pointcut.TRUE;
    }
  }
}
