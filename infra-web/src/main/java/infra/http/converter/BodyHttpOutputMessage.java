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

package infra.http.converter;

import java.io.IOException;
import java.io.OutputStream;

import infra.http.HttpOutputMessage;
import infra.http.HttpOutputMessageDecorator;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/12 16:24
 */
final class BodyHttpOutputMessage extends HttpOutputMessageDecorator {

  private final OutputStream body;

  public BodyHttpOutputMessage(HttpOutputMessage delegate, OutputStream body) {
    super(delegate);
    this.body = body;
  }

  @Override
  public OutputStream getBody() throws IOException {
    return body;
  }

}
