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

package cn.taketoday.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.stereotype.Component;

/**
 * Indicates that a method produces a bean to be managed by the  container.
 *
 * <h3>Overview</h3>
 *
 * <p>The names and semantics of the attributes to this annotation are intentionally
 * similar to those of the {@code <bean/>} element in the  XML schema. For
 * example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Bean Names</h3>
 *
 * <p>While a {@link #name} attribute is available, the default strategy for
 * determining the name of a bean is to use the name of the {@code @Component} method.
 * This is convenient and intuitive, but if explicit naming is desired, the
 * {@code name} attribute (or its alias {@code value}) may be used. Also note
 * that {@code name} accepts an array of Strings, allowing for multiple names
 * (i.e. a primary bean name plus one or more aliases) for a single bean.
 *
 * <pre class="code">
 *     &#064;Bean({"b1", "b2"}) // bean available as 'b1' and 'b2', but not 'myBean'
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Profile, Scope, Lazy, DependsOn, Primary, Order</h3>
 *
 * <p>Note that the {@code @Bean} annotation does not provide attributes for profile,
 * scope, lazy, depends-on or primary. Rather, it should be used in conjunction with
 * {@link Scope @Scope}, {@link Lazy @Lazy}, {@link DependsOn @DependsOn} and
 * {@link Primary @Primary} annotations to declare those semantics. For example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     &#064;Profile("production")
 *     &#064;Scope("prototype")
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * The semantics of the above-mentioned annotations match their use at the component
 * class level: {@code @Profile} allows for selective inclusion of certain beans.
 * {@code @Scope} changes the bean's scope from singleton to the specified scope.
 * {@code @Lazy} only has an actual effect in case of the default singleton scope.
 * {@code @DependsOn} enforces the creation of specific other beans before this
 * bean will be created, in addition to any dependencies that the bean expressed
 * through direct references, which is typically helpful for singleton startup.
 * {@code @Primary} is a mechanism to resolve ambiguity at the injection point level
 * if a single target component needs to be injected but several beans match by type.
 *
 * <p>Additionally, {@code @Bean} methods may also declare qualifier annotations
 * and {@link Order @Order} values, to be
 * taken into account during injection point resolution just like corresponding
 * annotations on the corresponding component classes but potentially being very
 * individual per bean definition (in case of multiple definitions with the same
 * bean class). Qualifiers narrow the set of candidates after the initial type match;
 * order values determine the order of resolved elements in case of collection
 * injection points (with several target beans matching by type and qualifier).
 *
 * <p><b>NOTE:</b> {@code @Order} values may influence priorities at injection points,
 * but please be aware that they do not influence singleton startup order which is an
 * orthogonal concern determined by dependency relationships and {@code @DependsOn}
 * declarations as mentioned above. Also, {@link jakarta.annotation.Priority} is not
 * available at this level since it cannot be declared on methods; its semantics can
 * be modeled through {@code @Order} values in combination with {@code @Primary} on
 * a single bean per type.
 *
 * <h3>{@code @Bean} Methods in {@code @Configuration} Classes</h3>
 *
 * <p>Typically, {@code @Bean} methods are declared within {@code @Configuration}
 * classes. In this case, bean methods may reference other {@code @Bean} methods in the
 * same class by calling them <i>directly</i>. This ensures that references between beans
 * are strongly typed and navigable. Such so-called <em>'inter-bean references'</em> are
 * guaranteed to respect scoping and AOP semantics, just like {@code getBean()} lookups
 * would. These are the semantics known from the original 'Framework JavaConfig' project
 * which require CGLIB subclassing of each such configuration class at runtime. As a
 * consequence, {@code @Configuration} classes and their factory methods must not be
 * marked as final or private in this mode. For example:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService(fooRepository());
 *     }
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>{@code @Bean} <em>Lite</em> Mode</h3>
 *
 * <p>{@code @Bean} methods may also be declared within classes that are <em>not</em>
 * annotated with {@code @Configuration}. For example, bean methods may be declared
 * in a {@code @Component} class or even in a <em>plain old class</em>. In such cases,
 * a {@code @Bean} method will get processed in a so-called <em>'lite'</em> mode.
 *
 * <p>Bean methods in <em>lite</em> mode will be treated as plain <em>factory
 * methods</em> by the container (similar to {@code factory-method} declarations
 * in XML), with scoping and lifecycle callbacks properly applied. The containing
 * class remains unmodified in this case, and there are no unusual constraints for
 * the containing class or the factory methods.
 *
 * <p>In contrast to the semantics for bean methods in {@code @Configuration} classes,
 * <em>'inter-bean references'</em> are not supported in <em>lite</em> mode. Instead,
 * when one {@code @Bean}-method invokes another {@code @Bean}-method in <em>lite</em>
 * mode, the invocation is a standard Java method invocation; Framework does not intercept
 * the invocation via a CGLIB proxy. This is analogous to inter-{@code @Transactional}
 * method calls where in proxy mode, Framework does not intercept the invocation &mdash;
 * Framework does so only in AspectJ mode.
 *
 * <p>For example:
 *
 * <pre class="code">
 * &#064;Component
 * public class Calculator {
 *     public int sum(int a, int b) {
 *         return a+b;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean();
 *     }
 * }</pre>
 *
 * <h3>Bootstrapping</h3>
 *
 * <p>See the @{@link Configuration} javadoc for further details including how to bootstrap
 * the container using {@link StandardApplicationContext} and friends.
 *
 * <h3>{@code BeanFactoryPostProcessor}-returning {@code @Bean} methods</h3>
 *
 * <p>Special consideration must be taken for {@code @Bean} methods that return Framework
 * {@link BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * ({@code BFPP}) types. Because {@code BFPP} objects must be instantiated very early in the
 * container lifecycle, they can interfere with processing of annotations such as {@code @Autowired},
 * {@code @Value}, and {@code @PostConstruct} within {@code @Configuration} classes. To avoid these
 * lifecycle issues, mark {@code BFPP}-returning {@code @Bean} methods as {@code static}. For example:
 *
 * <pre class="code">
 *     &#064;Bean
 *     public static PropertySourcesPlaceholderConfigurer pspc() {
 *         // instantiate, configure and return pspc ...
 *     }
 * </pre>
 *
 * By marking this method as {@code static}, it can be invoked without causing instantiation of its
 * declaring {@code @Configuration} class, thus avoiding the above-mentioned lifecycle conflicts.
 * Note however that {@code static} {@code @Bean} methods will not be enhanced for scoping and AOP
 * semantics as mentioned above. This works out in {@code BFPP} cases, as they are not typically
 * referenced by other {@code @Bean} methods. As a reminder, an INFO-level log message will be
 * issued for any non-static {@code @Bean} methods having a return type assignable to
 * {@code BeanFactoryPostProcessor}.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see Configuration
 * @see Scope
 * @see DependsOn
 * @see Lazy
 * @see Primary
 * @see Component
 * @see Autowired
 * @see Value
 * @since 4.0
 */
@Component
@Documented
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {

  /**
   * Alias for {@link #name}.
   * <p>Intended to be used when no other attributes are needed, for example:
   * {@code @Bean("customBeanName")}.
   *
   * @see #name
   */
  @AliasFor(annotation = Component.class)
  String[] value() default {};

  /**
   * The name of this bean, or if several names, a primary bean name plus aliases.
   * <p>If left unspecified, the name of the bean is the name of the annotated method.
   * If specified, the method name is ignored.
   * <p>The bean name and aliases may also be configured via the {@link #value}
   * attribute if no other attributes are declared.
   *
   * @see #value
   */
  @AliasFor(annotation = Component.class)
  String[] name() default {};

  /**
   * The optional name of a method to call on the bean instance during initialization.
   * Not commonly used, given that the method may be called programmatically directly
   * within the body of a Bean-annotated method.
   * <p>The default value is {@code ""}, indicating no init method to be called.
   *
   * @see InitializingBean
   * @see ConfigurableApplicationContext#refresh()
   */
  @AliasFor(annotation = Component.class)
  String[] initMethods() default {};

  /**
   * The optional name of a method to call on the bean instance upon closing the
   * application context, for example a {@code close()} method on a JDBC
   * {@code DataSource} implementation, or a Hibernate {@code SessionFactory} object.
   * The method must have no arguments but may throw any exception.
   * <p>As a convenience to the user, the container will attempt to infer a destroy
   * method against an object returned from the {@code @Bean} method. For example, given
   * an {@code @Bean} method returning an Apache Commons DBCP {@code BasicDataSource},
   * the container will notice the {@code close()} method available on that object and
   * automatically register it as the {@code destroyMethod}. This 'destroy method
   * inference' is currently limited to detecting only public, no-arg methods named
   * 'close' or 'shutdown'. The method may be declared at any level of the inheritance
   * hierarchy and will be detected regardless of the return type of the {@code @Bean}
   * method (i.e., detection occurs reflectively against the bean instance itself at
   * creation time).
   * <p>To disable destroy method inference for a particular {@code @Bean}, specify an
   * empty string as the value, e.g. {@code @Bean(destroyMethod="")}. Note that the
   * {@link DisposableBean} callback interface will
   * nevertheless get detected and the corresponding destroy method invoked: In other
   * words, {@code destroyMethod=""} only affects custom close/shutdown methods and
   * {@link java.io.Closeable}/{@link java.lang.AutoCloseable} declared close methods.
   * <p>Note: Only invoked on beans whose lifecycle is under the full control of the
   * factory, which is always the case for singletons but not guaranteed for any
   * other scope.
   *
   * @see DisposableBean
   * @see ConfigurableApplicationContext#close()
   */
  @AliasFor(annotation = Component.class)
  String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;

  /**
   * Is this bean a candidate for getting autowired into some other bean?
   * <p>Default is {@code true}; set this to {@code false} for internal delegates
   * that are not meant to get in the way of beans of the same type in other places.
   *
   * @since 4.0
   */
  @AliasFor(annotation = Component.class)
  boolean autowireCandidate() default true;
}
