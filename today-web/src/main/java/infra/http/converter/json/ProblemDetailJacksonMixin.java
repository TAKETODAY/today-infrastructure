/*
 * Copyright 2017 - 2026 the original author or authors.
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
