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

package cn.taketoday.web.resource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * Abstract base class for {@link VersionStrategy} implementations.
 *
 * <p>Supports versions as:
 * <ul>
 * <li>prefix in the request path, like "version/static/myresource.js"
 * <li>file name suffix in the request path, like "static/myresource-version.js"
 * </ul>
 *
 * <p>Note: This base class does <i>not</i> provide support for generating the
 * version string.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractVersionStrategy implements VersionStrategy {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final VersionPathStrategy pathStrategy;

  protected AbstractVersionStrategy(VersionPathStrategy pathStrategy) {
    Assert.notNull(pathStrategy, "VersionPathStrategy is required");
    this.pathStrategy = pathStrategy;
  }

  public VersionPathStrategy getVersionPathStrategy() {
    return this.pathStrategy;
  }

  @Override
  @Nullable
  public String extractVersion(String requestPath) {
    return this.pathStrategy.extractVersion(requestPath);
  }

  @Override
  public String removeVersion(String requestPath, String version) {
    return this.pathStrategy.removeVersion(requestPath, version);
  }

  @Override
  public String addVersion(String requestPath, String version) {
    return this.pathStrategy.addVersion(requestPath, version);
  }

  /**
   * A prefix-based {@code VersionPathStrategy},
   * e.g. {@code "{version}/path/foo.js"}.
   */
  protected static class PrefixVersionPathStrategy implements VersionPathStrategy {

    private final String prefix;

    public PrefixVersionPathStrategy(String version) {
      Assert.hasText(version, "Version must not be empty");
      this.prefix = version;
    }

    @Override
    @Nullable
    public String extractVersion(String requestPath) {
      return (requestPath.startsWith(this.prefix) ? this.prefix : null);
    }

    @Override
    public String removeVersion(String requestPath, String version) {
      return requestPath.substring(this.prefix.length());
    }

    @Override
    public String addVersion(String path, String version) {
      if (path.startsWith(".")) {
        return path;
      }
      else {
        return (this.prefix.endsWith("/") || path.startsWith("/") ?
                this.prefix + path : this.prefix + '/' + path);
      }
    }
  }

  /**
   * File name-based {@code VersionPathStrategy},
   * e.g. {@code "path/foo-{version}.css"}.
   */
  protected static class FileNameVersionPathStrategy implements VersionPathStrategy {

    private static final Pattern pattern = Pattern.compile("-(\\S*)\\.");

    @Override
    @Nullable
    public String extractVersion(String requestPath) {
      Matcher matcher = pattern.matcher(requestPath);
      if (matcher.find()) {
        String match = matcher.group(1);
        return (match.contains("-") ? match.substring(match.lastIndexOf('-') + 1) : match);
      }
      else {
        return null;
      }
    }

    @Override
    public String removeVersion(String requestPath, String version) {
      return StringUtils.delete(requestPath, "-" + version);
    }

    @Override
    public String addVersion(String requestPath, String version) {
      String baseFilename = StringUtils.stripFilenameExtension(requestPath);
      String extension = StringUtils.getFilenameExtension(requestPath);
      return (baseFilename + '-' + version + '.' + extension);
    }
  }

}
