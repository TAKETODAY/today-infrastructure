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
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.util.pattern.PathMatchInfo;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 14:16
 */
public class HandlerMatchingMetadata {
  private final String directPath;
  private final RequestPath requestPath;

  private PathPattern bestMatchingPattern;

  private PathContainer pathWithinMapping;

  private PathMatchInfo pathMatchInfo;

  private PathContainer pathWithinApplication;

  @Nullable
  private MediaType[] producibleMediaTypes;

  private final PathPatternParser patternParser;

  public HandlerMatchingMetadata(RequestContext request) {
    this.bestMatchingPattern = null;
    this.directPath = request.getRequestPath();
    this.requestPath = request.getLookupPath();
    this.patternParser = PathPatternParser.defaultInstance;
  }

  public HandlerMatchingMetadata(
          String directPath, RequestPath requestPath, PathPattern bestMatchingPattern, PathPatternParser patternParser) {
    this.directPath = directPath;
    this.requestPath = requestPath;
    this.patternParser = patternParser;
    this.bestMatchingPattern = bestMatchingPattern;
  }

  public HandlerMatchingMetadata(HandlerMatchingMetadata other) {
    this.directPath = other.directPath;
    this.requestPath = other.requestPath;
    this.pathMatchInfo = other.pathMatchInfo;
    this.patternParser = other.patternParser;
    this.pathWithinMapping = other.pathWithinMapping;
    this.bestMatchingPattern = other.bestMatchingPattern;
    this.producibleMediaTypes = other.producibleMediaTypes;
    this.pathWithinApplication = other.pathWithinApplication;
  }

  public PathMatchInfo getPathMatchInfo() {
    PathMatchInfo pathMatchInfo = this.pathMatchInfo;
    if (pathMatchInfo == null) {
      pathMatchInfo = getBestMatchingPattern().matchAndExtract(requestPath);
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
      pathWithinMapping = getBestMatchingPattern().extractPathWithinPattern(requestPath);
      this.pathWithinMapping = pathWithinMapping;
    }
    return pathWithinMapping;
  }

  @Experimental
  public final PathContainer pathWithinApplication() {
    PathContainer pathWithinApplication = this.pathWithinApplication;
    if (pathWithinApplication == null) {
      pathWithinApplication = requestPath.pathWithinApplication();
      this.pathWithinApplication = pathWithinApplication;
    }
    return pathWithinApplication;
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
      bestMatchingPattern = patternParser.parse(directPath);
      this.bestMatchingPattern = bestMatchingPattern;
    }
    return bestMatchingPattern;
  }

  public RequestPath getRequestPath() {
    return requestPath;
  }

  public String getDirectPath() {
    return directPath;
  }

  public void setProducibleMediaTypes(@Nullable MediaType[] producibleMediaTypes) {
    this.producibleMediaTypes = producibleMediaTypes;
  }

  @Nullable
  public MediaType[] getProducibleMediaTypes() {
    return producibleMediaTypes;
  }

}
