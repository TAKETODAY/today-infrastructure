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

package infra.aop.framework.autoproxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import infra.aop.AopInfrastructureBean;
import infra.aop.Advisor;
import infra.aop.ClassFilter;
import infra.aop.framework.Advised;
import infra.aop.framework.ProxyFactory;
import infra.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import infra.aop.support.AopUtils;
import infra.aop.support.DefaultPointcutAdvisor;
import infra.aop.support.NameMatchMethodPointcut;
import infra.aop.target.SingletonTargetSource;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.SmartClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AutoProxyCreatorLifecycleTests {

  @Test
  void retrieverSkipsAdvisorsInCreationAndAdvisorsDependingOnTheBeanBeingCreated() {
    ConfigurableBeanFactory factory = mock();
    given(factory.getBeanNamesForType(Advisor.class, true, false))
            .willReturn(new String[] { "inProgress", "dependent", "independent" });
    given(factory.isCurrentlyInCreation("inProgress")).willReturn(true);
    given(factory.isCurrentlyInCreation("service")).willReturn(true);
    given(factory.getBean("dependent", Advisor.class)).willThrow(
            new BeanCreationException("dependent", "cannot resolve target", new BeanCurrentlyInCreationException("service")));
    Advisor independent = advisor("independent:");
    given(factory.getBean("independent", Advisor.class)).willReturn(independent);

    assertThat(new BeanFactoryAdvisorRetriever(factory).retrieveAdvisorBeans()).containsExactly(independent);
    verify(factory, never()).getBean("inProgress", Advisor.class);
  }

  @Test
  void retrieverPropagatesCreationFailuresWithoutAnActiveCircularDependency() {
    ConfigurableBeanFactory factory = mock();
    given(factory.getBeanNamesForType(Advisor.class, true, false)).willReturn(new String[] { "broken" });
    var failure = new BeanCreationException("broken", "advisor initialization failed");
    given(factory.getBean("broken", Advisor.class)).willThrow(failure);

    assertThatThrownBy(new BeanFactoryAdvisorRetriever(factory)::retrieveAdvisorBeans).isSameAs(failure);
  }

  @Test
  void fallsBackToSingletonTargetSourceAndSupportsACustomAdapterRegistry() throws Exception {
    var creator = new BeanNameAutoProxyCreator();
    var registry = new DefaultAdvisorAdapterRegistry();
    creator.setAdvisorAdapterRegistry(registry);
    var service = new Service();

    assertThat(creator.getTargetSource(service, "service")).isEqualTo(new SingletonTargetSource(service));
  }

  @Test
  void predictsTheProxyTypeUsingTheExplicitlyExposedInterface() {
    StandardBeanFactory factory = new StandardBeanFactory();
    var definition = new RootBeanDefinition(Service.class);
    definition.setAttribute(AutoProxyUtils.EXPOSED_INTERFACES_ATTRIBUTE, Lookup.class);
    factory.registerBeanDefinition("service", definition);
    var creator = new BeanNameAutoProxyCreator();
    creator.setBeanFactory(factory);
    creator.setBeanNames("service");

    assertThat(creator.predictBeanType(Service.class, "service")).isNull();
    assertThat(creator.determineBeanType(Service.class, "other")).isEqualTo(Service.class);
    Class<?> predicted = creator.determineBeanType(Service.class, "service");
    assertThat(creator.determineBeanType(Service.class, "service")).isSameAs(predicted);
    assertThat(creator.predictBeanType(Service.class, "service")).isSameAs(predicted);
    assertThat(creator.postProcessBeforeInstantiation(Service.class, "service")).isNull();

    Object proxy = creator.postProcessAfterInitialization(new Service(), "service");
    assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();
    assertThat(proxy).isExactlyInstanceOf(predicted).isNotInstanceOf(Runnable.class);
    assertThat(((Lookup) proxy).value()).isEqualTo("target");
    assertThat(AutoProxyUtils.determineTargetClass(factory, "service")).isEqualTo(Service.class);
  }

  @Test
  void customTargetSourceIsUsedInsteadOfWrappingTheInitializedBeanAgain() throws Exception {
    var creator = new BeanNameAutoProxyCreator();
    creator.setBeanNames("service");
    Service target = new Service();
    var source = new SingletonTargetSource(target);
    creator.setCustomTargetSourceCreators((type, name) -> null, (type, name) -> source);

    Class<?> predicted = creator.determineBeanType(Service.class, "service");
    Object proxy = creator.postProcessBeforeInstantiation(Service.class, "service");

    assertThat(proxy).isExactlyInstanceOf(predicted);
    assertThat(((Advised) proxy).getTargetSource().getTarget()).isSameAs(target);
    assertThat(creator.postProcessAfterInitialization(proxy, "service")).isSameAs(proxy);
  }

  @Test
  void infrastructureCreatorIgnoresApplicationAdvisors() {
    StandardBeanFactory factory = new StandardBeanFactory();
    DefaultPointcutAdvisor infrastructure = advisor("infrastructure:");
    var definition = new RootBeanDefinition(DefaultPointcutAdvisor.class, () -> infrastructure);
    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    factory.registerBeanDefinition("infrastructureAdvisor", definition);
    factory.registerSingleton("applicationAdvisor", advisor("application:"));
    var creator = new InfrastructureAdvisorAutoProxyCreator();
    creator.setBeanFactory(factory);

    Object proxy = creator.postProcessAfterInitialization(new Service(), "service");

    assertThat(((Advised) proxy).getAdvisors()).containsExactly(infrastructure);
    assertThat(((Lookup) proxy).value()).isEqualTo("infrastructure:target");
  }

  @Test
  void explicitPrefixSelectsSpecificAdviceBeforeCommonAdvice() {
    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerSingleton("selected.advisor", advisor("specific:"));
    factory.registerSingleton("creator.advisor", advisor("excluded:"));
    factory.registerSingleton("common", (MethodInterceptor) invocation -> "common:" + invocation.proceed());
    var creator = new DefaultAdvisorAutoProxyCreator();
    creator.setUsePrefix(true);
    creator.setAdvisorBeanNamePrefix("selected.");
    creator.setBeanName("creator");
    creator.setBeanFactory(factory);
    creator.setInterceptorNames("common");
    creator.setApplyCommonInterceptorsFirst(false);

    Lookup proxy = (Lookup) creator.postProcessAfterInitialization(new Service(), "service");

    assertThat(proxy.value()).isEqualTo("specific:common:target");
  }

  @Test
  void advisingPostProcessorPredictsProxyTypeUsingOriginalClassLoader() {
    var processor = new AdvisingProcessor();
    processor.setProxyClassLoader(new OverridingLoader(getClass().getClassLoader()));
    var pointcut = new NameMatchMethodPointcut();
    pointcut.setClassFilter((ClassFilter) type -> type == Service.class);
    pointcut.setMappedName("value");
    processor.configure(new DefaultPointcutAdvisor(pointcut, (MethodInterceptor) invocation -> "local:" + invocation.proceed()));

    Class<?> predicted = processor.determineBeanType(Service.class, "service");
    Object proxy = processor.postProcessAfterInitialization(new Service(), "service");
    assertThat(proxy).isExactlyInstanceOf(predicted);
    assertThat(proxy.getClass().getClassLoader()).isSameAs(getClass().getClassLoader());
    assertThat(((Lookup) proxy).value()).isEqualTo("local:target");
    assertThat(processor.determineBeanType(String.class, "text")).isEqualTo(String.class);
  }

  @Test
  void advisingPostProcessorLeavesUnconfiguredAndInfrastructureBeansUnchanged() {
    var processor = new AdvisingProcessor();
    var bean = new Service();
    assertThat(processor.isEligible(bean, "service")).isFalse();
    assertThat(processor.postProcessAfterInitialization(bean, "service")).isSameAs(bean);

    processor.configure(advisor("local:"));
    var infrastructure = new AopInfrastructureBean() { };
    assertThat(processor.postProcessAfterInitialization(infrastructure, "infrastructure")).isSameAs(infrastructure);
  }

  @Test
  void advisingPostProcessorInsertsAdviceBeforeTerminalInterceptorOnTargetlessProxy() {
    var proxyFactory = new ProxyFactory(Lookup.class, (MethodInterceptor) invocation -> "terminal");
    Object proxy = proxyFactory.getProxy();
    var processor = new AdvisingProcessor();
    processor.configure(advisor("local:"));

    assertThat(processor.postProcessAfterInitialization(proxy, "lookup")).isSameAs(proxy);
    assertThat(((Lookup) proxy).value()).isEqualTo("local:terminal");
  }

  @Test
  void advisingPostProcessorAppendsAdviceToAnExistingProxyWithATarget() {
    var proxyFactory = new ProxyFactory(new Service());
    proxyFactory.addAdvisor(advisor("existing:"));
    Object proxy = proxyFactory.getProxy();
    var processor = new AdvisingProcessor();
    processor.configure(advisor("local:"));

    assertThat(processor.postProcessAfterInitialization(proxy, "service")).isSameAs(proxy);
    assertThat(((Lookup) proxy).value()).isEqualTo("existing:local:target");
  }

  @Test
  void beanDefinitionCanRequireAClassProxyOrRestrictTheExposedInterfaces() {
    var factory = new StandardBeanFactory();
    var classDefinition = new RootBeanDefinition(Service.class);
    classDefinition.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, true);
    factory.registerBeanDefinition("classProxy", classDefinition);
    var interfaceDefinition = new RootBeanDefinition(Service.class);
    interfaceDefinition.setAttribute(AutoProxyUtils.EXPOSED_INTERFACES_ATTRIBUTE, Lookup.class);
    factory.registerBeanDefinition("interfaceProxy", interfaceDefinition);
    var processor = new AdvisingProcessor();
    processor.configure(advisor("local:"));
    processor.setBeanFactory(factory);

    Object classProxy = processor.postProcessAfterInitialization(new Service(), "classProxy");
    assertThat(AopUtils.isCglibProxy(classProxy)).isTrue();
    assertThat(((Service) classProxy).value()).isEqualTo("local:target");

    processor.setProxyTargetClass(true);
    Object interfaceProxy = processor.postProcessAfterInitialization(new Service(), "interfaceProxy");
    assertThat(AopUtils.isJdkDynamicProxy(interfaceProxy)).isTrue();
    assertThat(interfaceProxy).isInstanceOf(Lookup.class).isNotInstanceOf(Runnable.class);
  }

  private static DefaultPointcutAdvisor advisor(String prefix) {
    return new DefaultPointcutAdvisor((MethodInterceptor) invocation -> prefix + invocation.proceed());
  }

  private static class AdvisingProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

    void configure(DefaultPointcutAdvisor advisor) {
      this.advisor = advisor;
    }
  }

  private static class OverridingLoader extends ClassLoader implements SmartClassLoader {

    OverridingLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    public ClassLoader getOriginalClassLoader() {
      return getParent();
    }
  }

  interface Lookup {

    String value();
  }

  static class Service implements Lookup, Runnable {

    @Override
    public String value() {
      return "target";
    }

    @Override
    public void run() {
    }
  }
}
