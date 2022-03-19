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

package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/19 18:35
 */
class PropertiesBeanDefinitionReaderTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private final PropertiesBeanDefinitionReader reader = new PropertiesBeanDefinitionReader(this.beanFactory);

  @Test
  void withSimpleConstructorArg() {
    this.reader.loadBeanDefinitions(new ClassPathResource("simpleConstructorArg.properties", getClass()));
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
    assertThat(bean.getName()).isEqualTo("Rob Harrop");
  }

  @Test
  void withConstructorArgRef() {
    this.reader.loadBeanDefinitions(new ClassPathResource("refConstructorArg.properties", getClass()));
    TestBean rob = (TestBean) this.beanFactory.getBean("rob");
    TestBean sally = (TestBean) this.beanFactory.getBean("sally");
    assertThat(rob.getSpouse()).isEqualTo(sally);
  }

  @Test
  void withMultipleConstructorsArgs() {
    this.reader.loadBeanDefinitions(new ClassPathResource("multiConstructorArgs.properties", getClass()));
    TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
    assertThat(bean.getName()).isEqualTo("Rob Harrop");
    assertThat(bean.getAge()).isEqualTo(23);
  }

}
