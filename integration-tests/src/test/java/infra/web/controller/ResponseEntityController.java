/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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
