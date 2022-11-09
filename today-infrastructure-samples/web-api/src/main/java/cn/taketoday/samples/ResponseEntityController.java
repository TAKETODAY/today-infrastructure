/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.samples;

import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/8 23:21
 */
@RestController
@RequestMapping("/response-entity")
public class ResponseEntityController {

  @GET
  public ResponseEntity<String> entity() {
    return ResponseEntity.ok("entity");
  }

  @GET("/problem-detail")
  public ResponseEntity<ProblemDetail> problemDetail() {
    return ResponseEntity.of(ProblemDetail.forRawStatusCode(201))
            .header("X-test", "test")
            .build();
  }
}
