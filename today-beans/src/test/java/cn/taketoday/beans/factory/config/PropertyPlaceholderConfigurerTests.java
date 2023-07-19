/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static cn.taketoday.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static cn.taketoday.beans.factory.support.BeanDefinitionReaderUtils.registerWithGeneratedName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/19 20:32
 */
@Execution(ExecutionMode.SAME_THREAD)
class PropertyPlaceholderConfigurerTests {

  private static final String P1 = "p1";
  private static final String P1_LOCAL_PROPS_VAL = "p1LocalPropsVal";
  private static final String P1_SYSTEM_PROPS_VAL = "p1SystemPropsVal";

  private final StandardBeanFactory bf = new StandardBeanFactory();

  private final PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();

  private final Properties ppcProperties = new Properties();

  private AbstractBeanDefinition p1BeanDef = rootBeanDefinition(TestBean.class)
          .addPropertyValue("name", "${" + P1 + "}")
          .getBeanDefinition();

  @BeforeEach
  void setup() {
    ppcProperties.setProperty(P1, P1_LOCAL_PROPS_VAL);
    System.setProperty(P1, P1_SYSTEM_PROPS_VAL);
    ppc.setProperties(ppcProperties);
  }

  @AfterEach
  void cleanup() {
    System.clearProperty(P1);
    System.clearProperty(P1_SYSTEM_PROPS_VAL);
  }

  @Test
  void localPropertiesViaResource() {
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "${my.name}")
                    .getBeanDefinition());

    PropertyPlaceholderConfigurer pc = new PropertyPlaceholderConfigurer();
    Resource resource = new ClassPathResource("PropertyPlaceholderConfigurerTests.properties", this.getClass());
    pc.setLocation(resource);
    pc.postProcessBeanFactory(bf);
  }

  @Test
  void resolveFromSystemProperties() {
    System.setProperty("otherKey", "systemValue");
    p1BeanDef = rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${" + P1 + "}")
            .addPropertyValue("sex", "${otherKey}")
            .getBeanDefinition();
    registerWithGeneratedName(p1BeanDef, bf);
    ppc.postProcessBeanFactory(bf);
    TestBean bean = bf.getBean(TestBean.class);
    assertThat(bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL);
    assertThat(bean.getSex()).isEqualTo("systemValue");
    System.clearProperty("otherKey");
  }

  @Test
  void resolveFromLocalProperties() {
    System.clearProperty(P1);
    registerWithGeneratedName(p1BeanDef, bf);
    ppc.postProcessBeanFactory(bf);
    TestBean bean = bf.getBean(TestBean.class);
    assertThat(bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL);
  }

  @Test
  void setSystemPropertiesMode_defaultIsFallback() {
    registerWithGeneratedName(p1BeanDef, bf);
    ppc.postProcessBeanFactory(bf);
    TestBean bean = bf.getBean(TestBean.class);
    assertThat(bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL);
  }

  @Test
  void setSystemSystemPropertiesMode_toOverride_andResolveFromSystemProperties() {
    setup();
    registerWithGeneratedName(p1BeanDef, bf);
    ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
    ppc.postProcessBeanFactory(bf);
    TestBean bean = bf.getBean(TestBean.class);
    assertThat(bean.getName()).isEqualTo(P1_SYSTEM_PROPS_VAL);
  }

  @Test
  void setSystemSystemPropertiesMode_toOverride_andSetSearchSystemEnvironment_toFalse() {
    registerWithGeneratedName(p1BeanDef, bf);
    System.clearProperty(P1); // will now fall all the way back to system environment
    ppc.setSearchSystemEnvironment(false);
    ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
    ppc.postProcessBeanFactory(bf);
    TestBean bean = bf.getBean(TestBean.class);
    assertThat(bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL); // has to resort to local props
  }

  /**
   * Creates a scenario in which two PPCs are configured, each with different
   * settings regarding resolving properties from the environment.
   */
  @Test
  void twoPlaceholderConfigurers_withConflictingSettings() {
    String P2 = "p2";
    String P2_LOCAL_PROPS_VAL = "p2LocalPropsVal";
    String P2_SYSTEM_PROPS_VAL = "p2SystemPropsVal";

    AbstractBeanDefinition p2BeanDef = rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${" + P1 + "}")
            .addPropertyValue("country", "${" + P2 + "}")
            .getBeanDefinition();

    bf.registerBeanDefinition("p1Bean", p1BeanDef);
    bf.registerBeanDefinition("p2Bean", p2BeanDef);

    ppc.setIgnoreUnresolvablePlaceholders(true);
    ppc.postProcessBeanFactory(bf);

    System.setProperty(P2, P2_SYSTEM_PROPS_VAL);
    Properties ppc2Properties = new Properties();
    ppc2Properties.put(P2, P2_LOCAL_PROPS_VAL);

    PropertyPlaceholderConfigurer ppc2 = new PropertyPlaceholderConfigurer();
    ppc2.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
    ppc2.setProperties(ppc2Properties);

    ppc2Properties = new Properties();
    ppc2Properties.setProperty(P2, P2_LOCAL_PROPS_VAL);
    ppc2.postProcessBeanFactory(bf);

    TestBean p1Bean = bf.getBean("p1Bean", TestBean.class);
    assertThat(p1Bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL);

    TestBean p2Bean = bf.getBean("p2Bean", TestBean.class);
    assertThat(p2Bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL);
    assertThat(p2Bean.getCountry()).isEqualTo(P2_SYSTEM_PROPS_VAL);

    System.clearProperty(P2);
  }

  @Test
  void customPlaceholderPrefixAndSuffix() {
    ppc.setPlaceholderPrefix("@<");
    ppc.setPlaceholderSuffix(">");

    bf.registerBeanDefinition("testBean",
            rootBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "@<key1>")
                    .addPropertyValue("sex", "${key2}")
                    .getBeanDefinition());

    System.setProperty("key1", "systemKey1Value");
    System.setProperty("key2", "systemKey2Value");
    ppc.postProcessBeanFactory(bf);
    System.clearProperty("key1");
    System.clearProperty("key2");

    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("systemKey1Value");
    assertThat(bf.getBean(TestBean.class).getSex()).isEqualTo("${key2}");
  }

  @Test
  void nullValueIsPreserved() {
    ppc.setNullValue("customNull");
    System.setProperty("my.name", "customNull");
    bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${my.name}")
            .getBeanDefinition());
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isNull();
    System.clearProperty("my.name");
  }

  @Test
  void trimValuesIsOffByDefault() {
    System.setProperty("my.name", " myValue  ");
    bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${my.name}")
            .getBeanDefinition());
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo(" myValue  ");
    System.clearProperty("my.name");
  }

  @Test
  void trimValuesIsApplied() {
    ppc.setTrimValues(true);
    System.setProperty("my.name", " myValue  ");
    bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
            .addPropertyValue("name", "${my.name}")
            .getBeanDefinition());
    ppc.postProcessBeanFactory(bf);
    assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("myValue");
    System.clearProperty("my.name");
  }

  /**
   * This test effectively verifies that the internal 'constants' map is properly
   * configured for all SYSTEM_PROPERTIES_MODE_ constants defined in
   * {@link PropertyPlaceholderConfigurer}.
   */
  @Test
  void setSystemPropertiesModeNameToAllSupportedValues() {
    streamSystemPropertiesModeConstants()
            .map(Field::getName)
            .forEach(name -> assertThatNoException().as(name).isThrownBy(() -> ppc.setSystemPropertiesModeName(name)));
  }

  private static Stream<Field> streamSystemPropertiesModeConstants() {
    return Arrays.stream(PropertyPlaceholderConfigurer.class.getFields())
            .filter(ReflectionUtils::isPublicStaticFinal)
            .filter(field -> field.getName().startsWith("SYSTEM_PROPERTIES_MODE_"));
  }

}