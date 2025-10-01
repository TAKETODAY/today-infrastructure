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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.InvalidMediaTypeException;
import infra.http.MediaType;
import infra.util.CollectionUtils;
import infra.util.StringUtils;
import infra.web.HandlerMatchingMetadata;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.HttpMediaTypeNotSupportedException;
import infra.web.HttpRequestMethodNotSupportedException;
import infra.web.RequestContext;
import infra.web.bind.UnsatisfiedRequestParameterException;
import infra.web.handler.condition.NameValueExpression;
import infra.web.handler.condition.PathPatternsRequestCondition;

/**
 * Abstract base class for classes for which {@link RequestMappingInfo} defines
 * the mapping between a request and a handler method.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/1 22:33
 */
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

  private static final Method HTTP_OPTIONS_HANDLE_METHOD;

  static {
    try {
      HTTP_OPTIONS_HANDLE_METHOD = HttpOptionsHandler.class.getMethod("handle");
    }
    catch (NoSuchMethodException ex) {
      // Should never happen
      throw new IllegalStateException("Failed to retrieve internal handler method for HTTP OPTIONS", ex);
    }
  }

  protected RequestMappingInfoHandlerMapping() {
    setHandlerMethodMappingNamingStrategy(new RequestMappingInfoHandlerMethodMappingNamingStrategy());
  }

  @Override
  protected Set<String> getDirectPaths(RequestMappingInfo info) {
    return info.getDirectPaths();
  }

  /**
   * Check if the given RequestMappingInfo matches the current request and
   * return a (potentially new) instance with conditions that match the
   * current request -- for example with a subset of URL patterns.
   *
   * @return an info in case of a match; or {@code null} otherwise.
   */
  @Nullable
  @Override
  protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, RequestContext request) {
    return info.getMatchingCondition(request);
  }

  /**
   * Provide a Comparator to sort RequestMappingInfos matched to a request.
   */
  @Override
  protected Comparator<RequestMappingInfo> getMappingComparator(final RequestContext request) {
    return (info1, info2) -> info1.compareTo(info2, request);
  }

  /**
   * Expose URI template variables, matrix variables, and producible media types in the request.
   */
  @Override
  protected void handleMatch(Match<RequestMappingInfo> bestMatch, String directLookupPath, RequestContext request) {
    RequestMappingInfo info = bestMatch.mapping;
    info.getVersionCondition().handleMatch(request);

    PathPatternsRequestCondition pathPatternsCondition = info.getPathPatternsCondition();
    HandlerMatchingMetadata matchingMetadata = new HandlerMatchingMetadata(
            bestMatch.getHandlerMethod(), directLookupPath, request.getRequestPath(),
            CollectionUtils.firstElement(pathPatternsCondition.getPatterns()), getPatternParser());

    request.setMatchingMetadata(matchingMetadata);

    Collection<MediaType> mediaTypes = info.getProducesCondition().getProducibleMediaTypes();
    if (!mediaTypes.isEmpty()) {
      matchingMetadata.setProducibleMediaTypes(mediaTypes);
    }
  }

  /**
   * Iterate all RequestMappingInfo's once again, look if any match by URL at
   * least and raise exceptions according to what doesn't match.
   *
   * @throws HttpRequestMethodNotSupportedException if there are matches by URL
   * but not by HTTP method
   * @throws HttpMediaTypeNotAcceptableException if there are matches by URL
   * but not by consumable/producible media types
   */
  @Nullable
  @Override
  protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> infos, String lookupPath, RequestContext request) {
    if (CollectionUtils.isEmpty(infos)) {
      return null;
    }

    PartialMatchHelper helper = new PartialMatchHelper(infos, request);
    if (helper.isEmpty()) {
      return null;
    }

    if (helper.hasMethodsMismatch()) {
      Set<String> methods = helper.getAllowedMethods();
      if (HttpMethod.OPTIONS == request.getMethod()) {
        Set<MediaType> mediaTypes = helper.getConsumablePatchMediaTypes();
        HttpOptionsHandler handler = new HttpOptionsHandler(methods, mediaTypes);
        return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
      }
      throw new HttpRequestMethodNotSupportedException(request.getMethodValue(), methods);
    }

    if (helper.hasConsumesMismatch()) {
      Set<MediaType> mediaTypes = helper.getConsumableMediaTypes();
      MediaType contentType = null;
      if (StringUtils.isNotEmpty(request.getContentType())) {
        try {
          contentType = MediaType.parseMediaType(request.getContentType());
        }
        catch (InvalidMediaTypeException ex) {
          throw new HttpMediaTypeNotSupportedException(ex.getMessage(), mediaTypes);
        }
      }
      throw new HttpMediaTypeNotSupportedException(contentType, mediaTypes, request.getMethod());
    }

    if (helper.hasProducesMismatch()) {
      Set<MediaType> mediaTypes = helper.getProducibleMediaTypes();
      throw new HttpMediaTypeNotAcceptableException(new ArrayList<>(mediaTypes));
    }

    if (helper.hasParamsMismatch()) {
      List<String[]> conditions = helper.getParamConditions();
      throw new UnsatisfiedRequestParameterException(conditions, request.getParameters());
    }

    return null;
  }

  /**
   * Aggregate all partial matches and expose methods checking across them.
   */
  private static class PartialMatchHelper {

    private final ArrayList<PartialMatch> partialMatches;

    public PartialMatchHelper(Set<RequestMappingInfo> infos, RequestContext request) {
      this.partialMatches = new ArrayList<>(infos.size());
      for (RequestMappingInfo info : infos) {
        if (info.getPathPatternsCondition().getMatchingCondition(request) != null) {
          partialMatches.add(new PartialMatch(info, request));
        }
      }
    }

    /**
     * Whether there any partial matches.
     */
    public boolean isEmpty() {
      return this.partialMatches.isEmpty();
    }

    /**
     * Any partial matches for "methods"?
     */
    public boolean hasMethodsMismatch() {
      for (PartialMatch match : this.partialMatches) {
        if (match.methodsMatch) {
          return false;
        }
      }
      return true;
    }

    /**
     * Any partial matches for "methods" and "consumes"?
     */
    public boolean hasConsumesMismatch() {
      for (PartialMatch match : this.partialMatches) {
        if (match.hasConsumesMatch()) {
          return false;
        }
      }
      return true;
    }

    /**
     * Any partial matches for "methods", "consumes", and "produces"?
     */
    public boolean hasProducesMismatch() {
      for (PartialMatch match : this.partialMatches) {
        if (match.hasProducesMatch()) {
          return false;
        }
      }
      return true;
    }

    /**
     * Any partial matches for "methods", "consumes", "produces", and "params"?
     */
    public boolean hasParamsMismatch() {
      for (PartialMatch match : this.partialMatches) {
        if (match.hasParamsMatch()) {
          return false;
        }
      }
      return true;
    }

    /**
     * Return declared HTTP methods.
     */
    public Set<String> getAllowedMethods() {
      LinkedHashSet<String> result = new LinkedHashSet<>();
      for (PartialMatch match : this.partialMatches) {
        for (HttpMethod method : match.info.getMethodsCondition().getMethods()) {
          result.add(method.name());
        }
      }
      return result;
    }

    /**
     * Return declared "consumable" types but only among those that also
     * match the "methods" condition.
     */
    public Set<MediaType> getConsumableMediaTypes() {
      LinkedHashSet<MediaType> result = new LinkedHashSet<>();
      for (PartialMatch match : this.partialMatches) {
        if (match.methodsMatch) {
          result.addAll(match.info.getConsumesCondition().getConsumableMediaTypes());
        }
      }
      return result;
    }

    /**
     * Return declared "producible" types but only among those that also
     * match the "methods" and "consumes" conditions.
     */
    public Set<MediaType> getProducibleMediaTypes() {
      LinkedHashSet<MediaType> result = new LinkedHashSet<>();
      for (PartialMatch match : this.partialMatches) {
        if (match.hasConsumesMatch()) {
          CollectionUtils.addAll(result, match.info.getProducesCondition().getProducibleMediaTypes());
        }
      }
      return result;
    }

    /**
     * Return declared "params" conditions but only among those that also
     * match the "methods", "consumes", and "params" conditions.
     */
    public List<String[]> getParamConditions() {
      ArrayList<String[]> result = new ArrayList<>();
      for (PartialMatch match : partialMatches) {
        if (match.hasProducesMatch()) {
          Set<NameValueExpression> set = match.info.getParamsCondition().getExpressions();
          if (CollectionUtils.isNotEmpty(set)) {
            int i = 0;
            String[] array = new String[set.size()];
            for (NameValueExpression expression : set) {
              array[i++] = expression.toString();
            }
            result.add(array);
          }
        }
      }
      return result;
    }

    /**
     * Return declared "consumable" types but only among those that have
     * PATCH specified, or that have no methods at all.
     */
    public Set<MediaType> getConsumablePatchMediaTypes() {
      LinkedHashSet<MediaType> result = new LinkedHashSet<>();
      for (PartialMatch match : this.partialMatches) {
        Set<HttpMethod> methods = match.info.getMethodsCondition().getMethods();
        if (methods.isEmpty() || methods.contains(HttpMethod.PATCH)) {
          result.addAll(match.info.getConsumesCondition().getConsumableMediaTypes());
        }
      }
      return result;
    }

    /**
     * Container for a RequestMappingInfo that matches the URL path at least.
     */
    private static class PartialMatch {

      public final RequestMappingInfo info;

      public final boolean methodsMatch;

      private final boolean consumesMatch;

      private final boolean producesMatch;

      private final boolean paramsMatch;

      /**
       * Create a new {@link PartialMatch} instance.
       *
       * @param info the RequestMappingInfo that matches the URL path.
       * @param request the current request
       */
      public PartialMatch(RequestMappingInfo info, RequestContext request) {
        this.info = info;
        this.methodsMatch = info.getMethodsCondition().getMatchingCondition(request) != null;
        this.consumesMatch = info.getConsumesCondition().getMatchingCondition(request) != null;
        this.producesMatch = info.getProducesCondition().getMatchingCondition(request) != null;
        this.paramsMatch = info.getParamsCondition().getMatchingCondition(request) != null;
      }

      public boolean hasConsumesMatch() {
        return methodsMatch && this.consumesMatch;
      }

      public boolean hasProducesMatch() {
        return hasConsumesMatch() && this.producesMatch;
      }

      public boolean hasParamsMatch() {
        return hasProducesMatch() && this.paramsMatch;
      }

      @Override
      public String toString() {
        return this.info.toString();
      }
    }
  }

  /**
   * Default handler for HTTP OPTIONS.
   */
  private static class HttpOptionsHandler {

    private final HttpHeaders headers = HttpHeaders.forWritable();

    public HttpOptionsHandler(Set<String> declaredMethods, Set<MediaType> acceptPatch) {
      this.headers.setAllow(initAllowedHttpMethods(declaredMethods));
      this.headers.setAcceptPatch(acceptPatch);
    }

    private static Set<HttpMethod> initAllowedHttpMethods(Set<String> declaredMethods) {
      var result = CollectionUtils.<HttpMethod>newLinkedHashSet(declaredMethods.size());
      if (declaredMethods.isEmpty()) {
        for (HttpMethod method : HttpMethod.values()) {
          if (method != HttpMethod.TRACE) {
            result.add(method);
          }
        }
      }
      else {
        for (String method : declaredMethods) {
          HttpMethod httpMethod = HttpMethod.valueOf(method);
          result.add(httpMethod);
          if (httpMethod == HttpMethod.GET) {
            result.add(HttpMethod.HEAD);
          }
        }
        result.add(HttpMethod.OPTIONS);
      }
      return result;
    }

    @SuppressWarnings("unused")
    public HttpHeaders handle() {
      return this.headers;
    }
  }

}
