/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.support;

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
import infra.lang.Nullable;

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
