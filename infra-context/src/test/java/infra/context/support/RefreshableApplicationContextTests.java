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

package infra.context.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.beans.factory.DisposableBean;
import infra.beans.factory.support.BeanDefinitionOverrideException;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshableApplicationContextTests {

  @Test
  void replacesTheFactoryOnRefreshAndDestroysSingletonsFromThePreviousFactory() {
    TestContext context = new TestContext();
    try (context) {
      assertThatIllegalStateException().isThrownBy(() -> context.getBean("resource"))
              .withMessageContaining("call 'refresh'");

      context.refresh();
      var originalFactory = context.getBeanFactory();
      var originalResource = context.getBean("resource", TrackedResource.class);
      assertThat(originalResource.destroyed).isFalse();

      context.refresh();

      assertThat(context.getBeanFactory()).isNotSameAs(originalFactory);
      assertThat(originalResource.destroyed).isTrue();
      assertThat(context.getBean("resource")).isNotSameAs(originalResource);
    }
    assertThatIllegalStateException().isThrownBy(context::getBeanFactory)
            .withMessageContaining("already closed");
  }

  @Test
  void wrapsDefinitionReadFailuresAndAllowsARefreshAfterTheSourceIsFixed() {
    var failure = new IOException("definitions unavailable");
    try (var context = new TestContext()) {
      context.readFailure = failure;

      assertThatThrownBy(context::refresh).isInstanceOf(ApplicationContextException.class)
              .hasMessageContaining("I/O error parsing bean definition source").hasCause(failure);
      assertThatIllegalStateException().isThrownBy(context::getBeanFactory);

      context.readFailure = null;
      context.refresh();
      assertThat(context.getBean("resource")).isInstanceOf(TrackedResource.class);
    }
  }

  @Test
  void exposesBeanDefinitionsAndAliasesThroughTheCurrentRegistry() {
    try (var parent = new TestContext(); var child = new TestContext(parent)) {
      parent.refresh();
      parent.getBeanFactory().registerSingleton("parentValue", "inherited");
      child.setAllowBeanDefinitionOverriding(false);
      child.setAllowCircularReferences(false);
      child.refresh();

      assertThat(child.getBean("parentValue")).isEqualTo("inherited");
      assertThat(child.isAllowBeanDefinitionOverriding()).isFalse();
      assertThat(child.isBeanDefinitionOverridable("resource")).isFalse();
      assertThat(child.getBeanFactory().isAllowCircularReferences()).isFalse();

      int originalCount = child.getBeanDefinitionCount();
      var definition = new RootBeanDefinition(StringBuilder.class);
      child.registerBeanDefinition("builder", definition);
      child.registerAlias("builder", "builderAlias");

      assertThat(child.getBeanDefinitionCount()).isEqualTo(originalCount + 1);
      assertThat(child.getBeanDefinitionNames()).contains("builder");
      assertThat(child.getBeanDefinition("builder")).isSameAs(definition);
      assertThat(child.getBeanDefinition(StringBuilder.class)).isSameAs(definition);
      assertThat(child.containsBeanDefinition("builder")).isTrue();
      assertThat(child.containsBeanDefinition("builder", StringBuilder.class)).isTrue();
      assertThat(child.containsBeanDefinition(StringBuilder.class)).isTrue();
      assertThat(child.containsBeanDefinition(StringBuilder.class, true)).isTrue();
      assertThat(child.isAlias("builderAlias")).isTrue();
      assertThat(child.isBeanNameInUse("builderAlias")).isTrue();
      assertThat(child.getAliasList("builder")).containsExactly("builderAlias");
      assertThat(child.getBean("builderAlias")).isSameAs(child.getBean("builder"));
      assertThatThrownBy(() -> child.registerBeanDefinition("builder", new RootBeanDefinition(StringBuilder.class)))
              .isInstanceOf(BeanDefinitionOverrideException.class);

      child.removeAlias("builderAlias");
      child.removeBeanDefinition("builder");
      assertThat(child.isAlias("builderAlias")).isFalse();
      assertThat(child.containsBeanDefinition("builder")).isFalse();
      assertThat(child.getBeanDefinitionCount()).isEqualTo(originalCount);
    }
  }

  private static class TestContext extends AbstractRefreshableApplicationContext {

    private IOException readFailure;

    TestContext() { }

    TestContext(ApplicationContext parent) {
      super(parent);
    }

    @Override
    protected void loadBeanDefinitions(StandardBeanFactory beanFactory) throws IOException {
      if (readFailure != null) {
        throw readFailure;
      }
      beanFactory.registerBeanDefinition("resource", new RootBeanDefinition(TrackedResource.class));
    }
  }

  private static class TrackedResource implements DisposableBean {

    private boolean destroyed;

    @Override
    public void destroy() {
      destroyed = true;
    }
  }

}
