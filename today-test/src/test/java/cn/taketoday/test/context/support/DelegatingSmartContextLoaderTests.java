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

package cn.taketoday.test.context.support;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DelegatingSmartContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class DelegatingSmartContextLoaderTests {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];
  private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

  private final DelegatingSmartContextLoader loader = new DelegatingSmartContextLoader();

  @Nested
  class SmartContextLoaderSpiTests {

    @Test
    void processContextConfigurationWithDefaultXmlConfigGeneration() {
      ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
              XmlTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
      loader.processContextConfiguration(configAttributes);
      assertThat(configAttributes.getLocations()).hasSize(1);
      assertThat(configAttributes.getClasses()).isEmpty();
    }

    @Test
    void processContextConfigurationWithDefaultConfigurationClassGeneration() {
      ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
              ConfigClassTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
      loader.processContextConfiguration(configAttributes);
      assertThat(configAttributes.getClasses()).hasSize(1);
      assertThat(configAttributes.getLocations()).isEmpty();
    }

    @Test
    void processContextConfigurationWithDefaultXmlConfigAndConfigurationClassGeneration() {
      ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
              ImproperDuplicateDefaultXmlAndConfigClassTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY,
              true, null, true, ContextLoader.class);
      assertThatIllegalStateException()
              .isThrownBy(() -> loader.processContextConfiguration(configAttributes))
              .withMessageContaining("both default locations AND default configuration classes were detected");
    }

    @Test
    void processContextConfigurationWithLocation() {
      String[] locations = new String[] { "classpath:/foo.xml" };
      ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
              getClass(), locations, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
      loader.processContextConfiguration(configAttributes);
      assertThat(configAttributes.getLocations()).isEqualTo(locations);
      assertThat(configAttributes.getClasses()).isEmpty();
    }

    @Test
    void processContextConfigurationWithConfigurationClass() {
      Class<?>[] classes = new Class<?>[] { getClass() };
      ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
              getClass(), EMPTY_STRING_ARRAY, classes, true, null, true, ContextLoader.class);
      loader.processContextConfiguration(configAttributes);
      assertThat(configAttributes.getClasses()).isEqualTo(classes);
      assertThat(configAttributes.getLocations()).isEmpty();
    }

    @Test
    void loadContextWithNullConfig() throws Exception {
      assertThatIllegalArgumentException().isThrownBy(() -> loader.loadContext((MergedContextConfiguration) null));
    }

    @Test
    void loadContextWithoutLocationsAndConfigurationClasses() throws Exception {
      MergedContextConfiguration mergedConfig = new MergedContextConfiguration(
              getClass(), EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
      assertThatIllegalStateException()
              .isThrownBy(() -> loader.loadContext(mergedConfig))
              .withMessageStartingWith("Neither")
              .withMessageContaining("is able to load an ApplicationContext for");
    }

    /**
     * @since 4.1
     */
    @Test
    void loadContextWithLocationsAndConfigurationClasses() throws Exception {
      MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(),
              new String[] { "test.xml" }, new Class<?>[] { getClass() }, EMPTY_STRING_ARRAY, loader);
      assertThatIllegalStateException()
              .isThrownBy(() -> loader.loadContext(mergedConfig))
              .withMessageStartingWith("Neither")
              .withMessageContaining("declare either 'locations' or 'classes' but not both.");
    }

    @Test
    void loadContextWithXmlConfig() throws Exception {
      MergedContextConfiguration mergedConfig = new MergedContextConfiguration(
              XmlTestCase.class,
              new String[] { "classpath:/cn/taketoday/test/context/support/DelegatingSmartContextLoaderTests$XmlTestCase-context.xml" },
              EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);

      assertApplicationContextLoadsAndContainsFooString(mergedConfig);
    }

    @Test
    void loadContextWithConfigurationClass() throws Exception {
      MergedContextConfiguration mergedConfig = new MergedContextConfiguration(ConfigClassTestCase.class,
              EMPTY_STRING_ARRAY, new Class<?>[] { ConfigClassTestCase.Config.class }, EMPTY_STRING_ARRAY, loader);

      assertApplicationContextLoadsAndContainsFooString(mergedConfig);
    }

    private void assertApplicationContextLoadsAndContainsFooString(MergedContextConfiguration mergedConfig)
            throws Exception {

      ApplicationContext applicationContext = loader.loadContext(mergedConfig);
      assertThat(applicationContext).isInstanceOf(ConfigurableApplicationContext.class);
      assertThat(applicationContext.getBean(String.class)).isEqualTo("foo");
      ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
      cac.close();
    }

    /**
     * @since 6.0
     */
    @Test
    void loadContextWithXmlConfigWithoutRefresh() throws Exception {
      MergedContextConfiguration mergedConfig = new MergedContextConfiguration(
              XmlTestCase.class,
              new String[] { "classpath:/cn/taketoday/test/context/support/DelegatingSmartContextLoaderTests$XmlTestCase-context.xml" },
              EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);

      assertApplicationContextLoadsForAotProcessing(mergedConfig, "foo");
    }

    /**
     * @since 6.0
     */
    @Test
    void loadContextWithConfigurationClassWithoutRefresh() throws Exception {
      MergedContextConfiguration mergedConfig = new MergedContextConfiguration(ConfigClassTestCase.class,
              EMPTY_STRING_ARRAY, new Class<?>[] { ConfigClassTestCase.Config.class }, EMPTY_STRING_ARRAY, loader);

      assertApplicationContextLoadsForAotProcessing(mergedConfig, "ConfigClassTestCase.Config");
    }

    private void assertApplicationContextLoadsForAotProcessing(MergedContextConfiguration mergedConfig,
            String expectedBeanDefName) throws Exception {

      ApplicationContext context = loader.loadContextForAotProcessing(mergedConfig);
      assertThat(context).isInstanceOf(ConfigurableApplicationContext.class);
      ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
      assertThat(cac.isActive()).as("ApplicationContext is active").isFalse();
      assertThat(Arrays.stream(context.getBeanDefinitionNames())).anyMatch(name -> name.contains(expectedBeanDefName));
      cac.close();
    }

  }

  @Nested
  class ContextLoaderSpiTests {

    @Test
    void processLocations() {
      assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> loader.processLocations(getClass(), EMPTY_STRING_ARRAY));
    }

    @Test
    void loadContextFromLocations() {
      assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> loader.loadContext(EMPTY_STRING_ARRAY));
    }

  }

  static class XmlTestCase {
  }

  static class ConfigClassTestCase {

    @Configuration(proxyBeanMethods = false)
    static class Config {

      @Bean
      public String foo() {
        return "foo";
      }
    }

    static class NotAConfigClass {
    }
  }

  static class ImproperDuplicateDefaultXmlAndConfigClassTestCase {

    @Configuration
    static class Config {
      // intentionally empty: we just need the class to be present to fail
      // the test
    }
  }

}
