/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.server.netty;

import java.nio.charset.StandardCharsets;

import infra.context.ApplicationContext;
import infra.http.MediaType;
import infra.web.DispatcherHandler;
import infra.web.HttpContext;

/**
 * Minimal dispatcher shared by the HTTP/1.1 and HTTP/2 benchmarks.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class BenchmarkHandler extends DispatcherHandler {

  private static final byte[] BODY = "hello".getBytes(StandardCharsets.UTF_8);

  BenchmarkHandler(ApplicationContext context) {
    super(context);
  }

  @Override
  public void handleRequest(HttpContext context) throws Exception {
    context.setContentType(MediaType.TEXT_PLAIN);
    context.setContentLength(BODY.length);
    context.getOutputStream().write(BODY);
    requestCompleted(context, null);
  }

}
