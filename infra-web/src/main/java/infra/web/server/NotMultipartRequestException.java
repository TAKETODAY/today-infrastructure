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

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when a request is expected to be multipart but is not.
 *
 * @author TODAY 2021/3/21 21:08
 * @since 3.0
 */
public class NotMultipartRequestException extends MultipartException {

  public NotMultipartRequestException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
