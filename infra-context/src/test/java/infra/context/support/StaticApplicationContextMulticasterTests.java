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

package infra.context.support;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import infra.beans.PropertyValues;
import infra.beans.factory.support.PropertiesBeanDefinitionReader;
import infra.beans.testfixture.beans.TestBean;
import infra.context.ApplicationEvent;
import infra.context.ConfigurableApplicationContext;
import infra.context.event.SimpleApplicationEventMulticaster;
import infra.context.testfixture.AbstractApplicationContextTests;
import infra.context.testfixture.beans.ACATester;
import infra.context.testfixture.beans.BeanThatListens;
import infra.core.ResolvableType;
import infra.core.io.ClassPathResource;
import infra.core.io.EncodedResource;
import infra.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for static application context with custom application event multicaster.
 *
 * @author Juergen Hoeller
 */
public class StaticApplicationContextMulticasterTests extends AbstractApplicationContextTests {

  protected StaticApplicationContext sac;

  @Override
  protected ConfigurableApplicationContext createContext() throws Exception {
    StaticApplicationContext parent = new StaticApplicationContext();
    Map<String, String> m = new HashMap<>();
    m.put("name", "Roderick");
    parent.registerPrototype("rod", TestBean.class, new PropertyValues(m));
    m.put("name", "Albert");
    parent.registerPrototype("father", TestBean.class, new PropertyValues(m));
    parent.registerSingleton(StaticApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
            TestApplicationEventMulticaster.class, null);
    parent.refresh();
    parent.addApplicationListener(parentListener);

    parent.getStaticMessageSource().addMessage("code1", Locale.getDefault(), "message1");

    this.sac = new StaticApplicationContext(parent);
    sac.registerSingleton("beanThatListens", BeanThatListens.class, new PropertyValues());
    sac.registerSingleton("aca", ACATester.class, new PropertyValues());
    sac.registerPrototype("aca-prototype", ACATester.class, new PropertyValues());
    PropertiesBeanDefinitionReader reader = new PropertiesBeanDefinitionReader(sac.getBeanFactory());
    Resource resource = new ClassPathResource("testBeans.properties", getClass());
    reader.loadBeanDefinitions(new EncodedResource(resource, "ISO-8859-1"));
    sac.refresh();
    sac.addApplicationListener(listener);

    sac.getStaticMessageSource().addMessage("code2", Locale.getDefault(), "message2");

    return sac;
  }

  @Test
  @Override
  public void count() {
    assertCount(15);
  }

  @Test
  @Override
  public void events() throws Exception {
    TestApplicationEventMulticaster.counter = 0;
    super.events();
    assertThat(TestApplicationEventMulticaster.counter).isEqualTo(1);
  }

  public static class TestApplicationEventMulticaster extends SimpleApplicationEventMulticaster {

    private static int counter = 0;

    @Override
    public void multicastEvent(ApplicationEvent event, @Nullable ResolvableType eventType) {
      super.multicastEvent(event, eventType);
      counter++;
    }
  }

}
