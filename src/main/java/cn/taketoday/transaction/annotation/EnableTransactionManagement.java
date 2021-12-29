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

package cn.taketoday.transaction.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.AdviceMode;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.Ordered;
import cn.taketoday.transaction.PlatformTransactionManager;

/**
 * Enables Framework's annotation-driven transaction management capability, similar to
 * the support found in Framework's {@code <tx:*>} XML namespace. To be used on
 * {@link cn.taketoday.context.annotation.Configuration @Configuration}
 * classes to configure traditional, imperative transaction management or
 * reactive transaction management.
 *
 * <p>The following example demonstrates imperative transaction management
 * using a {@link PlatformTransactionManager
 * PlatformTransactionManager}. For reactive transaction management, configure a
 * {@link cn.taketoday.transaction.ReactiveTransactionManager
 * ReactiveTransactionManager} instead.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTransactionManagement
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         // configure and return a class having &#064;Transactional methods
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // configure and return the necessary JDBC DataSource
 *     }
 *
 *     &#064;Bean
 *     public PlatformTransactionManager txManager() {
 *         return new DataSourceTransactionManager(dataSource());
 *     }
 * }</pre>
 *
 * <p>For reference, the example above can be compared to the following Framework XML
 * configuration:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;tx:annotation-driven/&gt;
 *
 *     &lt;bean id="fooRepository" class="com.foo.JdbcFooRepository"&gt;
 *         &lt;constructor-arg ref="dataSource"/&gt;
 *     &lt;/bean&gt;
 *
 *     &lt;bean id="dataSource" class="com.vendor.VendorDataSource"/&gt;
 *
 *     &lt;bean id="transactionManager" class="org.sfwk...DataSourceTransactionManager"&gt;
 *         &lt;constructor-arg ref="dataSource"/&gt;
 *     &lt;/bean&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * In both of the scenarios above, {@code @EnableTransactionManagement} and {@code
 * <tx:annotation-driven/>} are responsible for registering the necessary Framework
 * components that power annotation-driven transaction management, such as the
 * TransactionInterceptor and the proxy- or AspectJ-based advice that weaves the
 * interceptor into the call stack when {@code JdbcFooRepository}'s {@code @Transactional}
 * methods are invoked.
 *
 * <p>A minor difference between the two examples lies in the naming of the {@code
 * TransactionManager} bean: In the {@code @Bean} case, the name is
 * <em>"txManager"</em> (per the name of the method); in the XML case, the name is
 * <em>"transactionManager"</em>. {@code <tx:annotation-driven/>} is hard-wired to
 * look for a bean named "transactionManager" by default, however
 * {@code @EnableTransactionManagement} is more flexible; it will fall back to a by-type
 * lookup for any {@code TransactionManager} bean in the container. Thus the name
 * can be "txManager", "transactionManager", or "tm": it simply does not matter.
 *
 * <p>For those that wish to establish a more direct relationship between
 * {@code @EnableTransactionManagement} and the exact transaction manager bean to be used,
 * the {@link TransactionManagementConfigurer} callback interface may be implemented -
 * notice the {@code implements} clause and the {@code @Override}-annotated method below:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTransactionManagement
 * public class AppConfig implements TransactionManagementConfigurer {
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         // configure and return a class having &#064;Transactional methods
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // configure and return the necessary JDBC DataSource
 *     }
 *
 *     &#064;Bean
 *     public PlatformTransactionManager txManager() {
 *         return new DataSourceTransactionManager(dataSource());
 *     }
 *
 *     &#064;Override
 *     public PlatformTransactionManager annotationDrivenTransactionManager() {
 *         return txManager();
 *     }
 * }</pre>
 *
 * <p>This approach may be desirable simply because it is more explicit, or it may be
 * necessary in order to distinguish between two {@code TransactionManager} beans
 * present in the same container.  As the name suggests, the
 * {@code annotationDrivenTransactionManager()} will be the one used for processing
 * {@code @Transactional} methods. See {@link TransactionManagementConfigurer} Javadoc
 * for further details.
 *
 * <p>The {@link #mode} attribute controls how advice is applied: If the mode is
 * {@link AdviceMode#PROXY} (the default), then the other attributes control the behavior
 * of the proxying. Please note that proxy mode allows for interception of calls through
 * the proxy only; local calls within the same class cannot get intercepted that way.
 *
 * <p>Note that if the {@linkplain #mode} is set to {@link AdviceMode#ASPECTJ}, then the
 * value of the {@link #proxyTargetClass} attribute will be ignored. Note also that in
 * this case the {@code today-aspects} module JAR must be present on the classpath, with
 * compile-time weaving or load-time weaving applying the aspect to the affected classes.
 * There is no proxy involved in such a scenario; local calls will be intercepted as well.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see TransactionManagementConfigurer
 * @see TransactionManagementConfigurationSelector
 * @see ProxyTransactionManagementConfiguration
 * @see cn.taketoday.transaction.aspectj.AspectJTransactionManagementConfiguration
 * @since 4.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(TransactionManagementConfigurationSelector.class)
public @interface EnableTransactionManagement {

  /**
   * Indicate whether subclass-based (CGLIB) proxies are to be created ({@code true}) as
   * opposed to standard Java interface-based proxies ({@code false}). The default is
   * {@code false}. <strong>Applicable only if {@link #mode()} is set to
   * {@link AdviceMode#PROXY}</strong>.
   * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
   * Framework-managed beans requiring proxying, not just those marked with
   * {@code @Transactional}. For example, other beans marked with Framework's
   * {@code @Async} annotation will be upgraded to subclass proxying at the same
   * time. This approach has no negative impact in practice unless one is explicitly
   * expecting one type of proxy vs another, e.g. in tests.
   */
  boolean proxyTargetClass() default false;

  /**
   * Indicate how transactional advice should be applied.
   * <p><b>The default is {@link AdviceMode#PROXY}.</b>
   * Please note that proxy mode allows for interception of calls through the proxy
   * only. Local calls within the same class cannot get intercepted that way; an
   * {@link Transactional} annotation on such a method within a local call will be
   * ignored since Framework's interceptor does not even kick in for such a runtime
   * scenario. For a more advanced mode of interception, consider switching this to
   * {@link AdviceMode#ASPECTJ}.
   */
  AdviceMode mode() default AdviceMode.PROXY;

  /**
   * Indicate the ordering of the execution of the transaction advisor
   * when multiple advices are applied at a specific joinpoint.
   * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
   */
  int order() default Ordered.LOWEST_PRECEDENCE;

}
