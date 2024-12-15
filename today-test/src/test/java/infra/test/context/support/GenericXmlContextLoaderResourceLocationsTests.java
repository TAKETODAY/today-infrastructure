/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.support;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import infra.core.annotation.AnnotationUtils;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextLoader;
import infra.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit test which verifies proper
 * {@link ContextLoader#processLocations(Class, String...) processing} of
 * {@code resource locations} by a {@link GenericXmlContextLoader}
 * configured via {@link ContextConfiguration @ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class GenericXmlContextLoaderResourceLocationsTests {

  private static final Logger logger = LoggerFactory.getLogger(GenericXmlContextLoaderResourceLocationsTests.class);

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("contextConfigurationLocationsData")
  void assertContextConfigurationLocations(Class<?> testClass, String[] expectedLocations) throws Exception {
    ContextConfiguration contextConfig = testClass.getAnnotation(ContextConfiguration.class);
    ContextLoader contextLoader = new GenericXmlContextLoader();
    String[] configuredLocations = (String[]) AnnotationUtils.getValue(contextConfig);
    String[] processedLocations = contextLoader.processLocations(testClass, configuredLocations);

    if (logger.isDebugEnabled()) {
      logger.debug("----------------------------------------------------------------------");
      logger.debug("Configured locations: " + ObjectUtils.nullSafeToString(configuredLocations));
      logger.debug("Expected   locations: " + ObjectUtils.nullSafeToString(expectedLocations));
      logger.debug("Processed  locations: " + ObjectUtils.nullSafeToString(processedLocations));
    }

    assertThat(processedLocations).as("Verifying locations for test [" + testClass + "].").isEqualTo(expectedLocations);
  }

  static Stream<Arguments> contextConfigurationLocationsData() {
    return Stream.of(
            args(ClasspathNonExistentDefaultLocationsTestCase.class, array()),

            args(ClasspathExistentDefaultLocationsTestCase.class, array(
                    "classpath:/infra/test/context/support/GenericXmlContextLoaderResourceLocationsTests$ClasspathExistentDefaultLocationsTestCase-context.xml")),

            args(ImplicitClasspathLocationsTestCase.class,
                    array("classpath:/infra/test/context/support/context1.xml",
                            "classpath:/infra/test/context/support/context2.xml")),

            args(ExplicitClasspathLocationsTestCase.class, array("classpath:context.xml")),

            args(ExplicitFileLocationsTestCase.class, array("file:/testing/directory/context.xml")),

            args(ExplicitUrlLocationsTestCase.class, array("https://example.com/context.xml")),

            args(ExplicitMixedPathTypesLocationsTestCase.class,
                    array("classpath:/infra/test/context/support/context1.xml", "classpath:context2.xml",
                            "classpath:/context3.xml", "file:/testing/directory/context.xml",
                            "https://example.com/context.xml"))
    );
  }

  private static Arguments args(Class<?> testClass, String[] expectedLocations) {
    return arguments(named(testClass.getSimpleName(), testClass), expectedLocations);
  }

  private static String[] array(String... elements) {
    return elements;
  }

  @ContextConfiguration
  class ClasspathNonExistentDefaultLocationsTestCase {
  }

  @ContextConfiguration
  class ClasspathExistentDefaultLocationsTestCase {
  }

  @ContextConfiguration({ "context1.xml", "context2.xml" })
  class ImplicitClasspathLocationsTestCase {
  }

  @ContextConfiguration("classpath:context.xml")
  class ExplicitClasspathLocationsTestCase {
  }

  @ContextConfiguration("file:/testing/directory/context.xml")
  class ExplicitFileLocationsTestCase {
  }

  @ContextConfiguration("https://example.com/context.xml")
  class ExplicitUrlLocationsTestCase {
  }

  @ContextConfiguration({ "context1.xml", "classpath:context2.xml", "/context3.xml",
          "file:/testing/directory/context.xml", "https://example.com/context.xml" })
  class ExplicitMixedPathTypesLocationsTestCase {
  }

}
