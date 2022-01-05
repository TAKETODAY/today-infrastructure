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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.classloading.LoadTimeWeaver;
import cn.taketoday.context.weaving.DefaultContextLoadTimeWeaver;

/**
 * Activates a Framework {@link LoadTimeWeaver} for this application context, available as
 * a bean with the name "loadTimeWeaver", similar to the {@code <context:load-time-weaver>}
 * element in Framework XML.
 *
 * <p>To be used on @{@link Configuration Configuration} classes;
 * the simplest possible example of which follows:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving
 * public class AppConfig {
 *
 *     // application-specific &#064;Bean definitions ...
 * }</pre>
 *
 * The example above is equivalent to the following Framework XML configuration:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;context:load-time-weaver/&gt;
 *
 *     &lt;!-- application-specific &lt;bean&gt; definitions --&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * <h2>The {@code LoadTimeWeaverAware} interface</h2>
 * Any bean that implements the {@link
 * cn.taketoday.context.weaving.LoadTimeWeaverAware LoadTimeWeaverAware} interface
 * will then receive the {@code LoadTimeWeaver} reference automatically; for example,
 * Framework's JPA bootstrap support.
 *
 * <h2>Customizing the {@code LoadTimeWeaver}</h2>
 * The default weaver is determined automatically: see {@link DefaultContextLoadTimeWeaver}.
 *
 * <p>To customize the weaver used, the {@code @Configuration} class annotated with
 * {@code @EnableLoadTimeWeaving} may also implement the {@link LoadTimeWeavingConfigurer}
 * interface and return a custom {@code LoadTimeWeaver} instance through the
 * {@code #getLoadTimeWeaver} method:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving
 * public class AppConfig implements LoadTimeWeavingConfigurer {
 *
 *     &#064;Override
 *     public LoadTimeWeaver getLoadTimeWeaver() {
 *         MyLoadTimeWeaver ltw = new MyLoadTimeWeaver();
 *         ltw.addClassTransformer(myClassFileTransformer);
 *         // ...
 *         return ltw;
 *     }
 * }</pre>
 *
 * <p>The example above can be compared to the following Framework XML configuration:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;context:load-time-weaver weaverClass="com.acme.MyLoadTimeWeaver"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * <p>The code example differs from the XML example in that it actually instantiates the
 * {@code MyLoadTimeWeaver} type, meaning that it can also configure the instance, e.g.
 * calling the {@code #addClassTransformer} method. This demonstrates how the code-based
 * configuration approach is more flexible through direct programmatic access.
 *
 * <h2>Enabling AspectJ-based weaving</h2>
 * AspectJ load-time weaving may be enabled with the {@link #aspectjWeaving()}
 * attribute, which will cause the {@linkplain
 * org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter AspectJ class transformer} to
 * be registered through {@link LoadTimeWeaver#addTransformer}. AspectJ weaving will be
 * activated by default if a "META-INF/aop.xml" resource is present on the classpath.
 * Example:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableLoadTimeWeaving(aspectjWeaving=ENABLED)
 * public class AppConfig {
 * }</pre>
 *
 * <p>The example above can be compared to the following Framework XML configuration:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;context:load-time-weaver aspectj-weaving="on"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * @author Chris Beams
 * @see LoadTimeWeaver
 * @see DefaultContextLoadTimeWeaver
 * @see org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LoadTimeWeavingConfiguration.class)
public @interface EnableLoadTimeWeaving {

  /**
   * Whether AspectJ weaving should be enabled.
   */
  AspectJWeaving aspectjWeaving() default AspectJWeaving.AUTODETECT;

  /**
   * AspectJ weaving enablement options.
   */
  enum AspectJWeaving {

    /**
     * Switches on Framework-based AspectJ load-time weaving.
     */
    ENABLED,

    /**
     * Switches off Framework-based AspectJ load-time weaving (even if a
     * "META-INF/aop.xml" resource is present on the classpath).
     */
    DISABLED,

    /**
     * Switches on AspectJ load-time weaving if a "META-INF/aop.xml" resource
     * is present in the classpath. If there is no such resource, then AspectJ
     * load-time weaving will be switched off.
     */
    AUTODETECT;
  }

}
