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

package infra.samples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestPart;
import infra.web.annotation.RestController;
import infra.web.bind.WebDataBinder;
import infra.web.bind.annotation.InitBinder;
import infra.web.multipart.Part;
import infra.web.multipart.support.StringPartEditor;
import infra.web.view.ViewRef;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/11 11:36
 */
@RestController
@RequestMapping("/upload")
class UploadHttpHandler {

  private static final Logger log = LoggerFactory.getLogger(UploadHttpHandler.class);

  // for annotation-request-param
  @InitBinder("data")
  void init(WebDataBinder binder) {
    log.info("binder: {}", binder);
    binder.registerCustomEditor(String.class, new StringPartEditor(StandardCharsets.UTF_8));
  }

  @GET
  public ViewRef upload() {
    return ViewRef.forViewName("upload");
  }

  @POST("/annotation-request-part")
  public String upload(@RequestPart String data, Part file) throws IOException {
    return data + " -> " + file.getContentAsString();
  }

  @POST("/annotation-request-param")
  public String up(String data, Part file) throws IOException {
    return "param: " + data + " -> " + file.getContentAsString();
  }

}
