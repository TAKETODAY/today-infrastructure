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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.persistence.GenerationType;

/**
 * Declares a UUID primary key generated before insertion.
 *
 * <p>Equivalent to {@code @GeneratedId(strategy = GenerationType.UUID)} and
 * includes the primary-key mapping; an additional {@link Id} is not required.
 * Supports {@link java.util.UUID} and String properties. The configured UUID
 * generator defaults to UUID version 7. Generation runs after pre-persist
 * callbacks and preserves existing non-null IDs.
 *
 * <p>May also be used as a meta-annotation for application-specific UUID IDs.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see GeneratedId
 * @since 5.0
 */
@GeneratedId(strategy = GenerationType.UUID)
@Documented
@Target({ ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratedUuid {

}
