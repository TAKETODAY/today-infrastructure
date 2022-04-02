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

package cn.taketoday.test.context.support;

import org.junit.jupiter.api.Test;

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

  private static void assertEmpty(Object[] array) {
    assertThat(ObjectUtils.isEmpty(array)).isTrue();
  }

  // --- SmartContextLoader - processContextConfiguration() ------------------

  @Test
  void processContextConfigurationWithDefaultXmlConfigGeneration() {
    ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
            XmlTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
    loader.processContextConfiguration(configAttributes);
    assertThat(configAttributes.getLocations().length).isEqualTo(1);
    assertEmpty(configAttributes.getClasses());
  }

  @Test
  void processContextConfigurationWithDefaultConfigurationClassGeneration() {
    ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
            ConfigClassTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
    loader.processContextConfiguration(configAttributes);
    assertThat(configAttributes.getClasses().length).isEqualTo(1);
    assertEmpty(configAttributes.getLocations());
  }

  @Test
  void processContextConfigurationWithDefaultXmlConfigAndConfigurationClassGeneration() {
    ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
            ImproperDuplicateDefaultXmlAndConfigClassTestCase.class, EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY,
            true, null, true, ContextLoader.class);
    assertThatIllegalStateException().isThrownBy(() ->
                    loader.processContextConfiguration(configAttributes))
            .withMessageContaining("both default locations AND default configuration classes were detected");
  }

  @Test
  void processContextConfigurationWithLocation() {
    String[] locations = new String[] { "classpath:/foo.xml" };
    ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
            getClass(), locations, EMPTY_CLASS_ARRAY, true, null, true, ContextLoader.class);
    loader.processContextConfiguration(configAttributes);
    assertThat(configAttributes.getLocations()).isEqualTo(locations);
    assertEmpty(configAttributes.getClasses());
  }

  @Test
  void processContextConfigurationWithConfigurationClass() {
    Class<?>[] classes = new Class<?>[] { getClass() };
    ContextConfigurationAttributes configAttributes = new ContextConfigurationAttributes(
            getClass(), EMPTY_STRING_ARRAY, classes, true, null, true, ContextLoader.class);
    loader.processContextConfiguration(configAttributes);
    assertThat(configAttributes.getClasses()).isEqualTo(classes);
    assertEmpty(configAttributes.getLocations());
  }

  // --- SmartContextLoader - loadContext() ----------------------------------

  @Test
  void loadContextWithNullConfig() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            loader.loadContext((MergedContextConfiguration) null));
  }

  @Test
  void loadContextWithoutLocationsAndConfigurationClasses() throws Exception {
    MergedContextConfiguration mergedConfig = new MergedContextConfiguration(
            getClass(), EMPTY_STRING_ARRAY, EMPTY_CLASS_ARRAY, EMPTY_STRING_ARRAY, loader);
    assertThatIllegalStateException().isThrownBy(() ->
                    loader.loadContext(mergedConfig))
            .withMessageStartingWith("Neither")
            .withMessageContaining("was able to load an ApplicationContext from");
  }

  /**
   * @since 4.0
   */
  @Test
  void loadContextWithLocationsAndConfigurationClasses() throws Exception {
    MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(),
            new String[] { "test.xml" }, new Class<?>[] { getClass() }, EMPTY_STRING_ARRAY, loader);
    assertThatIllegalStateException().isThrownBy(() ->
                    loader.loadContext(mergedConfig))
            .withMessageStartingWith("Neither")
            .withMessageContaining("declare either 'locations' or 'classes' but not both.");
  }

  private void assertApplicationContextLoadsAndContainsFooString(MergedContextConfiguration mergedConfig)
          throws Exception {

    ApplicationContext applicationContext = loader.loadContext(mergedConfig);
    assertThat(applicationContext).isNotNull();
    assertThat(applicationContext.getBean(String.class)).isEqualTo("foo");
    boolean condition = applicationContext instanceof ConfigurableApplicationContext;
    assertThat(condition).isTrue();
    ((ConfigurableApplicationContext) applicationContext).close();
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

  // --- ContextLoader -------------------------------------------------------

  @Test
  void processLocations() {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            loader.processLocations(getClass(), EMPTY_STRING_ARRAY));
  }

  @Test
  void loadContextFromLocations() throws Exception {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            loader.loadContext(EMPTY_STRING_ARRAY));
  }

  // -------------------------------------------------------------------------

  static class XmlTestCase {
  }

  static class ConfigClassTestCase {

    @Configuration
    static class Config {

      @Bean
      public String foo() {
        return new String("foo");
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
