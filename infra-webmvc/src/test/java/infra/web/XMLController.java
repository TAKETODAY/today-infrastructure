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

package infra.web;

import infra.util.StringUtils;

/**
 * @author TODAY <br>
 * 2020-05-05 17:34
 */
public class XMLController {

  public void test(RequestContext request) {
    request.setAttribute("key", "World");
    System.err.println(request);
  }

  public Object obj(RequestContext request) {

    String key = request.getParameter("r");
    if (StringUtils.isNotEmpty(key)) {
      return "redirect:/" + key;
    }
    request.setAttribute("key", request.getParameter("key"));

    return "/xml/test";
  }

}
