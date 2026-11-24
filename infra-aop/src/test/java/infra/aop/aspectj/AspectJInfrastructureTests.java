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

package infra.aop.aspectj;

import org.aspectj.bridge.IMessage;
import org.junit.jupiter.api.Test;

import infra.aop.AfterAdvice;
import infra.aop.BeforeAdvice;
import infra.aop.framework.AopConfigException;
import infra.aop.support.AopUtils;
import infra.aop.support.DefaultPointcutAdvisor;
import infra.beans.factory.BeanFactory;
import infra.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AspectJInfrastructureTests {

  @Test
  void simpleAspectFactoryCreatesInstancesAndReportsOrder() {
    var factory = new SimpleAspectInstanceFactory(OrderedAspect.class) {
      @Override
      protected int getOrderForAspectClass(Class<?> aspectClass) {
        return 42;
      }
    };
    assertThat(factory.getAspectClass()).isEqualTo(OrderedAspect.class);
    assertThat(factory.getAspectClassLoader()).isEqualTo(OrderedAspect.class.getClassLoader());
    assertThat(factory.getAspectInstance()).isInstanceOf(OrderedAspect.class);
    assertThat(factory.getOrder()).isEqualTo(42);
    assertThat(AopUtils.isAopProxy(factory.getAspectInstance())).isFalse();
  }

  @Test
  void simpleAspectFactoryRejectsMissingDefaultConstructor() {
    var factory = new SimpleAspectInstanceFactory(NoDefaultConstructorAspect.class);
    assertThatExceptionOfType(AopConfigException.class).isThrownBy(factory::getAspectInstance)
            .withMessageContaining("No default constructor");
  }

  @Test
  void simpleAspectFactoryReportsNonInstantiableTypesAndPreservesConstructorFailure() {
    var abstractFactory = new SimpleAspectInstanceFactory(AbstractAspect.class);
    assertThatExceptionOfType(AopConfigException.class).isThrownBy(abstractFactory::getAspectInstance)
            .withMessageContaining("Unable to instantiate aspect class").withCauseInstanceOf(InstantiationException.class);

    var throwingFactory = new SimpleAspectInstanceFactory(ThrowingAspect.class);
    assertThatExceptionOfType(AopConfigException.class).isThrownBy(throwingFactory::getAspectInstance)
            .withMessageContaining("Failed to invoke aspect constructor")
            .withCauseInstanceOf(IllegalStateException.class)
            .satisfies(ex -> assertThat(ex.getCause()).hasMessage("aspect initialization failed"));
  }

  @Test
  void ordinaryAdviceIsClassifiedWithoutAspectJPrecedenceInformation() {
    var before = new DefaultPointcutAdvisor(mock(BeforeAdvice.class));
    var after = new DefaultPointcutAdvisor(mock(AfterAdvice.class));

    assertThat(AspectJAopUtils.getAspectJPrecedenceInformationFor(before)).isNull();
    assertThat(AspectJAopUtils.isBeforeAdvice(before)).isTrue();
    assertThat(AspectJAopUtils.isAfterAdvice(before)).isFalse();
    assertThat(AspectJAopUtils.isBeforeAdvice(after)).isFalse();
    assertThat(AspectJAopUtils.isAfterAdvice(after)).isTrue();
  }

  @Test
  void aspectJProxyUtilsRecognizesAspectJAdvisorsAndJavaIdentifiers() {
    var advisors = new java.util.ArrayList<infra.aop.Advisor>();
    assertThat(AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(advisors)).isFalse();

    var pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression("execution(* *(..))");
    advisors.add(new DefaultPointcutAdvisor(pointcut, mock(BeforeAdvice.class)));
    assertThat(AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(advisors)).isTrue();
    assertThat(AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(advisors)).isFalse();
    assertThat(AspectJProxyUtils.isVariableName(null)).isFalse();
    assertThat(AspectJProxyUtils.isVariableName("9value")).isFalse();
    assertThat(AspectJProxyUtils.isVariableName("value-name")).isFalse();
    assertThat(AspectJProxyUtils.isVariableName("value_9")).isTrue();
  }

  @Test
  void adviceImplementationsExposeTheirAdviceKinds() throws Exception {
    var pointcut = new AspectJExpressionPointcut();
    var factory = mock(AspectInstanceFactory.class);
    var method = AspectJInfrastructureTests.class.getDeclaredMethod("adviceMethod");
    var after = new AspectJAfterAdvice(method, pointcut, factory);
    var returning = new AspectJAfterReturningAdvice(method, pointcut, factory);
    var throwing = new AspectJAfterThrowingAdvice(method, pointcut, factory);

    assertThat(after.isBeforeAdvice()).isFalse();
    assertThat(after.isAfterAdvice()).isTrue();
    assertThat(returning.isBeforeAdvice()).isFalse();
    assertThat(returning.isAfterAdvice()).isTrue();
    assertThat(throwing.isBeforeAdvice()).isFalse();
    assertThat(throwing.isAfterAdvice()).isTrue();
  }

  void adviceMethod() {
  }

  @Test
  void factoriesWithoutAnExplicitOrderHaveLowestPrecedence() {
    assertThat(new SimpleAspectInstanceFactory(Object.class).getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    var aspect = new Object();
    var factory = new SingletonAspectInstanceFactory(aspect);
    assertThat(factory.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    assertThat(factory.getAspectClassLoader()).isSameAs(aspect.getClass().getClassLoader());
  }

  @Test
  void expressionAdvisorDelegatesPointcutConfiguration() {
    var advisor = new AspectJExpressionPointcutAdvisor();
    advisor.setExpression("execution(* *(..))");
    advisor.setLocation("test.xml:1");
    advisor.setParameterNames("value");
    advisor.setParameterTypes(String.class);
    advisor.setBeanFactory(mock(BeanFactory.class));

    assertThat(advisor.getExpression()).isEqualTo("execution(* *(..))");
    assertThat(advisor.getLocation()).isEqualTo("test.xml:1");
    assertThat(advisor.getPointcut()).isSameAs(advisor.getPointcut());
  }

  @Test
  void weaverMessageHandlerAcceptsAllKnownKinds() throws Exception {
    var handler = new AspectJWeaverMessageHandler();
    for (IMessage.Kind kind : new IMessage.Kind[] {
            IMessage.DEBUG, IMessage.INFO, IMessage.WEAVEINFO, IMessage.WARNING,
            IMessage.ERROR, IMessage.ABORT }) {
      IMessage message = mock(IMessage.class);
      given(message.getKind()).willReturn(kind);
      given(message.getMessage()).willReturn("message");
      handler.handleMessage(message);
    }
    assertThat(handler.isIgnoring(IMessage.INFO)).isFalse();
    handler.dontIgnore(IMessage.INFO);
    handler.ignore(IMessage.INFO);
  }

  static class OrderedAspect implements Ordered {
    @Override
    public int getOrder() {
      return 42;
    }
  }

  static class NoDefaultConstructorAspect {
    NoDefaultConstructorAspect(String value) {
    }
  }

  abstract static class AbstractAspect {
  }

  static class ThrowingAspect {

    ThrowingAspect() {
      throw new IllegalStateException("aspect initialization failed");
    }
  }

}
