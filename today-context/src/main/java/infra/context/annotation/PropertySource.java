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

package infra.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.annotation.Autowired;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.io.DefaultPropertySourceFactory;
import infra.core.io.PropertySourceFactory;
import infra.core.io.Resource;
import infra.core.io.ResourcePropertySource;

/**
 * Annotation providing a convenient and declarative mechanism for adding a
 * {@link infra.core.env.PropertySource PropertySource} to Infra
 * {@link Environment Environment}. To be used in
 * conjunction with @{@link Configuration} classes.
 *
 * <h3>Example usage</h3>
 *
 * <p>Given a file {@code app.properties} containing the key/value pair
 * {@code testbean.name=myTestBean}, the following {@code @Configuration} class
 * uses {@code @PropertySource} to contribute {@code app.properties} to the
 * {@code Environment}'s set of {@code PropertySources}.
 *
 * <pre>{@code
 * @Configuration
 * @PropertySource("classpath:/com/myco/app.properties")
 * public class AppConfig {
 *
 *     @Autowired
 *     Environment env;
 *
 *     @Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }
 * }</pre>
 *
 * <p>Notice that the {@code Environment} object is
 * {@link Autowired @Autowired} into the
 * configuration class and then used when populating the {@code TestBean} object. Given
 * the configuration above, a call to {@code testBean.getName()} will return "myTestBean".
 *
 * <h3>Resolving <code>${...}</code> placeholders in {@code <bean>} and {@code @Value} annotations</h3>
 *
 * <p>In order to resolve ${...} placeholders in {@code <bean>} definitions or {@code @Value}
 * annotations using properties from a {@code PropertySource}, you must ensure that an
 * appropriate <em>embedded value resolver</em> is registered in the {@code BeanFactory}
 * used by the {@code ApplicationContext}. This happens automatically when using
 * {@code <context:property-placeholder>} in XML. When using {@code @Configuration} classes
 * this can be achieved by explicitly registering a {@code PropertySourcesPlaceholderConfigurer}
 * via a {@code static} {@code @Bean} method. Note, however, that explicit registration
 * of a {@code PropertySourcesPlaceholderConfigurer} via a {@code static} {@code @Bean}
 * method is typically only required if you need to customize configuration such as the
 * placeholder syntax, etc. See the "Working with externalized values" section of
 * {@link Configuration @Configuration}'s javadocs and "a note on
 * BeanFactoryPostProcessor-returning {@code @Bean} methods" of {@link Bean @Bean}'s
 * javadocs for details and examples.
 *
 * <h3>Resolving ${...} placeholders within {@code @PropertySource} resource locations</h3>
 *
 * <p>Any ${...} placeholders present in a {@code @PropertySource} {@linkplain #value()
 * resource location} will be resolved against the set of property sources already
 * registered against the environment. For example:
 *
 * <pre>{@code
 * @Configuration
 * @PropertySource("classpath:/com/${my.placeholder:default/path}/app.properties")
 * public class AppConfig {
 *
 *     @Autowired
 *     Environment env;
 *
 *     @Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }
 * }</pre>
 *
 * <p>Assuming that "my.placeholder" is present in one of the property sources already
 * registered &mdash; for example, system properties or environment variables &mdash;
 * the placeholder will be resolved to the corresponding value. If not, then "default/path"
 * will be used as a default. Expressing a default value (delimited by colon ":") is
 * optional. If no default is specified and a property cannot be resolved, an {@code
 * IllegalArgumentException} will be thrown.
 *
 * <h3>A note on property overriding with {@code @PropertySource}</h3>
 *
 * <p>In cases where a given property key exists in more than one property resource
 * file, the last {@code @PropertySource} annotation processed will 'win' and override
 * any previous key with the same name.
 *
 * <p>For example, given two properties files {@code a.properties} and
 * {@code b.properties}, consider the following two configuration classes
 * that reference them with {@code @PropertySource} annotations:
 *
 * <pre>{@code
 * @Configuration
 * @PropertySource("classpath:/com/myco/a.properties")
 * public class ConfigA { }
 *
 * @Configuration
 * @PropertySource("classpath:/com/myco/b.properties")
 * public class ConfigB { }
 * }</pre>
 *
 * <p>The override ordering depends on the order in which these classes are registered
 * with the application context.
 *
 * <pre>{@code
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(ConfigA.class);
 * ctx.register(ConfigB.class);
 * ctx.refresh();
 * }</pre>
 *
 * <p>In the scenario above, the properties in {@code b.properties} will override any
 * duplicates that exist in {@code a.properties}, because {@code ConfigB} was registered
 * last.
 *
 * <p>In certain situations, it may not be possible or practical to tightly control
 * property source ordering when using {@code @PropertySource} annotations. For example,
 * if the {@code @Configuration} classes above were registered via component-scanning,
 * the ordering is difficult to predict. In such cases &mdash; and if overriding is important
 * &mdash; it is recommended that the user fall back to using the programmatic
 * {@code PropertySource} API. See {@link ConfigurableEnvironment
 * ConfigurableEnvironment} and {@link infra.core.env.PropertySources
 * MutablePropertySources} javadocs for details.
 *
 * <p>{@code @PropertySource} can be used as a <em>{@linkplain Repeatable repeatable}</em>
 * annotation. {@code @PropertySource} may also be used as a <em>meta-annotation</em>
 * to create custom <em>composed annotations</em> with attribute overrides.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertySources
 * @see Configuration
 * @see infra.core.env.PropertySource
 * @see ConfigurableEnvironment#getPropertySources()
 * @see infra.core.env.PropertySources
 * @since 4.0 2021/10/28 17:24
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PropertySources.class)
public @interface PropertySource {

  /**
   * Indicate the unique name of this property source.
   * <p>If omitted, the {@link #factory} will generate a name based on the
   * underlying resource (in the case of
   * {@link DefaultPropertySourceFactory
   * DefaultPropertySourceFactory}: derived from the resource description through
   * a corresponding name-less
   * {@link ResourcePropertySource
   * ResourcePropertySource} constructor).
   * <p>The name of a {@code PropertySource} serves two general purposes.
   * <ul>
   * <li>Diagnostics: to determine the source of the properties in logging and
   * debugging &mdash; for example, in a Infra App application via Infra App's
   * {@code PropertySourceOrigin}.</li>
   * <li>Programmatic interaction with
   * {@link infra.core.env.PropertySources MutablePropertySources}:
   * the name can be used to retrieve properties from a particular property
   * source (or to determine if a particular named property source already exists).
   * The name can also be used to add a new property source relative to an existing
   * property source (see
   * {@link infra.core.env.PropertySources#addBefore addBefore()} and
   * {@link infra.core.env.PropertySources#addAfter addAfter()}).</li>
   * </ul>
   *
   * @see infra.core.env.PropertySource#getName()
   * @see Resource#toString()
   */
  String name() default "";

  /**
   * Indicate the resource locations of the properties files to be loaded.
   * <p>The default {@link #factory() factory} supports both traditional and
   * XML-based properties file formats &mdash; for example,
   * {@code "classpath:/com/myco/app.properties"} or {@code "file:/path/to/file.xml"}.
   * <p>resource location wildcards are also
   * supported &mdash; for example, {@code "classpath*:/config/*.properties"}.
   * <p>{@code ${...}} placeholders will be resolved against property sources already
   * registered with the {@code Environment}. See {@linkplain PropertySource above}
   * for examples.
   * <p>Each location will be added to the enclosing {@code Environment} as its own
   * property source, and in the order declared (or in the order in which resource
   * locations are resolved when location wildcards are used).
   */
  String[] value();

  /**
   * Indicate if a failure to find a {@link #value property resource} should be
   * ignored.
   * <p>{@code true} is appropriate if the properties file is completely optional.
   * <p>Default is {@code false}.
   */
  boolean ignoreResourceNotFound() default false;

  /**
   * A specific character encoding for the given resources, e.g. "UTF-8".
   */
  String encoding() default "";

  /**
   * Specify a custom {@link PropertySourceFactory}, if any.
   * <p>By default, a default factory for standard resource files will be used
   * which supports {@code *.properties} and {@code *.xml} file formats for
   * {@link java.util.Properties}.
   *
   * @see DefaultPropertySourceFactory
   * @see ResourcePropertySource
   */
  Class<? extends PropertySourceFactory> factory() default PropertySourceFactory.class;

}
