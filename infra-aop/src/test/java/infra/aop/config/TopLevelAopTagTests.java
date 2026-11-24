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

package infra.aop.config;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

import java.util.List;

import infra.aop.aspectj.AspectJExpressionPointcut;
import infra.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import infra.aop.framework.ProxyFactoryBean;
import infra.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import infra.aop.support.DefaultBeanFactoryPointcutAdvisor;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.RuntimeBeanReference;
import infra.beans.factory.support.AutowireCandidateQualifier;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.ParserContext;
import infra.beans.factory.xml.XmlBeanDefinitionReader;

import static infra.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * Tests that the &lt;aop:config/&gt; element can be used as a top level element.
 *
 * @author Rob Harrop
 * @author Chris Beams
 */
class TopLevelAopTagTests {

  @Test
  void interceptorDecoratorsShareOneProxyAndPreserveTargetMetadata() {
    var registry = new StandardBeanFactory();
    var context = mock(ParserContext.class);
    given(context.getRegistry()).willReturn(registry);
    var target = new RootBeanDefinition(Object.class);
    target.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    target.setPrimary(true);
    target.setLazyInit(true);
    target.setAutowireCandidate(false);
    target.addQualifier(new AutowireCandidateQualifier("qualified"));
    var holder = new BeanDefinitionHolder(target, "service");
    Node node = mock();

    var decorated = decorator(String.class).decorate(node, holder, context);
    assertThat(decorator(Integer.class).decorate(node, decorated, context)).isSameAs(decorated);

    var proxy = (RootBeanDefinition) decorated.getBeanDefinition();
    assertThat(decorated.getBeanName()).isEqualTo("service");
    assertThat(proxy.getBeanClass()).isEqualTo(ProxyFactoryBean.class);
    assertThat(proxy.getDecoratedDefinition().getBeanDefinition()).isSameAs(target);
    assertThat(proxy.isPrototype()).isTrue();
    assertThat(proxy.isPrimary()).isTrue();
    assertThat(proxy.isLazyInit()).isTrue();
    assertThat(proxy.isAutowireCandidate()).isFalse();
    assertThat(proxy.hasQualifier("qualified")).isTrue();
    assertThat(proxy.getPropertyValues().getPropertyValue("interceptorNames"))
            .isEqualTo(List.of("service.string", "service.integer"));
    assertThat(registry.getBeanDefinition("service.string").getBeanClassName()).isEqualTo(String.class.getName());
    assertThat(registry.getBeanDefinition("service.integer").getBeanClassName()).isEqualTo(Integer.class.getName());
  }

  private static AbstractInterceptorDrivenBeanDefinitionDecorator decorator(Class<?> interceptorClass) {
    return new AbstractInterceptorDrivenBeanDefinitionDecorator() {
      @Override
      protected BeanDefinition createInterceptorDefinition(Node node) {
        return new RootBeanDefinition(interceptorClass);
      }
    };
  }

  @Test
  void parse() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
            qualifiedResource(TopLevelAopTagTests.class, "context.xml"));

    assertThat(beanFactory.containsBeanDefinition("testPointcut")).isTrue();
  }

  @Test
  void advisorAndPointcutComponentsRetainSourceAndDescriptiveNames() {
    var definition = new RootBeanDefinition(AspectJExpressionPointcut.class);
    definition.setSource("pointcut.xml");
    var pointcut = new PointcutComponentDefinition("greeting", definition, "execution(* greet(..))");
    assertThat(pointcut.getName()).isEqualTo("greeting");
    assertThat(pointcut.getDescription()).contains("greeting", "execution(* greet(..))");
    assertThat(pointcut.getSource()).isEqualTo("pointcut.xml");

    var advisorDefinition = new RootBeanDefinition(DefaultBeanFactoryPointcutAdvisor.class);
    advisorDefinition.setSource("advisor.xml");
    advisorDefinition.getPropertyValues().add("adviceBeanName", new RuntimeBeanReference("audit"));
    advisorDefinition.getPropertyValues().add("pointcut", new RuntimeBeanReference("greeting"));
    var advisor = new AdvisorComponentDefinition("greetingAdvisor", advisorDefinition);
    assertThat(advisor.getName()).isEqualTo("greetingAdvisor");
    assertThat(advisor.getDescription()).contains("audit", "greeting");
    assertThat(advisor.getSource()).isEqualTo("advisor.xml");
  }

  @Test
  void autoProxyCreatorEscalatesButDoesNotOverwriteUnknownDefinitions() {
    var registry = new StandardBeanFactory();
    var definition = AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
    assertThat(definition.getBeanClassName()).isEqualTo(InfrastructureAdvisorAutoProxyCreator.class.getName());
    assertThat(AopConfigUtils.registerAspectJAutoProxyCreatorIfNecessary(registry)).isNull();
    assertThat(definition.getBeanClassName()).isEqualTo(AspectJAwareAdvisorAutoProxyCreator.class.getName());
    AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
    assertThat(definition.getBeanClassName()).isEqualTo(AspectJAwareAdvisorAutoProxyCreator.class.getName());

    definition.setBeanClassName(Object.class.getName());
    assertThatIllegalArgumentException().isThrownBy(() -> AopConfigUtils.registerAspectJAutoProxyCreatorIfNecessary(registry))
            .withMessageContaining("not a known auto-proxy creator class");
    assertThat(definition.getBeanClassName()).isEqualTo(Object.class.getName());
  }

}
