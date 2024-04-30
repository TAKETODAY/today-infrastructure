/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web;

import java.util.Map;

import cn.taketoday.http.MediaType;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.lang.NullValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.util.pattern.PathMatchInfo;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Handler matching metadata
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 14:16
 */
public class HandlerMatchingMetadata {
  private final Object handler;

  // direct handler lookup path
  private final String directLookupPath;

  // handler lookup path
  private final PathContainer lookupPath;

  @Nullable
  private PathPattern bestMatchingPattern;

  @Nullable
  private PathContainer pathWithinMapping;

  @Nullable
  private PathMatchInfo pathMatchInfo;

  @Nullable
  private MediaType[] producibleMediaTypes;

  private final PathPatternParser patternParser;

  @Nullable
  private Map<String, Object> pathVariables;

  public HandlerMatchingMetadata(RequestContext request) {
    this(NullValue.INSTANCE, request);
  }

  public HandlerMatchingMetadata(Object handler, RequestContext request) {
    this.handler = handler;
    this.bestMatchingPattern = null;
    this.lookupPath = request.getRequestPath();
    this.directLookupPath = lookupPath.value();
    this.patternParser = PathPatternParser.defaultInstance;
  }

  public HandlerMatchingMetadata(Object handler, RequestContext request, PathPatternParser patternParser) {
    this.handler = handler;
    this.bestMatchingPattern = null;
    this.lookupPath = request.getRequestPath();
    this.directLookupPath = lookupPath.value();
    this.patternParser = patternParser;
  }

  public HandlerMatchingMetadata(Object handler, String directLookupPath,
          PathContainer lookupPath, PathPattern bestMatchingPattern, PathPatternParser patternParser) {
    this.handler = handler;
    this.lookupPath = lookupPath;
    this.patternParser = patternParser;
    this.directLookupPath = directLookupPath;
    this.bestMatchingPattern = bestMatchingPattern;
  }

  public HandlerMatchingMetadata(HandlerMatchingMetadata other) {
    this.handler = other.handler;
    this.lookupPath = other.lookupPath;
    this.pathVariables = other.pathVariables;
    this.pathMatchInfo = other.pathMatchInfo;
    this.patternParser = other.patternParser;
    this.directLookupPath = other.directLookupPath;
    this.pathWithinMapping = other.pathWithinMapping;
    this.bestMatchingPattern = other.bestMatchingPattern;
    this.producibleMediaTypes = other.producibleMediaTypes;
  }

  public PathMatchInfo getPathMatchInfo() {
    PathMatchInfo pathMatchInfo = this.pathMatchInfo;
    if (pathMatchInfo == null) {
      pathMatchInfo = getBestMatchingPattern().matchAndExtract(lookupPath);
      if (pathMatchInfo == null) {
        pathMatchInfo = PathMatchInfo.EMPTY;
      }
      this.pathMatchInfo = pathMatchInfo;
    }
    return pathMatchInfo;
  }

  public PathContainer getPathWithinMapping() {
    PathContainer pathWithinMapping = this.pathWithinMapping;
    if (pathWithinMapping == null) {
      PathPattern bestMatchingPattern = getBestMatchingPattern();
      if (bestMatchingPattern.hasPatternSyntax()) {
        pathWithinMapping = bestMatchingPattern.extractPathWithinPattern(lookupPath);
      }
      else {
        pathWithinMapping = lookupPath;
      }
      this.pathWithinMapping = pathWithinMapping;
    }
    return pathWithinMapping;
  }

  /**
   * This method returns PathVariables is after type conversion
   */
  public Map<String, Object> getPathVariables() {
    Map<String, Object> pathVariables = this.pathVariables;
    if (pathVariables == null) {
      pathVariables = CollectionUtils.newLinkedHashMap(getUriVariables().size());
      this.pathVariables = pathVariables;
    }
    return pathVariables;
  }

  public boolean hasPathVariables() {
    return pathVariables != null;
  }

  public Map<String, String> getUriVariables() {
    return getPathMatchInfo().getUriVariables();
  }

  @Nullable
  public String getUriVariable(String name) {
    return getPathMatchInfo().getUriVariable(name);
  }

  public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
    return getPathMatchInfo().getMatrixVariables();
  }

  public MultiValueMap<String, String> getMatrixVariable(String name) {
    return getPathMatchInfo().getMatrixVariables().get(name);
  }

  public PathPattern getBestMatchingPattern() {
    PathPattern pattern = this.bestMatchingPattern;
    if (pattern == null) {
      pattern = patternParser.parse(directLookupPath);
      this.bestMatchingPattern = pattern;
    }
    return pattern;
  }

  public PathContainer getLookupPath() {
    return lookupPath;
  }

  public String getDirectLookupPath() {
    return directLookupPath;
  }

  public void setProducibleMediaTypes(@Nullable MediaType[] producibleMediaTypes) {
    this.producibleMediaTypes = producibleMediaTypes;
  }

  // todo use List
  @Nullable
  public MediaType[] getProducibleMediaTypes() {
    return producibleMediaTypes;
  }

  public Object getHandler() {
    return handler;
  }

}
