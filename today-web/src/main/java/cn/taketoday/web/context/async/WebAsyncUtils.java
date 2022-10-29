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

package cn.taketoday.web.context.async;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.RequestContext;

/**
 * Utility methods related to processing asynchronous web requests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class WebAsyncUtils {

  /**
   * The name attribute of the {@link RequestContext}.
   */
  public static final String WEB_ASYNC_REQUEST_ATTRIBUTE =
          WebAsyncManager.class.getName() + ".WEB_REQUEST";

  /**
   * The name attribute containing the result.
   */
  public static final String WEB_ASYNC_RESULT_ATTRIBUTE =
          WebAsyncManager.class.getName() + ".WEB_ASYNC_RESULT";

  /**
   * The name attribute containing the {@link WebAsyncManager}.
   */
  public static final String WEB_ASYNC_MANAGER_ATTRIBUTE =
          WebAsyncManager.class.getName() + ".WEB_ASYNC_MANAGER";

  /**
   * Obtain the {@link WebAsyncManager} for the current request, or if not
   * found, create and associate it with the request.
   */
  public static WebAsyncManager getAsyncManager(RequestContext context) {
    WebAsyncManager asyncManager = null;
    Object asyncManagerAttr = context.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE);
    if (asyncManagerAttr instanceof WebAsyncManager) {
      asyncManager = (WebAsyncManager) asyncManagerAttr;
    }
    if (asyncManager == null) {
      asyncManager = new WebAsyncManager(context);
      context.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager);
    }
    return asyncManager;
  }

  @Nullable
  public static Object findHttpRequestHandler(RequestContext request) {
    var asyncManager = WebAsyncUtils.getAsyncManager(request);
    Object[] concurrentResultContext = asyncManager.getConcurrentResultContext();
    if (concurrentResultContext != null && concurrentResultContext.length == 1) {
      return concurrentResultContext[0];
    }
    else {
      HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
      if (matchingMetadata != null) {
        return matchingMetadata.getHandler();
      }
    }
    return null;
  }

  /**
   * is a WebAsyncManager already associated with the request?
   */
  public static boolean isAvailable(RequestContext context) {
    Object asyncManagerAttr = context.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE);
    return asyncManagerAttr instanceof WebAsyncManager;
  }

}
