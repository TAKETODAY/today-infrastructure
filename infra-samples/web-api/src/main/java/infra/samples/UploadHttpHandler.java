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
