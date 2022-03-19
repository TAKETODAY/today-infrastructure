/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.util.List;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Costin Leau
 */
@TestInstance(Lifecycle.PER_CLASS)
class ComponentBeanDefinitionParserTests {

  private final StandardBeanFactory bf = new StandardBeanFactory();

  @BeforeAll
  void setUp() throws Exception {
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            new ClassPathResource("component-config.xml", ComponentBeanDefinitionParserTests.class));
  }

  @AfterAll
  void tearDown() {
    bf.destroySingletons();
  }

  @Test
  void testBionicBasic() {
    Component cp = getBionicFamily();
    assertThat("Bionic-1").isEqualTo(cp.getName());
  }

  @Test
  void testBionicFirstLevelChildren() {
    Component cp = getBionicFamily();
    List<Component> components = cp.getComponents();
    assertThat(2).isEqualTo(components.size());
    assertThat("Mother-1").isEqualTo(components.get(0).getName());
    assertThat("Rock-1").isEqualTo(components.get(1).getName());
  }

  @Test
  void testBionicSecondLevelChildren() {
    Component cp = getBionicFamily();
    List<Component> components = cp.getComponents().get(0).getComponents();
    assertThat(2).isEqualTo(components.size());
    assertThat("Karate-1").isEqualTo(components.get(0).getName());
    assertThat("Sport-1").isEqualTo(components.get(1).getName());
  }

  private Component getBionicFamily() {
    return bf.getBean("bionic-family", Component.class);
  }

}

