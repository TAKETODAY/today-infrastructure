/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.framework.config;

import cn.taketoday.util.DataSize;

/**
 * @author TODAY <br>
 * 2019-02-07 16:31
 */
public class CompressionConfiguration {

  private String level = "on";
  private boolean enable = true;

  private String[] excludePaths;
  private String[] includedPaths;
  private String[] excludeMethods;
  private String[] includeMethods;

  private String[] excludeUserAgents;
  private String[] excludeAgentPatterns;

  private String[] includeAgentPatterns;

  private DataSize minResponseSize = DataSize.ofKilobytes(2);

  private String[] mimeTypes = new String[] { //
          "text/html", "text/xml", "text/plain", //
          "text/javascript", "application/javascript", //
          "text/css", "text/javascript", "application/xml"//
  };

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  public String[] getExcludePaths() {
    return excludePaths;
  }

  public void setExcludePaths(String[] excludePaths) {
    this.excludePaths = excludePaths;
  }

  public String[] getIncludedPaths() {
    return includedPaths;
  }

  public void setIncludedPaths(String[] includedPaths) {
    this.includedPaths = includedPaths;
  }

  public String[] getExcludeMethods() {
    return excludeMethods;
  }

  public void setExcludeMethods(String[] excludeMethods) {
    this.excludeMethods = excludeMethods;
  }

  public String[] getIncludeMethods() {
    return includeMethods;
  }

  public void setIncludeMethods(String[] includeMethods) {
    this.includeMethods = includeMethods;
  }

  public String[] getExcludeUserAgents() {
    return excludeUserAgents;
  }

  public void setExcludeUserAgents(String[] excludeUserAgents) {
    this.excludeUserAgents = excludeUserAgents;
  }

  public String[] getExcludeAgentPatterns() {
    return excludeAgentPatterns;
  }

  public void setExcludeAgentPatterns(String[] excludeAgentPatterns) {
    this.excludeAgentPatterns = excludeAgentPatterns;
  }

  public String[] getIncludeAgentPatterns() {
    return includeAgentPatterns;
  }

  public void setIncludeAgentPatterns(String[] includeAgentPatterns) {
    this.includeAgentPatterns = includeAgentPatterns;
  }

  public DataSize getMinResponseSize() {
    return minResponseSize;
  }

  public void setMinResponseSize(DataSize minResponseSize) {
    this.minResponseSize = minResponseSize;
  }

  public String[] getMimeTypes() {
    return mimeTypes;
  }

  public void setMimeTypes(String[] mimeTypes) {
    this.mimeTypes = mimeTypes;
  }
}
