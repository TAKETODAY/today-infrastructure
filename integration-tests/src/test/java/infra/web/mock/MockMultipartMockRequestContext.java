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

package infra.web.mock;

import infra.context.ApplicationContext;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.web.MockMultipartHttpMockRequest;
import infra.web.multipart.MultipartRequest;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 17:04
 */
@SuppressWarnings("serial")
public class MockMultipartMockRequestContext extends MockRequestContext {

  private final MockMultipartHttpMockRequest request;

  public MockMultipartMockRequestContext(MockMultipartHttpMockRequest request, HttpMockResponse response) {
    super(request, response);
    this.request = request;
  }

  public MockMultipartMockRequestContext(ApplicationContext applicationContext, MockMultipartHttpMockRequest request, HttpMockResponse response) {
    super(applicationContext, request, response);
    this.request = request;
  }

  @Override
  public MultipartRequest asMultipartRequest() {
    return request.getMultipartRequest();
  }

}
