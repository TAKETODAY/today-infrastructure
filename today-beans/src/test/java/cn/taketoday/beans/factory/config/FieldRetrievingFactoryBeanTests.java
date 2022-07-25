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

package cn.taketoday.beans.factory.config;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static cn.taketoday.beans.testfixture.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/6 17:31
 */
class FieldRetrievingFactoryBeanTests {

  @Test
  public void testStaticField() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setStaticField("java.sql.Connection.TRANSACTION_SERIALIZABLE");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
  }

  @Test
  public void testStaticFieldWithWhitespace() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setStaticField("  java.sql.Connection.TRANSACTION_SERIALIZABLE  ");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
  }

  @Test
  public void testStaticFieldViaClassAndFieldName() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setTargetClass(Connection.class);
    fr.setTargetField("TRANSACTION_SERIALIZABLE");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
  }

  @Test
  public void testNonStaticField() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    PublicFieldHolder target = new PublicFieldHolder();
    fr.setTargetObject(target);
    fr.setTargetField("publicField");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo(target.publicField);
  }

  @Test
  public void testNothingButBeanName() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setBeanName("java.sql.Connection.TRANSACTION_SERIALIZABLE");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
  }

  @Test
  public void testJustTargetField() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setTargetField("TRANSACTION_SERIALIZABLE");
    try {
      fr.afterPropertiesSet();
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testJustTargetClass() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setTargetClass(Connection.class);
    try {
      fr.afterPropertiesSet();
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testJustTargetObject() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setTargetObject(new PublicFieldHolder());
    try {
      fr.afterPropertiesSet();
    }
    catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void testWithConstantOnClassWithPackageLevelVisibility() throws Exception {
    FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
    fr.setBeanName("cn.taketoday.beans.testfixture.beans.PackageLevelVisibleBean.CONSTANT");
    fr.afterPropertiesSet();
    assertThat(fr.getObject()).isEqualTo("Wuby");
  }

  @Test
  public void testBeanNameSyntaxWithBeanFactory() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            qualifiedResource(FieldRetrievingFactoryBeanTests.class, "context.xml"));

    TestBean testBean = (TestBean) bf.getBean("testBean");
    assertThat(testBean.getSomeIntegerArray()[0]).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
    assertThat(testBean.getSomeIntegerArray()[1]).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
  }

  private static class PublicFieldHolder {

    public String publicField = "test";
  }

}
