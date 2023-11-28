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

package cn.taketoday.web.handler.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A logical disjunction (' || ') request condition that matches a request
 * against a set of {@link HttpMethod HttpMethods}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class RequestMethodsRequestCondition extends AbstractRequestCondition<RequestMethodsRequestCondition> {

  /** Per HTTP method cache to return ready instances from getMatchingCondition. */
  private static final EnumMap<HttpMethod, RequestMethodsRequestCondition> requestMethodConditionCache
          = new EnumMap<>(HttpMethod.class);

  static {
    for (HttpMethod method : HttpMethod.values()) {
      requestMethodConditionCache.put(method, new RequestMethodsRequestCondition(method));
    }
  }

  private final Set<HttpMethod> methods;

  /**
   * Create a new instance with the given request methods.
   *
   * @param requestMethods 0 or more HTTP request methods;
   * if, 0 the condition will match to every request
   */
  public RequestMethodsRequestCondition(HttpMethod... requestMethods) {
    this.methods = (ObjectUtils.isEmpty(requestMethods) ?
                    Collections.emptySet() : new LinkedHashSet<>(Arrays.asList(requestMethods)));
  }

  /**
   * Private constructor for internal use when combining conditions.
   */
  private RequestMethodsRequestCondition(Set<HttpMethod> methods) {
    this.methods = methods;
  }

  /**
   * Returns all {@link HttpMethod RequestMethods} contained in this condition.
   */
  public Set<HttpMethod> getMethods() {
    return this.methods;
  }

  @Override
  protected Collection<HttpMethod> getContent() {
    return this.methods;
  }

  @Override
  protected String getToStringInfix() {
    return " || ";
  }

  /**
   * Returns a new instance with a union of the HTTP request methods
   * from "this" and the "other" instance.
   */
  @Override
  public RequestMethodsRequestCondition combine(RequestMethodsRequestCondition other) {
    if (isEmpty() && other.isEmpty()) {
      return this;
    }
    else if (other.isEmpty()) {
      return this;
    }
    else if (isEmpty()) {
      return other;
    }
    LinkedHashSet<HttpMethod> set = new LinkedHashSet<>(this.methods);
    set.addAll(other.methods);
    return new RequestMethodsRequestCondition(set);
  }

  /**
   * Check if any of the HTTP request methods match the given request and
   * return an instance that contains the matching HTTP request method only.
   *
   * @param request the current request
   * @return the same instance if the condition is empty (unless the request
   * method is HTTP OPTIONS), a new condition with the matched request method,
   * or {@code null} if there is no match or the condition is empty and the
   * request method is OPTIONS.
   */
  @Override
  @Nullable
  public RequestMethodsRequestCondition getMatchingCondition(RequestContext request) {
    if (request.isPreFlightRequest()) {
      return matchPreFlight(request);
    }

    if (ServletDetector.runningInServlet(request)) {
      if (getMethods().isEmpty()) {
        if (HttpMethod.OPTIONS == request.getMethod()) {
          HttpServletRequest servletRequest = ServletUtils.getServletRequest(request);
          if (!DispatcherType.ERROR.equals(servletRequest.getDispatcherType())) {
            return null; // We handle OPTIONS transparently, so don't match if no explicit declarations
          }
        }
        return this;
      }
    }
    else {
      if (getMethods().isEmpty()) {
        if (HttpMethod.OPTIONS == request.getMethod()) {
          return null; // We handle OPTIONS transparently, so don't match if no explicit declarations
        }
        return this;
      }
    }

    return matchRequestMethod(request.getMethod());
  }

  /**
   * On a pre-flight request match to the would-be, actual request.
   * Hence empty conditions is a match, otherwise try to match to the HTTP
   * method in the "Access-Control-Request-Method" header.
   */
  @Nullable
  private RequestMethodsRequestCondition matchPreFlight(RequestContext request) {
    if (getMethods().isEmpty()) {
      return this;
    }
    HttpHeaders headers = request.getHeaders();
    HttpMethod expectedMethod = headers.getAccessControlRequestMethod();
//     TODO expectedMethod maybe null
    Assert.state(expectedMethod != null, "No Access-Control-Request-Method Header");
    return matchRequestMethod(expectedMethod);
  }

  @Nullable
  private RequestMethodsRequestCondition matchRequestMethod(HttpMethod requestMethod) {
    try {
      if (getMethods().contains(requestMethod)) {
        return requestMethodConditionCache.get(requestMethod);
      }
      if (requestMethod.equals(HttpMethod.HEAD) && getMethods().contains(HttpMethod.GET)) {
        return requestMethodConditionCache.get(HttpMethod.GET);
      }
    }
    catch (IllegalArgumentException ex) {
      // Custom request method
    }
    return null;
  }

  /**
   * Returns:
   * <ul>
   * <li>0 if the two conditions contain the same number of HTTP request methods
   * <li>Less than 0 if "this" instance has an HTTP request method but "other" doesn't
   * <li>Greater than 0 "other" has an HTTP request method but "this" doesn't
   * </ul>
   * <p>It is assumed that both instances have been obtained via
   * {@link #getMatchingCondition(RequestContext)} and therefore each instance
   * contains the matching HTTP request method only or is otherwise empty.
   */
  @Override
  public int compareTo(RequestMethodsRequestCondition other, RequestContext request) {
    if (other.methods.size() != this.methods.size()) {
      return other.methods.size() - this.methods.size();
    }
    else if (this.methods.size() == 1) {
      if (this.methods.contains(HttpMethod.HEAD) && other.methods.contains(HttpMethod.GET)) {
        return -1;
      }
      else if (this.methods.contains(HttpMethod.GET) && other.methods.contains(HttpMethod.HEAD)) {
        return 1;
      }
    }
    return 0;
  }

}
