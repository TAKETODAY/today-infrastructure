/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.bind;

import java.io.Serial;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.annotation.RequestMapping;

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
  @Serial
  private static final long serialVersionUID = 1L;

  private final List<String[]> paramConditions;

  private final Map<String, String[]> actualParams;

  /**
   * Create a new UnsatisfiedServletRequestParameterException.
   *
   * @param paramConditions the parameter conditions that have been violated
   * @param actualParams the actual parameter Map associated with the ServletRequest
   */
  public UnsatisfiedRequestParameterException(String[] paramConditions, Map<String, String[]> actualParams) {
    this(List.<String[]>of(paramConditions), actualParams);
  }

  /**
   * Create a new UnsatisfiedServletRequestParameterException.
   *
   * @param paramConditions all sets of parameter conditions that have been violated
   * @param actualParams the actual parameter Map associated with the ServletRequest
   */
  public UnsatisfiedRequestParameterException(List<String[]> paramConditions,
          Map<String, String[]> actualParams) {

    super("");
    Assert.notEmpty(paramConditions, "Parameter conditions must not be empty");
    this.paramConditions = paramConditions;
    this.actualParams = actualParams;
    setDetail("Invalid request parameters.");
  }

  @Override
  public String getMessage() {
    StringBuilder sb = new StringBuilder("Parameter conditions ");
    int i = 0;
    for (String[] conditions : this.paramConditions) {
      if (i > 0) {
        sb.append(" OR ");
      }
      sb.append('"');
      sb.append(StringUtils.arrayToDelimitedString(conditions, ", "));
      sb.append('"');
      i++;
    }
    sb.append(" not met for actual request parameters: ");
    sb.append(requestParameterMapToString(this.actualParams));
    return sb.toString();
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
   * Return the actual parameter Map associated with the ServletRequest.
   *
   * @see jakarta.servlet.ServletRequest#getParameterMap()
   */
  public final Map<String, String[]> getActualParams() {
    return this.actualParams;
  }

  private static String requestParameterMapToString(Map<String, String[]> actualParams) {
    StringBuilder result = new StringBuilder();
    for (Iterator<Map.Entry<String, String[]>> it = actualParams.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String, String[]> entry = it.next();
      result.append(entry.getKey()).append('=').append(ObjectUtils.nullSafeToString(entry.getValue()));
      if (it.hasNext()) {
        result.append(", ");
      }
    }
    return result.toString();
  }

}
