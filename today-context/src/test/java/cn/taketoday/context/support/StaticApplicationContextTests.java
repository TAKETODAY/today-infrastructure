/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.support;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.testfixture.AbstractApplicationContextTests;
import cn.taketoday.context.testfixture.beans.ACATester;
import cn.taketoday.context.testfixture.beans.BeanThatListens;
import cn.taketoday.core.io.ClassPathResource;

/**
 * Tests for static application context.
 *
 * @author Rod Johnson
 */
public class StaticApplicationContextTests extends AbstractApplicationContextTests {

  protected StaticApplicationContext sac;

  @Override
  protected ConfigurableApplicationContext createContext() throws Exception {
    StaticApplicationContext parent = new StaticApplicationContext();
    Map<String, String> m = new HashMap<>();
    m.put("name", "Roderick");
    parent.registerPrototype("rod", TestBean.class, new PropertyValues(m));
    m.put("name", "Albert");
    parent.registerPrototype("father", TestBean.class, new PropertyValues(m));
    parent.refresh();
    parent.addApplicationListener(parentListener);

    parent.getStaticMessageSource().addMessage("code1", Locale.getDefault(), "message1");

    this.sac = new StaticApplicationContext(parent);
    sac.registerSingleton("beanThatListens", BeanThatListens.class, new PropertyValues());
    sac.registerSingleton("aca", ACATester.class, new PropertyValues());
    sac.registerPrototype("aca-prototype", ACATester.class, new PropertyValues());
    cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader reader =
            new cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader(sac.getBeanFactory());
    reader.loadBeanDefinitions(new ClassPathResource("testBeans.properties", getClass()));
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

}
