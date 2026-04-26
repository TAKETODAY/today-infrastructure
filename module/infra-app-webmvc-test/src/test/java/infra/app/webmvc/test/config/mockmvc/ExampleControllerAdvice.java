/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.webmvc.test.config.mockmvc;

import infra.app.webmvc.test.config.WebMvcTest;
import infra.http.HttpStatus;
import infra.http.ResponseEntity;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.ResponseStatus;
import infra.web.handler.HandlerNotFoundException;

/**
 * Example {@link ControllerAdvice @ControllerAdvice} used with
 * {@link WebMvcTest @WebMvcTest} tests.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@ControllerAdvice
public class ExampleControllerAdvice {

  @ExceptionHandler
  public ResponseEntity<String> onExampleError(ExampleException exception) {
    return ResponseEntity.ok("recovered");
  }

  @ExceptionHandler(HandlerNotFoundException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<String> noHandlerFoundHandler(HandlerNotFoundException exception) {
    return ResponseEntity.badRequest().body("Invalid request: " + exception.getRequestURI());
  }

}
