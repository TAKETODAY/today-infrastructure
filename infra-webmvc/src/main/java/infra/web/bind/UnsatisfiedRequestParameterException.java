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

package infra.web.bind;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import infra.lang.Assert;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.annotation.RequestMapping;

/**
 * {@link RequestBindingException} subclass that indicates an unsatisfied
 * parameter condition, as typically expressed using an {@code @RequestMapping}
 * annotation at the {@code @Controller} type level.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestMapping#params()
 * @since 4.0 2022/3/2 16:23
 */
public class UnsatisfiedRequestParameterException extends RequestBindingException {

  private final List<String[]> paramConditions;

  private final MultiValueMap<String, String> actualParams;

  /**
   * Create a new UnsatisfiedRequestParameterException.
   *
   * @param paramConditions the parameter conditions that have been violated
   * @param actualParams the actual parameter Map associated with the Request
   */
  public UnsatisfiedRequestParameterException(String[] paramConditions, MultiValueMap<String, String> actualParams) {
    this(List.<String[]>of(paramConditions), actualParams);
  }

  /**
   * Create a new UnsatisfiedRequestParameterException.
   *
   * @param paramConditions all sets of parameter conditions that have been violated
   * @param actualParams the actual parameter Map associated with the Request
   */
  public UnsatisfiedRequestParameterException(List<String[]> paramConditions, MultiValueMap<String, String> actualParams) {
    super("", null, new Object[] { paramsToStringList(paramConditions) });
    Assert.notEmpty(paramConditions, "Parameter conditions must not be empty");
    this.actualParams = actualParams;
    this.paramConditions = paramConditions;
    getBody().setDetail("Invalid request parameters.");
  }

  private static List<String> paramsToStringList(List<String[]> paramConditions) {
    Assert.notEmpty(paramConditions, "Parameter conditions must not be empty");
    return paramConditions.stream()
            .map(condition -> "\"" + StringUtils.arrayToDelimitedString(condition, ", ") + "\"")
            .collect(Collectors.toList());
  }

  @Override
  public String getMessage() {
    return "Parameter conditions %s not met for actual request parameters: %s"
            .formatted(String.join(" OR ", paramsToStringList(this.paramConditions)), requestParameterMapToString(this.actualParams));
  }

  /**
   * Return the parameter conditions that have been violated or the first group
   * in case of multiple groups.
   *
   * @see RequestMapping#params()
   */
  public final String[] getParamConditions() {
    return this.paramConditions.get(0);
  }

  /**
   * Return all parameter condition groups that have been violated.
   *
   * @see RequestMapping#params()
   */
  public final List<String[]> getParamConditionGroups() {
    return this.paramConditions;
  }

  /**
   * Return the actual parameter Map associated with the Request.
   *
   * @see RequestContext#getParameters()
   */
  public final MultiValueMap<String, String> getActualParams() {
    return this.actualParams;
  }

  private static String requestParameterMapToString(MultiValueMap<String, String> actualParams) {
    StringBuilder result = new StringBuilder();
    for (Iterator<Map.Entry<String, List<String>>> it = actualParams.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String, List<String>> entry = it.next();
      result.append(entry.getKey()).append('=').append(ObjectUtils.nullSafeToString(entry.getValue()));
      if (it.hasNext()) {
        result.append(", ");
      }
    }
    return result.toString();
  }

}
