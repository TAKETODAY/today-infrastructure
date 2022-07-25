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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import cn.taketoday.beans.factory.config.PropertiesFactoryBean;
import cn.taketoday.core.io.Resource;

import static cn.taketoday.beans.testfixture.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/12 16:36
 */
class PropertiesFactoryBeanTests {

  private static final Class<?> CLASS = PropertiesFactoryBeanTests.class;
  private static final Resource TEST_PROPS = qualifiedResource(CLASS, "test.properties");
  private static final Resource TEST_PROPS_XML = qualifiedResource(CLASS, "test.properties.xml");

  @Test
  public void testWithPropertiesFile() throws Exception {
    PropertiesFactoryBean pfb = new PropertiesFactoryBean();
    pfb.setLocation(TEST_PROPS);
    pfb.afterPropertiesSet();
    Properties props = pfb.getObject();
    assertThat(props.getProperty("tb.array[0].age")).isEqualTo("99");
  }

  @Test
  public void testWithPropertiesXmlFile() throws Exception {
    PropertiesFactoryBean pfb = new PropertiesFactoryBean();
    pfb.setLocation(TEST_PROPS_XML);
    pfb.afterPropertiesSet();
    Properties props = pfb.getObject();
    assertThat(props.getProperty("tb.array[0].age")).isEqualTo("99");
  }

  @Test
  public void testWithLocalProperties() throws Exception {
    PropertiesFactoryBean pfb = new PropertiesFactoryBean();
    Properties localProps = new Properties();
    localProps.setProperty("key2", "value2");
    pfb.setProperties(localProps);
    pfb.afterPropertiesSet();
    Properties props = pfb.getObject();
    assertThat(props.getProperty("key2")).isEqualTo("value2");
  }

  @Test
  public void testWithPropertiesFileAndLocalProperties() throws Exception {
    PropertiesFactoryBean pfb = new PropertiesFactoryBean();
    pfb.setLocation(TEST_PROPS);
    Properties localProps = new Properties();
    localProps.setProperty("key2", "value2");
    localProps.setProperty("tb.array[0].age", "0");
    pfb.setProperties(localProps);
    pfb.afterPropertiesSet();
    Properties props = pfb.getObject();
    assertThat(props.getProperty("tb.array[0].age")).isEqualTo("99");
    assertThat(props.getProperty("key2")).isEqualTo("value2");
  }

  @Test
  public void testWithPropertiesFileAndMultipleLocalProperties() throws Exception {
    PropertiesFactoryBean pfb = new PropertiesFactoryBean();
    pfb.setLocation(TEST_PROPS);

    Properties props1 = new Properties();
    props1.setProperty("key2", "value2");
    props1.setProperty("tb.array[0].age", "0");

    Properties props2 = new Properties();
    props2.setProperty("spring", "framework");
    props2.setProperty("Don", "Mattingly");

    Properties props3 = new Properties();
    props3.setProperty("spider", "man");
    props3.setProperty("bat", "man");

    pfb.setPropertiesArray(props1, props2, props3);
    pfb.afterPropertiesSet();

    Properties props = pfb.getObject();
    assertThat(props.getProperty("tb.array[0].age")).isEqualTo("99");
    assertThat(props.getProperty("key2")).isEqualTo("value2");
    assertThat(props.getProperty("spring")).isEqualTo("framework");
    assertThat(props.getProperty("Don")).isEqualTo("Mattingly");
    assertThat(props.getProperty("spider")).isEqualTo("man");
    assertThat(props.getProperty("bat")).isEqualTo("man");
  }

  @Test
  public void testWithPropertiesFileAndLocalPropertiesAndLocalOverride() throws Exception {
    PropertiesFactoryBean pfb = new PropertiesFactoryBean();
    pfb.setLocation(TEST_PROPS);
    Properties localProps = new Properties();
    localProps.setProperty("key2", "value2");
    localProps.setProperty("tb.array[0].age", "0");
    pfb.setProperties(localProps);
    pfb.setLocalOverride(true);
    pfb.afterPropertiesSet();
    Properties props = pfb.getObject();
    assertThat(props.getProperty("tb.array[0].age")).isEqualTo("0");
    assertThat(props.getProperty("key2")).isEqualTo("value2");
  }

  @Test
  public void testWithPrototype() throws Exception {
    PropertiesFactoryBean pfb = new PropertiesFactoryBean();
    pfb.setSingleton(false);
    pfb.setLocation(TEST_PROPS);
    Properties localProps = new Properties();
    localProps.setProperty("key2", "value2");
    pfb.setProperties(localProps);
    pfb.afterPropertiesSet();
    Properties props = pfb.getObject();
    assertThat(props.getProperty("tb.array[0].age")).isEqualTo("99");
    assertThat(props.getProperty("key2")).isEqualTo("value2");
    Properties newProps = pfb.getObject();
    assertThat(props != newProps).isTrue();
    assertThat(newProps.getProperty("tb.array[0].age")).isEqualTo("99");
    assertThat(newProps.getProperty("key2")).isEqualTo("value2");
  }

}
