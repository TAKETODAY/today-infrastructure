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

package infra.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation which marks a {@link java.util.Collection} or {@link java.util.Map} type
 * can be modified.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Unmodifiable
 * @since 4.0 2024/4/16 10:30
 */
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface Modifiable {

}
