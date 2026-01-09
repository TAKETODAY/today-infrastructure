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

package infra.web.server;

import infra.http.HttpStatus;

/**
 * Exception for errors that fit response status 413 (Content too large) for use in
 * Web applications.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@SuppressWarnings("serial")
public class RequestBodySizeExceededException extends ResponseStatusException {

  private final long maxContentLength;

  public RequestBodySizeExceededException(long maxContentLength) {
    super(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum request body size %sexceeded".formatted(maxContentLength >= 0 ? "of %d bytes ".formatted(maxContentLength) : ""), null);
    this.maxContentLength = maxContentLength;
  }

  public long getMaxContentLength() {
    return maxContentLength;
  }

}
