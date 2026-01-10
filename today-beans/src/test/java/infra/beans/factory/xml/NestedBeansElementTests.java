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

package infra.beans.factory.xml;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.StandardEnvironment;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;

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
