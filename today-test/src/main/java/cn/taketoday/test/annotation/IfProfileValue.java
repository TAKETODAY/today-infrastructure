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

package cn.taketoday.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.junit4.AbstractJUnit4SpringContextTests;
import cn.taketoday.test.context.junit4.SpringJUnit4ClassRunner;
import cn.taketoday.test.context.junit4.statements.ProfileValueChecker;

/**
 * Test annotation for use with JUnit 4 to indicate whether a test is enabled or
 * disabled for a specific testing profile.
 *
 * <p>In the context of this annotation, the term <em>profile</em> refers to
 * a Java system property by default; however, the semantics can be changed
 * by implementing a custom {@link ProfileValueSource}. If the configured
 * {@code ProfileValueSource} returns a matching {@link #value} for the
 * declared {@link #name}, the test will be enabled. Otherwise, the test
 * will be disabled and effectively <em>ignored</em>.
 *
 * <p>{@code @IfProfileValue} can be applied at the class level, the method
 * level, or both. Class-level usage of {@code @IfProfileValue} takes
 * precedence over method-level usage for any methods within that class or
 * its subclasses. Specifically, a test is enabled if it is enabled both at
 * the class level <em>and</em> at the method level; the absence of
 * {@code @IfProfileValue} means the test is implicitly enabled. This is
 * analogous to the semantics of JUnit's {@link org.junit.Ignore @Ignore}
 * annotation, except that the presence of {@code @Ignore} always disables
 * a test.
 *
 * <h3>Example</h3>
 * When using {@link SystemProfileValueSource} as the {@code ProfileValueSource}
 * implementation (which is configured by default), you can configure a test
 * method to run only on Java VMs from Oracle as follows:
 *
 * <pre class="code">
 * &#064;IfProfileValue(name = &quot;java.vendor&quot;, value = &quot;Oracle Corporation&quot;)
 * public void testSomething() {
 *     // ...
 * }</pre>
 *
 * <h3>'OR' Semantics</h3>
 * <p>You can alternatively configure {@code @IfProfileValue} with <em>OR</em>
 * semantics for multiple {@link #values}. The following test will be enabled
 * if a {@code ProfileValueSource} has been appropriately configured for the
 * {@code "test-groups"} profile with a value of either {@code unit-tests}
 * <em>or</em> {@code integration-tests}. This functionality is similar to
 * TestNG's support for test <em>groups</em> and JUnit's experimental support
 * for test <em>categories</em>.
 *
 * <pre class="code">
 * &#064;IfProfileValue(name = &quot;test-groups&quot;, values = { &quot;unit-tests&quot;, &quot;integration-tests&quot; })
 * public void testWhichRunsForUnitOrIntegrationTestGroups() {
 *     // ...
 * }</pre>
 *
 * <h3>{@code @IfProfileValue} vs. {@code @Profile}</h3>
 * <p>Although the {@code @IfProfileValue} and
 * {@link cn.taketoday.context.annotation.Profile @Profile} annotations
 * both involve <em>profiles</em>, they are not directly related. {@code @Profile}
 * involves bean definition profiles configured in the
 * {@link cn.taketoday.core.env.Environment Environment}; whereas,
 * {@code @IfProfileValue} is used to enable or disable tests.
 *
 * <h3>Meta-annotation Support</h3>
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @see ProfileValueSource
 * @see SystemProfileValueSource
 * @see ProfileValueSourceConfiguration
 * @see ProfileValueUtils
 * @see AbstractJUnit4SpringContextTests
 * @see SpringJUnit4ClassRunner
 * @see ProfileValueChecker
 * @see cn.taketoday.context.annotation.Profile
 * @see ActiveProfiles
 * @since 2.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface IfProfileValue {

  /**
   * The {@code name} of the <em>profile value</em> against which to test.
   */
  String name();

  /**
   * A single, permissible {@code value} of the <em>profile value</em>
   * for the given {@link #name}.
   * <p>Note: Assigning values to both {@code #value} and {@link #values}
   * will lead to a configuration conflict.
   */
  String value() default "";

  /**
   * A list of all permissible {@code values} of the <em>profile value</em>
   * for the given {@link #name}.
   * <p>Note: Assigning values to both {@link #value} and {@code #values}
   * will lead to a configuration conflict.
   */
  String[] values() default {};

}
