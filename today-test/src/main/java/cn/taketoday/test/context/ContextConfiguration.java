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

package cn.taketoday.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.test.context.junit.jupiter.ApplicationJUnitConfig;
import cn.taketoday.test.context.junit.jupiter.web.ApplicationJUnitWebConfig;
import cn.taketoday.test.context.support.AbstractContextLoader;
import cn.taketoday.test.context.support.AnnotationConfigContextLoader;
import cn.taketoday.test.context.support.DelegatingSmartContextLoader;
import cn.taketoday.test.context.support.GenericXmlContextLoader;
import cn.taketoday.test.context.web.AnnotationConfigWebContextLoader;
import cn.taketoday.test.context.web.GenericXmlWebContextLoader;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.context.web.WebDelegatingSmartContextLoader;

/**
 * {@code @ContextConfiguration} defines class-level metadata that is used to determine
 * how to load and configure an {@link cn.taketoday.context.ApplicationContext
 * ApplicationContext} for integration tests.
 *
 * <h3>Supported Resource Types</h3>
 *
 * <p>only path-based resource locations (typically XML configuration
 * files) were supported.  {@linkplain #loader context loaders} may
 * choose to support <em>either</em> path-based <em>or</em> class-based resources.
 * {@linkplain #loader context loaders} may choose to support path-based
 * <em>and</em> class-based resources simultaneously. Consequently
 * {@code @ContextConfiguration} can be used to declare either path-based resource
 * locations (via the {@link #locations} or {@link #value} attribute) <em>or</em>
 * component classes (via the {@link #classes} attribute). Note, however, that most
 * implementations of {@link SmartContextLoader} only support a single resource type.
 * path-based resource locations may be either XML configuration files or
 * Groovy scripts (if Groovy is on the classpath). Of course, third-party frameworks may
 * choose to support additional types of path-based resources.
 *
 * <h3>Component Classes</h3>
 *
 * <p>The term <em>component class</em> can refer to any of the following.
 *
 * <ul>
 * <li>A class annotated with {@link cn.taketoday.context.annotation.Configuration @Configuration}</li>
 * <li>A component (i.e., a class annotated with
 * {@link cn.taketoday.lang.Component @Component},
 * {@link cn.taketoday.lang.Service @Service},
 * {@link cn.taketoday.lang.Repository @Repository}, etc.)</li>
 * <li>A JSR-330 compliant class that is annotated with {@code jakarta.inject} annotations</li>
 * <li>Any class that contains {@link cn.taketoday.context.annotation.Bean @Bean}-methods</li>
 * <li>Any other class that is intended to be registered as a Spring component (i.e., a Spring bean
 * in the {@code ApplicationContext}), potentially taking advantage of automatic autowiring of a
 * single constructor without the use of Spring annotations</li>
 * </ul>
 *
 * A bean will be registered in the {@code ApplicationContext} for each component
 * class, and such beans can therefore be injected into other beans or into the
 * instance of the test class.
 *
 * <p>Consult the Javadoc for {@link cn.taketoday.context.annotation.Configuration @Configuration}
 * and {@link cn.taketoday.context.annotation.Bean @Bean} for further
 * information regarding the configuration and semantics of <em>component classes</em>.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p>this annotation will be inherited from an enclosing test class by default.
 * See {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * @author Sam Brannen
 * @see ApplicationJUnitConfig @SpringJUnitConfig
 * @see ApplicationJUnitWebConfig @SpringJUnitWebConfig
 * @see ContextHierarchy @ContextHierarchy
 * @see ActiveProfiles @ActiveProfiles
 * @see TestPropertySource @TestPropertySource
 * @see SmartContextLoader
 * @see ContextConfigurationAttributes
 * @see MergedContextConfiguration
 * @see cn.taketoday.context.ApplicationContext ApplicationContext
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ContextConfiguration {

  /**
   * Alias for {@link #locations}.
   * <p>This attribute may <strong>not</strong> be used in conjunction with
   * {@link #locations}, but it may be used instead of {@link #locations}.
   *
   * @see #inheritLocations
   * @since 4.0
   */
  @AliasFor("locations")
  String[] value() default {};

  /**
   * The resource locations to use for loading an
   * {@link cn.taketoday.context.ApplicationContext ApplicationContext}.
   * <p>Check out the Javadoc for
   * {@link AbstractContextLoader#modifyLocations
   * AbstractContextLoader.modifyLocations()} for details on how a location
   * will be interpreted at runtime, in particular in case of a relative
   * path. Also, check out the documentation on
   * {@link AbstractContextLoader#generateDefaultLocations
   * AbstractContextLoader.generateDefaultLocations()} for details on the
   * default locations that are going to be used if none are specified.
   * <p>Note that the aforementioned default rules only apply for a standard
   * {@link AbstractContextLoader
   * AbstractContextLoader} subclass such as
   * {@link GenericXmlContextLoader GenericXmlContextLoader}
   * which are the effective default implementations used at runtime if
   * {@code locations} are configured. See the documentation for {@link #loader}
   * for further details regarding default loaders.
   * <p>This attribute may <strong>not</strong> be used in conjunction with
   * {@link #value}, but it may be used instead of {@link #value}.
   *
   * @see #inheritLocations
   */
  @AliasFor("value")
  String[] locations() default {};

  /**
   * The <em>component classes</em> to use for loading an
   * {@link cn.taketoday.context.ApplicationContext ApplicationContext}.
   * <p>Check out the javadoc for
   * {@link AnnotationConfigContextLoader#detectDefaultConfigurationClasses
   * AnnotationConfigContextLoader.detectDefaultConfigurationClasses()} for details
   * on how default configuration classes will be detected if no
   * <em>component classes</em> are specified. See the documentation for
   * {@link #loader} for further details regarding default loaders.
   *
   * @see cn.taketoday.context.annotation.Configuration
   * @see AnnotationConfigContextLoader
   * @see #inheritLocations
   */
  Class<?>[] classes() default {};

  /**
   * The application context <em>initializer classes</em> to use for initializing
   * a {@link ConfigurableApplicationContext}.
   * <p>The concrete {@code ConfigurableApplicationContext} type supported by each
   * declared initializer must be compatible with the type of {@code ApplicationContext}
   * created by the {@link SmartContextLoader} in use.
   * <p>{@code SmartContextLoader} implementations typically detect whether
   * Framework's {@link cn.taketoday.core.Ordered Ordered} interface has been
   * implemented or if the @{@link cn.taketoday.core.annotation.Order Order}
   * annotation is present and sort instances accordingly prior to invoking them.
   *
   * @see cn.taketoday.context.ApplicationContextInitializer
   * @see cn.taketoday.context.ConfigurableApplicationContext
   * @see #inheritInitializers
   * @see #loader
   */
  Class<? extends ApplicationContextInitializer>[] initializers() default {};

  /**
   * Whether or not {@linkplain #locations resource locations} or
   * {@linkplain #classes <em>component classes</em>} from test superclasses
   * should be <em>inherited</em>.
   * <p>The default value is {@code true}. This means that an annotated test
   * class will <em>inherit</em> the resource locations or component classes
   * defined by test superclasses. Specifically, the resource locations or
   * component classes for a given test class will be appended to the list of
   * resource locations or component classes defined by test superclasses.
   * Thus, subclasses have the option of <em>extending</em> the list of resource
   * locations or component classes.
   * <p>If {@code inheritLocations} is set to {@code false}, the
   * resource locations or component classes for the annotated test class
   * will <em>shadow</em> and effectively replace any resource locations
   * or component classes defined by superclasses.
   * <p>In the following example that uses path-based resource locations, the
   * {@link cn.taketoday.context.ApplicationContext ApplicationContext}
   * for {@code ExtendedTest} will be loaded from
   * {@code "base-context.xml"} <strong>and</strong>
   * {@code "extended-context.xml"}, in that order. Beans defined in
   * {@code "extended-context.xml"} may therefore override those defined
   * in {@code "base-context.xml"}.
   * <pre class="code">
   * &#064;ContextConfiguration("base-context.xml")
   * public class BaseTest {
   *     // ...
   * }
   *
   * &#064;ContextConfiguration("extended-context.xml")
   * public class ExtendedTest extends BaseTest {
   *     // ...
   * }
   * </pre>
   * <p>Similarly, in the following example that uses component classes, the
   * {@link cn.taketoday.context.ApplicationContext ApplicationContext}
   * for {@code ExtendedTest} will be loaded from the
   * {@code BaseConfig} <strong>and</strong> {@code ExtendedConfig}
   * configuration classes, in that order. Beans defined in
   * {@code ExtendedConfig} may therefore override those defined in
   * {@code BaseConfig}.
   * <pre class="code">
   * &#064;ContextConfiguration(classes=BaseConfig.class)
   * public class BaseTest {
   *     // ...
   * }
   *
   * &#064;ContextConfiguration(classes=ExtendedConfig.class)
   * public class ExtendedTest extends BaseTest {
   *     // ...
   * }
   * </pre>
   */
  boolean inheritLocations() default true;

  /**
   * Whether or not {@linkplain #initializers context initializers} from test
   * superclasses should be <em>inherited</em>.
   * <p>The default value is {@code true}. This means that an annotated test
   * class will <em>inherit</em> the application context initializers defined
   * by test superclasses. Specifically, the initializers for a given test
   * class will be added to the set of initializers defined by test
   * superclasses. Thus, subclasses have the option of <em>extending</em> the
   * set of initializers.
   * <p>If {@code inheritInitializers} is set to {@code false}, the initializers
   * for the annotated test class will <em>shadow</em> and effectively replace
   * any initializers defined by superclasses.
   * <p>In the following example, the
   * {@link cn.taketoday.context.ApplicationContext ApplicationContext}
   * for {@code ExtendedTest} will be initialized using
   * {@code BaseInitializer} <strong>and</strong> {@code ExtendedInitializer}.
   * Note, however, that the order in which the initializers are invoked
   * depends on whether they implement {@link cn.taketoday.core.Ordered
   * Ordered} or are annotated with {@link cn.taketoday.core.annotation.Order
   * &#064;Order}.
   * <pre class="code">
   * &#064;ContextConfiguration(initializers = BaseInitializer.class)
   * public class BaseTest {
   *     // ...
   * }
   *
   * &#064;ContextConfiguration(initializers = ExtendedInitializer.class)
   * public class ExtendedTest extends BaseTest {
   *     // ...
   * }
   * </pre>
   */
  boolean inheritInitializers() default true;

  /**
   * The type of {@link SmartContextLoader} (or {@link ContextLoader}) to use
   * for loading an {@link cn.taketoday.context.ApplicationContext
   * ApplicationContext}.
   * <p>If not specified, the loader will be inherited from the first superclass
   * that is annotated or meta-annotated with {@code @ContextConfiguration} and
   * specifies an explicit loader. If no class in the hierarchy specifies an
   * explicit loader, a default loader will be used instead.
   * <p>The default concrete implementation chosen at runtime will be either
   * {@link DelegatingSmartContextLoader
   * DelegatingSmartContextLoader} or
   * {@link WebDelegatingSmartContextLoader
   * WebDelegatingSmartContextLoader} depending on the absence or presence of
   * {@link WebAppConfiguration
   * &#064;WebAppConfiguration}. For further details on the default behavior
   * of various concrete {@code SmartContextLoaders}, check out the Javadoc for
   * {@link AbstractContextLoader AbstractContextLoader},
   * {@link GenericXmlContextLoader GenericXmlContextLoader},
   * {@link AnnotationConfigContextLoader AnnotationConfigContextLoader},
   * {@link GenericXmlWebContextLoader GenericXmlWebContextLoader},
   * {@link AnnotationConfigWebContextLoader AnnotationConfigWebContextLoader}.
   */
  Class<? extends ContextLoader> loader() default ContextLoader.class;

  /**
   * The name of the context hierarchy level represented by this configuration.
   * <p>If not specified the name will be inferred based on the numerical level
   * within all declared contexts within the hierarchy.
   * <p>This attribute is only applicable when used within a test class hierarchy
   * that is configured using {@code @ContextHierarchy}, in which case the name
   * can be used for <em>merging</em> or <em>overriding</em> this configuration
   * with configuration of the same name in hierarchy levels defined in superclasses.
   * See the Javadoc for {@link ContextHierarchy @ContextHierarchy} for details.
   */
  String name() default "";

}
