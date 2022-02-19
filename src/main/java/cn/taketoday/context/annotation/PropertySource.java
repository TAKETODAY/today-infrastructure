/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.io.PropertySourceFactory;

/**
 * Annotation providing a convenient and declarative mechanism for adding a
 * {@link cn.taketoday.core.env.PropertySource PropertySource} to
 * {@link cn.taketoday.core.env.Environment Environment}. To be used in
 * conjunction with @{@link Configuration} classes.
 *
 * <h3>Example usage</h3>
 *
 * <p>Given a file {@code app.properties} containing the key/value pair
 * {@code testbean.name=myTestBean}, the following {@code @Configuration} class
 * uses {@code @PropertySource} to contribute {@code app.properties} to the
 * {@code Environment}'s set of {@code PropertySources}.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * Notice that the {@code Environment} object is
 * {@link Autowired @Autowired} into the
 * configuration class and then used when populating the {@code TestBean} object. Given
 * the configuration above, a call to {@code testBean.getName()} will return "myTestBean".
 *
 * <h3>Resolving ${...} placeholders in {@code <bean>} and {@code @Value} annotations</h3>
 *
 * In order to resolve ${...} placeholders in {@code <bean>} definitions or {@code @Value}
 * annotations using properties from a {@code PropertySource}, one must register
 * a {@code PropertySourcesPlaceholderConfigurer}. This happens automatically when using
 * {@code <context:property-placeholder>} in XML, but must be explicitly registered using
 * a {@code static} {@code @Component} method when using {@code @Configuration} classes. See
 * the "Working with externalized values" section of @{@link Configuration}'s javadoc and
 * "a note on BeanFactoryPostProcessor-returning @Component methods" of
 * {@link cn.taketoday.lang.Component}'s javadoc for details and examples.
 *
 * <h3>Resolving ${...} placeholders within {@code @PropertySource} resource locations</h3>
 *
 * Any ${...} placeholders present in a {@code @PropertySource} {@linkplain #value()
 * resource location} will be resolved against the set of property sources already
 * registered against the environment. For example:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/${my.placeholder:default/path}/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * Assuming that "my.placeholder" is present in one of the property sources already
 * registered, e.g. system properties or environment variables, the placeholder will
 * be resolved to the corresponding value. If not, then "default/path" will be used as a
 * default. Expressing a default value (delimited by colon ":") is optional.  If no
 * default is specified and a property cannot be resolved, an {@code
 * IllegalArgumentException} will be thrown.
 *
 * <h3>A note on property overriding with @PropertySource</h3>
 *
 * In cases where a given property key exists in more than one {@code .properties}
 * file, the last {@code @PropertySource} annotation processed will 'win' and override.
 *
 * For example, given two properties files {@code a.properties} and
 * {@code b.properties}, consider the following two configuration classes
 * that reference them with {@code @PropertySource} annotations:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/a.properties")
 * public class ConfigA { }
 *
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/b.properties")
 * public class ConfigB { }
 * </pre>
 *
 * The override ordering depends on the order in which these classes are registered
 * with the application context.
 *
 * <pre class="code">
 * StandardApplicationContext ctx = new StandardApplicationContext();
 * ctx.register(ConfigA.class);
 * ctx.register(ConfigB.class);
 * ctx.refresh();
 * </pre>
 *
 * In the scenario above, the properties in {@code b.properties} will override any
 * duplicates that exist in {@code a.properties}, because {@code ConfigB} was registered
 * last.
 *
 * <p>In certain situations, it may not be possible or practical to tightly control
 * property source ordering when using {@code @PropertySource} annotations. For example,
 * if the {@code @Configuration} classes above were registered via component-scanning,
 * the ordering is difficult to predict. In such cases - and if overriding is important -
 * it is recommended that the user fall back to using the programmatic PropertySource API.
 * See {@link cn.taketoday.core.env.ConfigurableEnvironment ConfigurableEnvironment} and
 * {@link cn.taketoday.core.env.PropertySources PropertySources} javadocs for details.
 *
 * <p><b>NOTE: This annotation is repeatable according to Java 8 conventions.</b>
 * However, all such {@code @PropertySource} annotations need to be declared at the same
 * level: either directly on the configuration class or as meta-annotations within the
 * same custom annotation. Mixing of direct annotations and meta-annotations is not
 * recommended since direct annotations will effectively override meta-annotations.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author TODAY 2021/10/28 17:24
 * @see PropertySources
 * @see Configuration
 * @see cn.taketoday.core.env.PropertySource
 * @see cn.taketoday.core.env.ConfigurableEnvironment#getPropertySources()
 * @see cn.taketoday.core.env.PropertySources
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PropertySources.class)
public @interface PropertySource {

  /**
   * Indicate the name of this property source. If omitted, a name will
   * be generated based on the description of the underlying resource.
   *
   * @see cn.taketoday.core.env.PropertySource#getName()
   * @see cn.taketoday.core.io.Resource#toString()
   */
  String name() default "";

  /**
   * Indicate the resource location(s) of the properties file to be loaded.
   * <p>Both traditional and XML-based properties file formats are supported
   * &mdash; for example, {@code "classpath:/com/myco/app.properties"}
   * or {@code "file:/path/to/file.xml"}.
   * <p>Resource location wildcards (e.g. *&#42;/*.properties) are not permitted;
   * each location must evaluate to exactly one {@code .properties} resource.
   * <p>${...} placeholders will be resolved against any/all property sources already
   * registered with the {@code Environment}. See {@linkplain PropertySource above}
   * for examples.
   * <p>Each location will be added to the enclosing {@code Environment} as its own
   * property source, and in the order declared.
   */
  String[] value();

  /**
   * Indicate if failure to find the a {@link #value() property resource} should be
   * ignored.
   * <p>{@code true} is appropriate if the properties file is completely optional.
   * Default is {@code false}.
   */
  boolean ignoreResourceNotFound() default false;

  /**
   * A specific character encoding for the given resources, e.g. "UTF-8".
   */
  String encoding() default "";

  /**
   * Specify a custom {@link PropertySourceFactory}, if any.
   * <p>By default, a default factory for standard resource files will be used.
   *
   * @see cn.taketoday.core.io.DefaultPropertySourceFactory
   * @see cn.taketoday.core.io.ResourcePropertySource
   */
  Class<? extends PropertySourceFactory> factory() default PropertySourceFactory.class;

}
