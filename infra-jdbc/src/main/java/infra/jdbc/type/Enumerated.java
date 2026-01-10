/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jdbc.type;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that a persistent property or field should be persisted
 * as a enumerated type.  If the enumerated type
 * is not specified or the <code>Enumerated</code> annotation is not
 * used, the {@code EnumType} value is assumed to be {@code EnumType.ORDINAL}.
 *
 * <p> Example:
 * <pre>{@code
 *
 *   public enum EmployeeStatus {FULL_TIME, PART_TIME, CONTRACT}
 *
 *   public enum SalaryRate {JUNIOR, SENIOR, MANAGER, EXECUTIVE}
 *
 *   public class Employee {
 *       public EmployeeStatus getStatus() {...}
 *       ...
 *       @Enumerated(NAME)
 *       public SalaryRate getPayScale() {...}
 *       ...
 *   }
 * } </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
public @interface Enumerated {

  /** (Optional) The type used in mapping an enum type. */
  EnumType value() default EnumType.ORDINAL;

}
