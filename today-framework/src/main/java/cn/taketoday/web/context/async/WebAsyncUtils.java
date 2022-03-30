/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.web.context.async;

import cn.taketoday.web.RequestContext;

/**
 * Utility methods related to processing asynchronous web requests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class WebAsyncUtils {

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

  /**
   * Create an AsyncWebRequest instance. By default, an instance of
   * {@link StandardServletAsyncWebRequest} gets created.
   *
   * @param requestContext the current request context
   * @return an AsyncWebRequest instance (never {@code null})
   */
  public static AsyncWebRequest createAsyncWebRequest(RequestContext requestContext) {
    return new StandardServletAsyncWebRequest(requestContext);
  }

}
