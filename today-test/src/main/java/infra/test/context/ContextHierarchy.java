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

package infra.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ContextHierarchy} is a class-level annotation that is used to define
 * a hierarchy of {@link infra.context.ApplicationContext
 * ApplicationContexts} for integration tests.
 *
 * <h3>Examples</h3>
 * <p>The following JUnit-based examples demonstrate common configuration
 * scenarios for integration tests that require the use of context hierarchies.
 *
 * <h4>Single Test Class with Context Hierarchy</h4>
 * <p>{@code ControllerIntegrationTests} represents a typical integration testing
 * scenario for a MVC web application by declaring a context hierarchy
 * consisting of two levels, one for the <em>root</em> {@code WebApplicationContext}
 * (with {@code TestAppConfig}) and one for the <em>dispatcher servlet</em>
 * {@code WebApplicationContext} (with {@code WebConfig}). The {@code
 * WebApplicationContext} that is <em>autowired</em> into the test instance is
 * the one for the child context (i.e., the lowest context in the hierarchy).
 *
 * <pre class="code">
 * &#064;RunWith(Runner.class)
 * &#064;WebAppConfiguration
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(classes = TestAppConfig.class),
 *     &#064;ContextConfiguration(classes = WebConfig.class)
 * })
 * public class ControllerIntegrationTests {
 *
 *     &#064;Autowired
 *     private WebApplicationContext wac;
 *
 *     // ...
 * }</pre>
 *
 * <h4>Class Hierarchy with Implicit Parent Context</h4>
 * <p>The following test classes define a context hierarchy within a test class
 * hierarchy. {@code AbstractWebTests} declares the configuration for a root
 * {@code WebApplicationContext} in a Infra-powered web application. Note,
 * however, that {@code AbstractWebTests} does not declare {@code @ContextHierarchy};
 * consequently, subclasses of {@code AbstractWebTests} can optionally participate
 * in a context hierarchy or follow the standard semantics for {@code @ContextConfiguration}.
 * {@code SoapWebServiceTests} and {@code RestWebServiceTests} both extend
 * {@code AbstractWebTests} and define a context hierarchy via {@code @ContextHierarchy}.
 * The result is that three application contexts will be loaded (one for each
 * declaration of {@code @ContextConfiguration}, and the application context
 * loaded based on the configuration in {@code AbstractWebTests} will be set as
 * the parent context for each of the contexts loaded for the concrete subclasses.
 *
 * <pre class="code">
 * &#064;RunWith(Runner.class)
 * &#064;WebAppConfiguration
 * &#064;ContextConfiguration("file:src/main/webapp/WEB-INF/applicationContext.xml")
 * public abstract class AbstractWebTests {}
 *
 * &#064;ContextHierarchy(&#064;ContextConfiguration("/today/soap-ws-config.xml"))
 * public class SoapWebServiceTests extends AbstractWebTests {}
 *
 * &#064;ContextHierarchy(&#064;ContextConfiguration("/today/rest-ws-config.xml"))
 * public class RestWebServiceTests extends AbstractWebTests {}</pre>
 *
 * <h4>Class Hierarchy with Merged Context Hierarchy Configuration</h4>
 * <p>The following classes demonstrate the use of <em>named</em> hierarchy levels
 * in order to <em>merge</em> the configuration for specific levels in a context
 * hierarchy. {@code BaseTests} defines two levels in the hierarchy, {@code parent}
 * and {@code child}. {@code ExtendedTests} extends {@code BaseTests} and instructs
 * the TestContext Framework to merge the context configuration for the
 * {@code child} hierarchy level, simply by ensuring that the names declared via
 * {@link ContextConfiguration#name} are both {@code "child"}. The result is that
 * three application contexts will be loaded: one for {@code "/app-config.xml"},
 * one for {@code "/user-config.xml"}, and one for <code>{"/user-config.xml",
 * "/order-config.xml"}</code>. As with the previous example, the application
 * context loaded from {@code "/app-config.xml"} will be set as the parent context
 * for the contexts loaded from {@code "/user-config.xml"} and <code>{"/user-config.xml",
 * "/order-config.xml"}</code>.
 *
 * <pre class="code">
 * &#064;RunWith(Runner.class)
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(name = "parent", locations = "/app-config.xml"),
 *     &#064;ContextConfiguration(name = "child",  locations = "/user-config.xml")
 * })
 * public class BaseTests {}
 *
 * &#064;ContextHierarchy(
 *     &#064;ContextConfiguration(name = "child",  locations = "/order-config.xml")
 * )
 * public class ExtendedTests extends BaseTests {}</pre>
 *
 * <h4>Class Hierarchy with Overridden Context Hierarchy Configuration</h4>
 * <p>In contrast to the previous example, this example demonstrates how to
 * <em>override</em> the configuration for a given named level in a context hierarchy
 * by setting the {@link ContextConfiguration#inheritLocations} flag to {@code false}.
 * Consequently, the application context for {@code ExtendedTests} will be loaded
 * only from {@code "/test-user-config.xml"} and will have its parent set to the
 * context loaded from {@code "/app-config.xml"}.
 *
 * <pre class="code">
 * &#064;RunWith(Runner.class)
 * &#064;ContextHierarchy({
 *     &#064;ContextConfiguration(name = "parent", locations = "/app-config.xml"),
 *     &#064;ContextConfiguration(name = "child",  locations = "/user-config.xml")
 * })
 * public class BaseTests {}
 *
 * &#064;ContextHierarchy(
 *     &#064;ContextConfiguration(name = "child",  locations = "/test-user-config.xml", inheritLocations = false)
 * )
 * public class ExtendedTests extends BaseTests {}</pre>
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p> this annotation will be inherited from an
 * enclosing test class by default. See
 * {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * @author Sam Brannen
 * @see ContextConfiguration
 * @see infra.context.ApplicationContext
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ContextHierarchy {

  /**
   * A list of {@link ContextConfiguration @ContextConfiguration} instances,
   * each of which defines a level in the context hierarchy.
   * <p>If you need to merge or override the configuration for a given level
   * of the context hierarchy within a test class hierarchy, you must explicitly
   * name that level by supplying the same value to the {@link ContextConfiguration#name
   * name} attribute in {@code @ContextConfiguration} at each level in the
   * class hierarchy. See the class-level Javadoc for examples.
   */
  ContextConfiguration[] value();

}
