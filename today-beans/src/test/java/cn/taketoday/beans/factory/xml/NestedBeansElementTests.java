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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for new nested beans element support in Infra XML
 *
 * @author Chris Beams
 */
public class NestedBeansElementTests {
  private final Resource XML =
          new ClassPathResource("NestedBeansElementTests-context.xml", this.getClass());

  @Test
  public void getBean_withoutActiveProfile() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(XML);

    Object foo = bf.getBean("foo");
    assertThat(foo).isInstanceOf(String.class);
  }

  @Test
  public void getBean_withActiveProfile() {
    ConfigurableEnvironment env = new StandardEnvironment();
    env.setActiveProfiles("dev");

    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setAllowBeanDefinitionOverriding(true);

    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
    reader.setEnvironment(env);
    reader.loadBeanDefinitions(XML);

    bf.getBean("devOnlyBean"); // should not throw NSBDE

    Object foo = bf.getBean("foo");
    assertThat(foo).isInstanceOf(Integer.class);

    bf.getBean("devOnlyBean");
  }

}
