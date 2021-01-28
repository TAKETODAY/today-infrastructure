/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.registry;

import cn.taketoday.context.PathMatcher;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY
 * @date 2020/12/23 15:56
 * @since 3.0
 */
public abstract class AbstractUrlHandlerRegistry extends CacheableMappedHandlerRegistry {

  private Object rootHandler;

  private boolean useTrailingSlashMatch = true;

  /**
   * Set the root handler for this handler mapping, that is,
   * the handler to be registered for the root path ("/").
   * <p>Default is {@code null}, indicating no root handler.
   */
  public void setRootHandler(Object rootHandler) {
    this.rootHandler = rootHandler;
  }

  /**
   * Return the root handler for this handler mapping (registered for "/"),
   * or {@code null} if none.
   */
  public Object getRootHandler() {
    return this.rootHandler;
  }

  /**
   * Whether to match to URLs irrespective of the presence of a trailing slash.
   * If enabled a URL pattern such as "/users" also matches to "/users/".
   * <p>The default value is {@code false}.
   */
  public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
    this.useTrailingSlashMatch = useTrailingSlashMatch;
  }

  /**
   * Whether to match to URLs irrespective of the presence of a trailing slash.
   */
  public boolean useTrailingSlashMatch() {
    return this.useTrailingSlashMatch;
  }

  /**
   * @param lookupPath
   *         使用 url path 作Key
   * @param context
   *         Current request context
   */
  @Override
  protected Object handlerNotFound(final String lookupPath, final RequestContext context) {
    Object rawHandler = super.handlerNotFound(lookupPath, context);
    if (rawHandler == null && "/".equals(lookupPath)) {
      // We need to care for the default handler directly, since we need to
      rawHandler = getRootHandler();
    }
    return rawHandler;
  }

  @Override
  protected boolean matchingPattern(
          final PathMatcher pathMatcher,
          final String pattern, final String urlPath
  ) {
    if (super.matchingPattern(pathMatcher, pattern, urlPath)) {
      return true;
    }
    else if (useTrailingSlashMatch()) {
      return !pattern.endsWith("/") && pathMatcher.match(pattern + '/', urlPath);
    }
    return false;
  }

}
