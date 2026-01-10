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

package infra.http.converter.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

import infra.http.ProblemDetail;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * An interface to associate Jackson annotations with
 * {@link ProblemDetail} to avoid a hard dependency on
 * the Jackson library.
 *
 * <p>The annotations ensure the {@link ProblemDetail#getProperties() properties}
 * map is unwrapped and rendered as top level JSON properties, and likewise that
 * the {@code properties} map contains unknown properties from the JSON.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/14 9:58
 */
@JsonInclude(NON_EMPTY)
public interface ProblemDetailJacksonMixin {

  @JsonAnySetter
  void setProperty(String name, Object value);

  @JsonAnyGetter
  Map<String, Object> getProperties();

}
