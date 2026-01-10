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

package infra.web.controller;

import infra.app.Application;
import infra.app.InfraApplication;
import infra.http.HttpHeaders;
import infra.http.ProblemDetail;
import infra.http.ResponseEntity;
import org.jspecify.annotations.Nullable;
import infra.web.annotation.GET;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/8 23:21
 */
@RestController
@InfraApplication
@RequestMapping("/response-entity")
class ResponseEntityController {

  @GET
  public ResponseEntity<String> entity(@RequestBody @Nullable String body, HttpHeaders headers) {
    return ResponseEntity.ok()
            .contentType(headers.getContentType())
            .body(body == null ? "entity" : body);
  }

  @GET("/problem-detail")
  public ResponseEntity<ProblemDetail> problemDetail() {
    return ResponseEntity.of(ProblemDetail.forRawStatusCode(201))
            .header("X-test", "test")
            .build();
  }

  public static void main(String[] args) {
    Application.run(ResponseEntityController.class, args);
  }

}
