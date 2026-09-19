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

package infra.persistence.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;

/**
 * Trims the string value of the annotated property before it is turned into a
 * query condition.
 *
 * <p>This is a general behaviour modifier that can be combined with any query
 * condition annotation such as {@link Where @Where} or {@link Like @Like}. The
 * value is trimmed with {@link String#trim()} before the corresponding strategy
 * renders the predicate.
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 *   @Where("name like ?")
 *   @Trim
 *   private String name;
 *
 *   @Like
 *   @Trim
 *   private String nickname;
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see String#trim()
 * @see Where
 * @see Like
 * @since 5.0
 */
@Reflective
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Trim {

}
