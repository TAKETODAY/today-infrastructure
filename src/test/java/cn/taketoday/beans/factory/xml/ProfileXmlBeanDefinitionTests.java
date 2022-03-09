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

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests various combinations of profile declarations against various profile
 * activation and profile default scenarios.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 4.0
 */
public class ProfileXmlBeanDefinitionTests {

  private static final String PROD_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-prodProfile.xml";
  private static final String DEV_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-devProfile.xml";
  private static final String NOT_DEV_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-notDevProfile.xml";
  private static final String ALL_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-noProfile.xml";
  private static final String MULTI_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-multiProfile.xml";
  private static final String MULTI_NEGATED_XML = "ProfileXmlBeanDefinitionTests-multiProfileNegated.xml";
  private static final String MULTI_NOT_DEV_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-multiProfileNotDev.xml";
  private static final String MULTI_ELIGIBLE_SPACE_DELIMITED_XML = "ProfileXmlBeanDefinitionTests-spaceDelimitedProfile.xml";
  private static final String UNKNOWN_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-unknownProfile.xml";
  private static final String DEFAULT_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-defaultProfile.xml";
  private static final String CUSTOM_DEFAULT_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-customDefaultProfile.xml";
  private static final String DEFAULT_AND_DEV_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-defaultAndDevProfile.xml";

  private static final String PROD_ACTIVE = "prod";
  private static final String DEV_ACTIVE = "dev";
  private static final String NULL_ACTIVE = null;
  private static final String UNKNOWN_ACTIVE = "unknown";
  private static final String[] NONE_ACTIVE = new String[0];
  private static final String[] MULTI_ACTIVE = new String[] { PROD_ACTIVE, DEV_ACTIVE };

  private static final String TARGET_BEAN = "foo";

  @Test
  public void testProfileValidation() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            beanFactoryFor(PROD_ELIGIBLE_XML, NULL_ACTIVE));
  }

  @Test
  public void testProfilePermutations() {
    assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, NONE_ACTIVE)).isNot(containingTarget());
    assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, DEV_ACTIVE)).isNot(containingTarget());
    assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, PROD_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, MULTI_ACTIVE)).is(containingTarget());

    assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, NONE_ACTIVE)).isNot(containingTarget());
    assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, DEV_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, PROD_ACTIVE)).isNot(containingTarget());
    assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, MULTI_ACTIVE)).is(containingTarget());

    assertThat(beanFactoryFor(NOT_DEV_ELIGIBLE_XML, NONE_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(NOT_DEV_ELIGIBLE_XML, DEV_ACTIVE)).isNot(containingTarget());
    assertThat(beanFactoryFor(NOT_DEV_ELIGIBLE_XML, PROD_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(NOT_DEV_ELIGIBLE_XML, MULTI_ACTIVE)).isNot(containingTarget());

    assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, NONE_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, DEV_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, PROD_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, MULTI_ACTIVE)).is(containingTarget());

    assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, NONE_ACTIVE)).isNot(containingTarget());
    assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, UNKNOWN_ACTIVE)).isNot(containingTarget());
    assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, DEV_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, PROD_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, MULTI_ACTIVE)).is(containingTarget());

    assertThat(beanFactoryFor(MULTI_NEGATED_XML, NONE_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_NEGATED_XML, UNKNOWN_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_NEGATED_XML, DEV_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_NEGATED_XML, PROD_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_NEGATED_XML, MULTI_ACTIVE)).isNot(containingTarget());

    assertThat(beanFactoryFor(MULTI_NOT_DEV_ELIGIBLE_XML, NONE_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_NOT_DEV_ELIGIBLE_XML, UNKNOWN_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_NOT_DEV_ELIGIBLE_XML, DEV_ACTIVE)).isNot(containingTarget());
    assertThat(beanFactoryFor(MULTI_NOT_DEV_ELIGIBLE_XML, PROD_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_NOT_DEV_ELIGIBLE_XML, MULTI_ACTIVE)).is(containingTarget());

    assertThat(beanFactoryFor(MULTI_ELIGIBLE_SPACE_DELIMITED_XML, NONE_ACTIVE)).isNot(containingTarget());
    assertThat(beanFactoryFor(MULTI_ELIGIBLE_SPACE_DELIMITED_XML, UNKNOWN_ACTIVE)).isNot(containingTarget());
    assertThat(beanFactoryFor(MULTI_ELIGIBLE_SPACE_DELIMITED_XML, DEV_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_ELIGIBLE_SPACE_DELIMITED_XML, PROD_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(MULTI_ELIGIBLE_SPACE_DELIMITED_XML, MULTI_ACTIVE)).is(containingTarget());

    assertThat(beanFactoryFor(UNKNOWN_ELIGIBLE_XML, MULTI_ACTIVE)).isNot(containingTarget());
  }

  @Test
  public void testDefaultProfile() {
    {
      StandardBeanFactory beanFactory = new StandardBeanFactory();
      XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
      ConfigurableEnvironment env = new StandardEnvironment();
      env.setDefaultProfiles("custom-default");
      reader.setEnvironment(env);
      reader.loadBeanDefinitions(new ClassPathResource(DEFAULT_ELIGIBLE_XML, getClass()));

      assertThat(beanFactory).isNot(containingTarget());
    }
    {
      StandardBeanFactory beanFactory = new StandardBeanFactory();
      XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
      ConfigurableEnvironment env = new StandardEnvironment();
      env.setDefaultProfiles("custom-default");
      reader.setEnvironment(env);
      reader.loadBeanDefinitions(new ClassPathResource(CUSTOM_DEFAULT_ELIGIBLE_XML, getClass()));

      assertThat(beanFactory).is(containingTarget());
    }
  }

  @Test
  public void testDefaultAndNonDefaultProfile() {
    assertThat(beanFactoryFor(DEFAULT_ELIGIBLE_XML, NONE_ACTIVE)).is(containingTarget());
    assertThat(beanFactoryFor(DEFAULT_ELIGIBLE_XML, "other")).isNot(containingTarget());

    {
      StandardBeanFactory beanFactory = new StandardBeanFactory();
      XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
      ConfigurableEnvironment env = new StandardEnvironment();
      env.setActiveProfiles(DEV_ACTIVE);
      env.setDefaultProfiles("default");
      reader.setEnvironment(env);
      reader.loadBeanDefinitions(new ClassPathResource(DEFAULT_AND_DEV_ELIGIBLE_XML, getClass()));
      assertThat(beanFactory).is(containingTarget());
    }
    {
      StandardBeanFactory beanFactory = new StandardBeanFactory();
      XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
      ConfigurableEnvironment env = new StandardEnvironment();
      // env.setActiveProfiles(DEV_ACTIVE);
      env.setDefaultProfiles("default");
      reader.setEnvironment(env);
      reader.loadBeanDefinitions(new ClassPathResource(DEFAULT_AND_DEV_ELIGIBLE_XML, getClass()));
      assertThat(beanFactory).is(containingTarget());
    }
    {
      StandardBeanFactory beanFactory = new StandardBeanFactory();
      XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
      ConfigurableEnvironment env = new StandardEnvironment();
      // env.setActiveProfiles(DEV_ACTIVE);
      //env.setDefaultProfiles("default");
      reader.setEnvironment(env);
      reader.loadBeanDefinitions(new ClassPathResource(DEFAULT_AND_DEV_ELIGIBLE_XML, getClass()));
      assertThat(beanFactory).is(containingTarget());
    }
  }

  private BeanDefinitionRegistry beanFactoryFor(String xmlName, String... activeProfiles) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
    StandardEnvironment env = new StandardEnvironment();
    env.setActiveProfiles(activeProfiles);
    reader.setEnvironment(env);
    reader.loadBeanDefinitions(new ClassPathResource(xmlName, getClass()));
    return beanFactory;
  }

  private Condition<BeanDefinitionRegistry> containingTarget() {
    return new Condition<>(registry -> registry.containsBeanDefinition(TARGET_BEAN), "contains target");
  }

}
