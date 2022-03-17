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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.BeanDefinitionReaderUtils;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class BeanNameGenerationTests {

  private StandardBeanFactory beanFactory;

  @BeforeEach
  public void setUp() {
    this.beanFactory = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    reader.loadBeanDefinitions(new ClassPathResource("beanNameGeneration.xml", getClass()));
  }

  @Test
  public void naming() {
    String className = GeneratedNameBean.class.getName();

    String targetName = className + BeanDefinitionReaderUtils.GENERATED_BEAN_NAME_SEPARATOR + "0";
    GeneratedNameBean topLevel1 = (GeneratedNameBean) beanFactory.getBean(targetName);
    assertThat(topLevel1).isNotNull();

    targetName = className + BeanDefinitionReaderUtils.GENERATED_BEAN_NAME_SEPARATOR + "1";
    GeneratedNameBean topLevel2 = (GeneratedNameBean) beanFactory.getBean(targetName);
    assertThat(topLevel2).isNotNull();

    GeneratedNameBean child1 = topLevel1.getChild();
    assertThat(child1.getBeanName()).isNotNull();
    assertThat(child1.getBeanName().startsWith(className)).isTrue();

    GeneratedNameBean child2 = topLevel2.getChild();
    assertThat(child2.getBeanName()).isNotNull();
    assertThat(child2.getBeanName().startsWith(className)).isTrue();

    assertThat(child1.getBeanName().equals(child2.getBeanName())).isFalse();
  }

}
