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

package cn.taketoday.web;

import java.util.Map;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.lang.NullValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.util.pattern.PathMatchInfo;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 14:16
 */
public class HandlerMatchingMetadata {
  private final Object handler;

  // direct handler lookup path
  private final String directLookupPath;

  // handler lookup path
  private final PathContainer lookupPath;

  private PathPattern bestMatchingPattern;

  private PathContainer pathWithinMapping;

  private PathMatchInfo pathMatchInfo;

  @Nullable
  private MediaType[] producibleMediaTypes;

  private final PathPatternParser patternParser;

  public HandlerMatchingMetadata(RequestContext request) {
    this(NullValue.INSTANCE, request);
  }

  public HandlerMatchingMetadata(Object handler, RequestContext request) {
    this.handler = handler;
    this.bestMatchingPattern = null;
    this.lookupPath = request.getLookupPath();
    this.directLookupPath = lookupPath.value();
    this.patternParser = PathPatternParser.defaultInstance;
  }

  public HandlerMatchingMetadata(
          Object handler, String directLookupPath, PathContainer lookupPath,
          PathPattern bestMatchingPattern, PathPatternParser patternParser) {
    this.handler = handler;
    this.lookupPath = lookupPath;
    this.patternParser = patternParser;
    this.directLookupPath = directLookupPath;
    this.bestMatchingPattern = bestMatchingPattern;
  }

  public HandlerMatchingMetadata(HandlerMatchingMetadata other) {
    this.handler = other.handler;
    this.lookupPath = other.lookupPath;
    this.directLookupPath = other.directLookupPath;
    this.pathMatchInfo = other.pathMatchInfo;
    this.patternParser = other.patternParser;
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

  public Map<String, String> getUriVariables() {
    return getPathMatchInfo().getUriVariables();
  }

  public String getUriVariable(String name) {
    return getPathMatchInfo().getUriVariable(name);
  }

  public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
    return getPathMatchInfo().getMatrixVariables();
  }

  public PathPattern getBestMatchingPattern() {
    PathPattern bestMatchingPattern = this.bestMatchingPattern;
    if (bestMatchingPattern == null) {
      bestMatchingPattern = patternParser.parse(directLookupPath);
      this.bestMatchingPattern = bestMatchingPattern;
    }
    return bestMatchingPattern;
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

  @Nullable
  public MediaType[] getProducibleMediaTypes() {
    return producibleMediaTypes;
  }

  public Object getHandler() {
    return handler;
  }

}
