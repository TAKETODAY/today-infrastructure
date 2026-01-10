/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.lang.Constant;

/**
 * This annotation may be used to annotate method parameters on request mappings
 * where a URI-template has been used in the path-mapping of the {@link RequestMapping}
 * annotation. The method parameter may be of type String, any Java primitive
 * type or any boxed version thereof.
 *
 * <p>For example:-
 * <pre>{@code
 * @RequestMapping("/bookings/{guest-id}")
 * public class BookingController {
 *
 *     @RequestMapping
 *     public void processBookingRequest(@PathVariable("guest-id") String guestID) {
 *         // process booking from the given guest here
 *     }
 * }
 * }</pre>
 *
 * <p>For example:-
 * <pre>{@code
 * @RequestMapping("/rewards/{vip-level}")
 * public class RewardController {
 *
 *     @RequestMapping
 *     public void processReward(@PathVariable("vip-level") Integer vipLevel) {
 *         // process reward here
 *     }
 * }
 * }</pre>
 *
 * @author TODAY 2018-06-29 16:27:12
 */
@Documented
@RequestParam
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable {

  /**
   * Alias for {@link #name}.
   */
  @AliasFor(annotation = RequestParam.class, attribute = "value")
  String value() default Constant.BLANK;

  /**
   * The name of the path variable to bind to.
   *
   * @since 4.0
   */
  @AliasFor(annotation = RequestParam.class, attribute = "name")
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
  @AliasFor(annotation = RequestParam.class, attribute = "required")
  boolean required() default true;

}
