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

package infra.http.service.registry;

import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;

import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.aot.AotServices;
import infra.beans.factory.aot.BeanRegistrationAotProcessor;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContextInitializer;
import infra.context.aot.ApplicationContextAotGenerator;
import infra.context.support.GenericApplicationContext;
import infra.core.test.tools.CompileWithForkedClassLoader;
import infra.core.test.tools.Compiled;
import infra.core.test.tools.TestCompiler;
import infra.http.service.registry.HttpServiceGroup.ClientType;
import infra.http.service.registry.echo.EchoA;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpServiceProxyBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 */
class HttpServiceProxyRegistrationAotProcessorTests {

  @Test
  void httpServiceProxyBeanRegistrationAotProcessorIsRegistered() {
    assertThat(AotServices.factories().load(BeanRegistrationAotProcessor.class))
            .anyMatch(HttpServiceProxyBeanRegistrationAotProcessor.class::isInstance);
  }

  @Test
  void getAotContributionWhenBeanHasNoGroup() {
    assertThat(hasContribution(new RootBeanDefinition(EchoA.class))).isFalse();
  }

  @Test
  void getAotContributionWhenBeanHasGroup() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(EchoA.class);
    beanDefinition.setAttribute(AbstractHttpServiceRegistrar.HTTP_SERVICE_GROUP_NAME_ATTRIBUTE, "echo");
    assertThat(hasContribution(beanDefinition)).isTrue();
  }

  private boolean hasContribution(RootBeanDefinition beanDefinition) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("test", beanDefinition);
    RegisteredBean registeredBean = RegisteredBean.of(beanFactory, "test");
    return new HttpServiceProxyBeanRegistrationAotProcessor().processAheadOfTime(registeredBean) != null;
  }

  @Test
  @CompileWithForkedClassLoader
  void processHttpServiceProxyWhenSingleClientType() {
    GroupsMetadata groupsMetadata = new GroupsMetadata();
    groupsMetadata.getOrCreateGroup("echo", ClientType.UNSPECIFIED)
            .httpServiceTypeNames().add(EchoA.class.getName());
    StandardBeanFactory beanFactory = prepareBeanFactory(groupsMetadata);
    RootBeanDefinition beanDefinition = new RootBeanDefinition(EchoA.class);
    beanDefinition.setAttribute(AbstractHttpServiceRegistrar.HTTP_SERVICE_GROUP_NAME_ATTRIBUTE, "echo");
    beanFactory.registerBeanDefinition("echoA", beanDefinition);
    compile(beanFactory, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
      HttpServiceProxyRegistry registry = freshApplicationContext.getBean(HttpServiceProxyRegistry.class);
      assertThat(registry.getClient("echo", EchoA.class)).isSameAs(freshApplicationContext.getBean(EchoA.class));
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void processHttpServiceProxyWhenSameClientTypeInDifferentGroups() {
    GroupsMetadata groupsMetadata = new GroupsMetadata();
    groupsMetadata.getOrCreateGroup("echo", ClientType.UNSPECIFIED)
            .httpServiceTypeNames().add(EchoA.class.getName());
    groupsMetadata.getOrCreateGroup("echo2", ClientType.UNSPECIFIED)
            .httpServiceTypeNames().add(EchoA.class.getName());
    StandardBeanFactory beanFactory = prepareBeanFactory(groupsMetadata);
    RootBeanDefinition beanDefinition = new RootBeanDefinition(EchoA.class);
    beanDefinition.setAttribute(AbstractHttpServiceRegistrar.HTTP_SERVICE_GROUP_NAME_ATTRIBUTE, "echo");
    beanFactory.registerBeanDefinition("echoA", beanDefinition);
    RootBeanDefinition beanDefinition2 = new RootBeanDefinition(EchoA.class);
    beanDefinition2.setAttribute(AbstractHttpServiceRegistrar.HTTP_SERVICE_GROUP_NAME_ATTRIBUTE, "echo2");
    beanFactory.registerBeanDefinition("echoA2", beanDefinition2);
    compile(beanFactory, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
      HttpServiceProxyRegistry registry = freshApplicationContext.getBean(HttpServiceProxyRegistry.class);
      assertThat(registry.getClient("echo", EchoA.class)).isSameAs(freshApplicationContext.getBean("echoA", EchoA.class));
      assertThat(registry.getClient("echo2", EchoA.class)).isSameAs(freshApplicationContext.getBean("echoA2", EchoA.class));
    });
  }

  private StandardBeanFactory prepareBeanFactory(GroupsMetadata metadata) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(HttpServiceProxyRegistryFactoryBean.class);
    beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, metadata);
    beanFactory.registerBeanDefinition(AbstractHttpServiceRegistrar.HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME, beanDefinition);
    return beanFactory;
  }

  private void compile(StandardBeanFactory beanFactory, BiConsumer<ApplicationContextInitializer, Compiled> result) {
    ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
    TestGenerationContext generationContext = new TestGenerationContext();
    generator.processAheadOfTime(new GenericApplicationContext(beanFactory), generationContext);
    generationContext.writeGeneratedContent();
    TestCompiler.forSystem().with(generationContext).compile(compiled ->
            result.accept(compiled.getInstance(ApplicationContextInitializer.class), compiled));
  }

  private GenericApplicationContext toFreshApplicationContext(ApplicationContextInitializer initializer) {
    GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
    initializer.initialize(freshApplicationContext);
    freshApplicationContext.refresh();
    return freshApplicationContext;
  }

}
