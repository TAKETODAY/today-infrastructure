/*
 * Copyright 2017 - 2025 the original author or authors.
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
