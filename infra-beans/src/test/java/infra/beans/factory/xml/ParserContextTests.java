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

package infra.beans.factory.xml;

import org.junit.jupiter.api.Test;

import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.parsing.BeanComponentDefinition;
import infra.beans.factory.parsing.CompositeComponentDefinition;
import infra.beans.factory.parsing.FailFastProblemReporter;
import infra.beans.factory.parsing.ReaderEventListener;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.SimpleBeanDefinitionRegistry;
import infra.core.io.DescriptiveResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class ParserContextTests {

  private final SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

  private final ReaderEventListener events = mock(ReaderEventListener.class);

  private final XmlReaderContext readerContext = new XmlReaderContext(new DescriptiveResource("test.xml"),
          new FailFastProblemReporter(), events, (candidate, resource) -> candidate,
          new XmlBeanDefinitionReader(registry), namespace -> null);

  private final BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);

  @Test
  void nestedContextExposesContainingDefinitionAndParsingDefaults() {
    var containingBean = new RootBeanDefinition(String.class);
    var context = new ParserContext(readerContext, delegate, containingBean);
    var source = new Object();

    assertThat(context.isNested()).isTrue();
    assertThat(context.getContainingBeanDefinition()).isSameAs(containingBean);
    assertThat(context.getRegistry()).isSameAs(registry);
    assertThat(context.extractSource(source)).isSameAs(source);
    assertThat(context.isDefaultLazyInit()).isFalse();

    delegate.getDefaults().setLazyInit("true");

    assertThat(context.isDefaultLazyInit()).isTrue();
  }

  @Test
  void aggregatesNestedComponentsAndPublishesOnlyTheOuterComponent() {
    var context = new ParserContext(readerContext, delegate);
    var outer = new CompositeComponentDefinition("outer", null);
    var inner = new CompositeComponentDefinition("inner", null);
    var definition = new RootBeanDefinition(String.class);
    var bean = new BeanComponentDefinition(new BeanDefinitionHolder(definition, "bean", new String[] { "alias" }));

    assertThat(context.isNested()).isFalse();
    assertThat(context.getContainingComponent()).isNull();
    context.pushContainingComponent(outer);
    context.pushContainingComponent(inner);
    context.registerBeanComponent(bean);
    context.popAndRegisterContainingComponent();

    assertThat(context.getContainingComponent()).isSameAs(outer);
    assertThat(inner.getNestedComponents()).containsExactly(bean);
    assertThat(outer.getNestedComponents()).containsExactly(inner);
    assertThat(registry.getBeanDefinition("bean")).isSameAs(definition);
    assertThat(registry.getAliases("bean")).containsExactly("alias");
    then(events).shouldHaveNoInteractions();

    context.popAndRegisterContainingComponent();

    assertThat(context.getContainingComponent()).isNull();
    then(events).should().componentRegistered(outer);
    then(events).shouldHaveNoMoreInteractions();
  }

}
