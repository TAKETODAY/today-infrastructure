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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.lang.Constant;

/**
 * This annotation may be used to annotate method parameters on request mappings
 * where a URI-template has been used in the path-mapping of the {@link ActionMapping}
 * annotation. The method parameter may be of type String, any Java primitive
 * type or any boxed version thereof.
 *
 * <p>For example:-
 * <pre><code>
 * &#64;RequestMapping("/bookings/{guest-id}")
 * public class BookingController {
 *
 *     &#64;RequestMapping
 *     public void processBookingRequest(@PathVariable("guest-id") String guestID) {
 *         // process booking from the given guest here
 *     }
 * }
 * </code></pre>
 *
 * <p>For example:-
 * <pre><code>
 * &#64;RequestMapping("/rewards/{vip-level}")
 * public class RewardController {
 *
 *     &#64;RequestMapping
 *     public void processReward(@PathVariable("vip-level") Integer vipLevel) {
 *         // process reward here
 *     }
 * }
 * </code></pre>
 *
 * @author TODAY 2018-06-29 16:27:12
 */
@RequestParam
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor("name")
  String value() default Constant.BLANK;

  /**
   * The name of the path variable to bind to.
   *
   * @since 4.0
   */
  @AliasFor("value")
  String name() default Constant.BLANK;

  /**
   * Whether the path variable is required.
   * <p>Defaults to {@code true}, leading to an exception being thrown if the path
   * variable is missing in the incoming request. Switch this to {@code false} if
   * you prefer a {@code null} or Java 8 {@code java.util.Optional} in this case.
   * e.g. on a {@code ModelAttribute} method which serves for different requests.
   *
   * @since 4.0
   */
  boolean required() default true;

}
