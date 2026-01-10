/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.annotation.Value;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;

/**
 * {@code @TestConstructor} is a type-level annotation that is used to configure
 * how the parameters of a test class constructor are autowired from components
 * in the test's {@link infra.context.ApplicationContext
 * ApplicationContext}.
 *
 * <p>If {@code @TestConstructor} is not <em>present</em> or <em>meta-present</em>
 * on a test class, the default <em>test constructor autowire mode</em> will be
 * used. See {@link #TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME} for details on
 * how to change the default mode. Note, however, that a local declaration of
 * {@link Autowired @Autowired}
 * {@link jakarta.inject.Inject @jakarta.inject.Inject}, or
 * {@link javax.inject.Inject @javax.inject.Inject} on a constructor takes
 * precedence over both {@code @TestConstructor} and the default mode.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p>this annotation is only supported in conjunction
 * with the {@link InfraExtension
 * ApplicationExtension} for use with JUnit Jupiter. Note that the {@code ApplicationExtension} is
 * often automatically registered for you &mdash; for example, when using annotations such as
 * {@link JUnitConfig @ApplicationJUnitConfig} and
 * {@link JUnitWebConfig @ApplicationJUnitWebConfig}
 * or various test-related annotations from Infra Test.
 *
 * <p> this annotation will be inherited from an
 * enclosing test class by default. See
 * {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Autowired @Autowired
 * @see InfraExtension
 * @see JUnitConfig @ApplicationJUnitConfig
 * @see JUnitWebConfig @ApplicationJUnitWebConfig
 * @see ContextConfiguration @ContextConfiguration
 * @see ContextHierarchy @ContextHierarchy
 * @see ActiveProfiles @ActiveProfiles
 * @see TestPropertySource @TestPropertySource
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestConstructor {

  /**
   * JVM system property used to change the default <em>test constructor
   * autowire mode</em>: {@value #TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}.
   * <p>Acceptable values include enum constants defined in {@link AutowireMode},
   * ignoring case. For example, the default may be changed to {@link AutowireMode#ALL}
   * by supplying the following JVM system property via the command line.
   * <pre style="code">-Dinfra.test.constructor.autowire.mode=all</pre>
   * <p>If the property is not set to {@code ALL}, parameters for test class
   * constructors will be autowired according to {@link AutowireMode#ANNOTATED}
   * semantics by default.
   * <p>May alternatively be configured via the
   * {@link TodayStrategies TodayStrategies}
   * mechanism.
   * <p>this property may also be configured as a
   * <a href="https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params">JUnit
   * Platform configuration parameter</a>.
   *
   * @see #autowireMode
   */
  String TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME = "infra.test.constructor.autowire.mode";

  /**
   * Flag for setting the <em>test constructor {@linkplain AutowireMode autowire
   * mode}</em> for the current test class.
   * <p>Setting this flag overrides the global default. See
   * {@link #TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME} for details on how
   * to change the global default.
   *
   * @return an {@link AutowireMode} to take precedence over the global default
   * @see #TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME
   * @see Autowired @Autowired
   * @see jakarta.inject.Inject @jakarta.inject.Inject
   * @see javax.inject.Inject @javax.inject.Inject
   * @see AutowireMode#ALL
   * @see AutowireMode#ANNOTATED
   */
  AutowireMode autowireMode();

  /**
   * Defines autowiring modes for parameters in a test constructor.
   *
   * @see #ALL
   * @see #ANNOTATED
   */
  enum AutowireMode {

    /**
     * All test constructor parameters will be autowired as if the constructor
     * itself were annotated with
     * {@link Autowired @Autowired},
     * {@link jakarta.inject.Inject @jakarta.inject.Inject}, or
     * {@link javax.inject.Inject @javax.inject.Inject}.
     *
     * @see #ANNOTATED
     */
    ALL,

    /**
     * Each individual test constructor parameter will only be autowired if it
     * is annotated with
     * {@link Autowired @Autowired},
     * {@link Qualifier @Qualifier},
     * or {@link Value @Value},
     * or if the constructor itself is annotated with
     * {@link Autowired @Autowired},
     * {@link jakarta.inject.Inject @jakarta.inject.Inject}, or
     * {@link javax.inject.Inject @javax.inject.Inject}.
     *
     * @see #ALL
     */
    ANNOTATED;

    /**
     * Get the {@code AutowireMode} enum constant with the supplied name,
     * ignoring case.
     *
     * @param name the name of the enum constant to retrieve
     * @return the corresponding enum constant or {@code null} if not found
     * @see AutowireMode#valueOf(String)
     */
    @Nullable
    public static AutowireMode from(@Nullable String name) {
      if (name == null) {
        return null;
      }
      try {
        return AutowireMode.valueOf(name.trim().toUpperCase(Locale.ROOT));
      }
      catch (IllegalArgumentException ex) {
        Logger logger = LoggerFactory.getLogger(AutowireMode.class);
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Failed to parse autowire mode from '%s': %s", name, ex.getMessage()));
        }
        return null;
      }
    }
  }

}
