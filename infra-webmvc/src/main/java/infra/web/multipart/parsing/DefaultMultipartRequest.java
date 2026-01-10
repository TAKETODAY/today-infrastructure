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

package infra.web.multipart.parsing;

import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.multipart.AbstractMultipartRequest;
import infra.web.multipart.Part;

/**
 * Default implementation of {@link infra.web.multipart.MultipartRequest}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 21:39
 */
final class DefaultMultipartRequest extends AbstractMultipartRequest {

  private final DefaultMultipartParser multipartParser;

  private final RequestContext context;

  public DefaultMultipartRequest(DefaultMultipartParser multipartParser, RequestContext context) {
    this.multipartParser = multipartParser;
    this.context = context;
  }

  @Override
  protected MultiValueMap<String, Part> parseRequest() {
    return multipartParser.parseRequest(context);
  }
}
