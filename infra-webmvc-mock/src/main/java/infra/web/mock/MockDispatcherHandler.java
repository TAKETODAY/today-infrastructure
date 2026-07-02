/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.mock;

import java.io.Serial;
import java.io.Serializable;

import infra.context.ApplicationContext;
import infra.mock.api.DispatcherType;
import infra.mock.api.MockException;
import infra.mock.api.MockHandler;
import infra.mock.api.MockResponse;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.web.MockRequest;
import infra.web.DispatcherHandler;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.async.WebAsyncManager;

/**
 * Central dispatcher for HTTP request handlers/controllers in Servlet
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.0 2018-06-25 19:47:14
 */
@SuppressWarnings("NullAway")
public class MockDispatcherHandler extends DispatcherHandler implements MockHandler, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public MockDispatcherHandler(ApplicationContext context) {
    super(context);
  }

  @Override
  public void service(MockRequest request, MockResponse response) throws MockException {
    if (request.getDispatcherType() == DispatcherType.ASYNC) {
      // send async results
      Object concurrentResult = request.getAttribute(WebAsyncManager.WEB_ASYNC_RESULT_ATTRIBUTE);
      RequestContext context = (RequestContext) request.getAttribute(WebAsyncManager.WEB_ASYNC_REQUEST_ATTRIBUTE);
      Object httpRequestHandler = WebAsyncManager.findHttpRequestHandler(context);

      try {
        handleConcurrentResult(context, httpRequestHandler, concurrentResult);
      }
      catch (final Throwable e) {
        throw new MockException("Async processing failed: " + e, e);
      }
      return;
    }

    RequestContext context = RequestContextHolder.current();

    boolean reset = false;
    if (context == null) {
      context = new MockRequestContext(getApplicationContext(),
              request, (HttpMockResponse) response, this);
      RequestContextHolder.set(context);
      reset = true;
    }

    try {
      handleRequest(context);
    }
    catch (final Throwable e) {
      throw new MockException("Handler processing failed: " + e, e);
    }
    finally {
      if (reset) {
        RequestContextHolder.cleanup();
      }
    }
  }

}
