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
package cn.taketoday.context.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.core.env.Environment;

/**
 * {@link Conditional @Conditional} that checks if the specified properties have a
 * specific value. By default the properties must be present in the {@link Environment}
 * and <strong>not</strong> equal to {@code false}. The {@link #havingValue()} and
 * {@link #matchIfMissing()} attributes allow further customizations.
 * <p>
 * The {@link #havingValue} attribute can be used to specify the value that the property
 * should have. The table below shows when a condition matches according to the property
 * value and the {@link #havingValue()} attribute:
 *
 * <table border="1">
 * <caption>Having values</caption>
 * <tr>
 * <th>Property Value</th>
 * <th>{@code havingValue=""}</th>
 * <th>{@code havingValue="true"}</th>
 * <th>{@code havingValue="false"}</th>
 * <th>{@code havingValue="foo"}</th>
 * </tr>
 * <tr>
 * <td>{@code "true"}</td>
 * <td>yes</td>
 * <td>yes</td>
 * <td>no</td>
 * <td>no</td>
 * </tr>
 * <tr>
 * <td>{@code "false"}</td>
 * <td>no</td>
 * <td>no</td>
 * <td>yes</td>
 * <td>no</td>
 * </tr>
 * <tr>
 * <td>{@code "foo"}</td>
 * <td>yes</td>
 * <td>no</td>
 * <td>no</td>
 * <td>yes</td>
 * </tr>
 * </table>
 * <p>
 * If the property is not contained in the {@link Environment} at all, the
 * {@link #matchIfMissing()} attribute is consulted. By default missing attributes do not
 * match.
 * <p>
 * This condition cannot be reliably used for matching collection properties. For example,
 * in the following configuration, the condition matches if {@code today.example.values}
 * is present in the {@link Environment} but does not match if
 * {@code today.example.values[0]} is present.
 *
 * <pre class="code">
 * &#064;ConditionalOnProperty(prefix = "today", name = "example.values")
 * class ExampleAutoConfiguration {
 * }
 * </pre>
 *
 * It is better to use a custom condition for such cases.
 *
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author TODAY <br>
 * 2019-06-18 15:06
 */
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnPropertyCondition.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnProperty {

  /**
   * Alias for {@link #name()}.
   *
   * @return the names
   */
  String[] value() default {};

  /**
   * A prefix that should be applied to each property. The prefix automatically ends
   * with a dot if not specified. A valid prefix is defined by one or more words
   * separated with dots (e.g. {@code "acme.system.feature"}).
   *
   * @return the prefix
   */
  String prefix() default "";

  /**
   * The name of the properties to test. If a prefix has been defined, it is applied to
   * compute the full key of each property. For instance if the prefix is
   * {@code app.config} and one value is {@code my-value}, the full key would be
   * {@code app.config.my-value}
   * <p>
   * Use the dashed notation to specify each property, that is all lower case with a "-"
   * to separate words (e.g. {@code my-long-property}).
   *
   * @return the names
   */
  String[] name() default {};

  /**
   * The string representation of the expected value for the properties. If not
   * specified, the property must <strong>not</strong> be equal to {@code false}.
   *
   * @return the expected value
   */
  String havingValue() default "";

  /**
   * Specify if the condition should match if the property is not set. Defaults to
   * {@code false}.
   *
   * @return if the condition should match if the property is missing
   */
  boolean matchIfMissing() default false;

}
