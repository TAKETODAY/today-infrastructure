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

package infra.aop.aspectj.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Test;

import infra.aop.aspectj.AspectJExpressionPointcut;
import infra.aop.framework.AopConfigException;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.support.StaticListableBeanFactory;
import infra.core.Ordered;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class AspectJAdvisorFactoryValidationTests {

  private final ReflectiveAspectJAdvisorFactory factory = new ReflectiveAspectJAdvisorFactory();

  @Test
  void reportsWhichClassIsNotAnAspect() {
    assertThatExceptionOfType(NotAnAtAspectException.class)
            .isThrownBy(() -> factory.validate(String.class))
            .withMessageContaining("java.lang.String is not an @AspectJ aspect")
            .satisfies(exception -> assertThat(exception.getNonAspectClass()).isEqualTo(String.class));
  }

  @Test
  void rejectsControlFlowInstantiationModels() {
    assertThatExceptionOfType(AopConfigException.class)
            .isThrownBy(() -> factory.validate(ControlFlow.class)).withMessageContaining("uses percflow instantiation model");
    assertThatExceptionOfType(AopConfigException.class)
            .isThrownBy(() -> factory.validate(ControlFlowBelow.class)).withMessageContaining("uses percflowbelow instantiation model");
  }

  @Test
  void requiresADefaultImplementationForIntroductions() {
    var instances = new SingletonMetadataAwareAspectInstanceFactory(new MissingImplementation(), "introduction");

    assertThatIllegalStateException().isThrownBy(() -> factory.getAdvisors(instances))
            .withMessage("'defaultImpl' attribute must be set on DeclareParents");
  }

  @Test
  void pointcutDeclarationsAreNotAdvice() throws Exception {
    var instances = new SingletonMetadataAwareAspectInstanceFactory(new OrderedAspect(), "ordering");
    var method = OrderedAspect.class.getDeclaredMethod("operations");

    assertThat(factory.getAdvice(method, new AspectJExpressionPointcut(), instances, 0, "ordering")).isNull();
    var advisor = (InstantiationModelAwarePointcutAdvisorImpl) factory.getAdvisor(method, instances, 0, "ordering");
    assertThat(advisor.isLazy()).isFalse();
    assertThat(advisor.isAdviceInstantiated()).isTrue();
    assertThat(advisor.isBeforeAdvice()).isFalse();
    assertThat(advisor.isAfterAdvice()).isFalse();
    assertThat(advisor.toString()).contains("operations", "execution(* *(..))");
  }

  @Test
  void rejectsAnAdviceAnnotationWithNoPointcutExpression() throws Exception {
    var instances = new SingletonMetadataAwareAspectInstanceFactory(new MissingExpression(), "missing");
    var method = MissingExpression.class.getDeclaredMethod("before");

    assertThatIllegalArgumentException().isThrownBy(() -> factory.getAdvisor(method, instances, 0, "missing"))
            .withMessageContaining("not a valid AspectJ annotation")
            .withCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void discoversNamesOnlyOnAnnotatedAdviceMethods() throws Exception {
    assertThat(factory.parameterNameDiscoverer.getParameterNames(OrderedAspect.class.getDeclaredMethod("helper", String.class)))
            .isNull();
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> factory.parameterNameDiscoverer.getParameterNames(OrderedAspect.class.getDeclaredConstructor()))
            .withMessageContaining("cannot handle constructor advice");
  }

  @Test
  void aspectAnnotationDescriptorRetainsTheOriginalAnnotation() throws Exception {
    var method = OrdinaryAspect.class.getDeclaredMethod("before");
    var annotation = method.getAnnotation(Before.class);
    var descriptor = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);

    assertThat(descriptor.getAnnotation()).isEqualTo(annotation);
    assertThat(descriptor.toString()).isEqualTo(annotation.toString());
    assertThatIllegalStateException().isThrownBy(() -> new AbstractAspectJAdvisorFactory.AspectJAnnotation(
            PerTargetAspect.class.getAnnotation(Aspect.class))).withMessageContaining("Unknown annotation type");
  }

  @Test
  void resolvesAnOrderedSingletonAspectThroughANonConfigurableFactory() {
    var beans = new StaticListableBeanFactory();
    var aspect = new OrderedAspect();
    beans.addBean("ordering", aspect);
    var instances = new BeanFactoryAspectInstanceFactory(beans, "ordering");
    var lazy = new LazySingletonAspectInstanceFactoryDecorator(instances);

    assertThat(lazy.getOrder()).isEqualTo(17);
    assertThat(lazy.getAspectClassLoader()).isSameAs(ClassUtils.getDefaultClassLoader());
    assertThat(lazy.getAspectCreationMutex()).isNull();
    assertThat(lazy.getAspectInstance()).isSameAs(aspect);
    assertThat(lazy.toString()).contains("decorating BeanFactoryAspectInstanceFactory: bean name 'ordering'");
    assertThat(new BeanFactoryAspectInstanceFactory(beans, "absent", OrderedAspect.class).getAspectMetadata().getAspectClass())
            .isEqualTo(OrderedAspect.class);
  }

  @Test
  void ignoresIncompatibleAspectsButKeepsCompatibleAdvisorsCached() {
    var beans = new StandardBeanFactory();
    beans.registerBeanDefinition("badScope", new RootBeanDefinition(PerTargetAspect.class));
    beans.registerBeanDefinition("badExpression", new RootBeanDefinition(MissingExpression.class));
    beans.registerBeanDefinition("valid", new RootBeanDefinition(OrdinaryAspect.class));
    var builder = new BeanFactoryAspectJAdvisorsBuilder(beans);

    var advisors = builder.buildAspectJAdvisors();
    assertThat(advisors).singleElement().satisfies(advisor -> assertThat(advisor.toString()).contains("before"));
    assertThat(builder.buildAspectJAdvisors()).containsExactlyElementsOf(advisors);
    assertThat(beans.containsSingleton("badScope")).isFalse();
    assertThat(beans.containsSingleton("valid")).isFalse();
  }

  @Test
  void beanFactoryAspectUsesTheConfiguredLoaderAndDefaultPrecedence() {
    var beans = new StandardBeanFactory();
    var loader = new ClassLoader(getClass().getClassLoader()) { };
    beans.setBeanClassLoader(loader);
    beans.registerBeanDefinition("aspect", new RootBeanDefinition(OrdinaryAspect.class));
    var instances = new BeanFactoryAspectInstanceFactory(beans, "aspect");

    assertThat(instances.getAspectClassLoader()).isSameAs(loader);
    assertThat(instances.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    assertThatIllegalArgumentException().isThrownBy(() -> new PrototypeAspectInstanceFactory(beans, "aspect"))
            .withMessageContaining("prototype");
  }

  @Aspect("percflow(execution(* *(..)))")
  static class ControlFlow {
  }

  @Aspect("percflowbelow(execution(* *(..)))")
  static class ControlFlowBelow {
  }

  @Aspect
  static class MissingImplementation {

    @DeclareParents("java.lang.Runnable+")
    Object introduced;
  }

  @Aspect
  static class MissingExpression {

    @Before("")
    public void before() {
    }
  }

  @Aspect
  static class OrderedAspect implements Ordered {

    @Override
    public int getOrder() {
      return 17;
    }

    @Pointcut("execution(* *(..))")
    void operations() {
    }

    void helper(String value) {
    }
  }

  @Aspect
  static class OrdinaryAspect {

    @Before("execution(* *(..))")
    void before() {
    }
  }

  @Aspect("pertarget(execution(* *(..)))")
  static class PerTargetAspect {
  }
}
