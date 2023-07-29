/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.error;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.framework.web.servlet.server.AbstractServletWebServerFactory;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * Basic global error {@link Controller @Controller}, rendering {@link ErrorAttributes}.
 * More specific errors can be handled either using Web MVC abstractions (e.g.
 * {@code @ExceptionHandler}) or by adding servlet
 * {@link AbstractServletWebServerFactory#setErrorPages server error pages}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Michael Stummvoll
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ErrorAttributes
 * @see ErrorProperties
 * @since 4.0
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class BasicErrorController extends AbstractErrorController {

  /**
   * Create a new {@link BasicErrorController} instance.
   *
   * @param errorAttributes the error attributes
   * @param errorProperties configuration properties
   */
  public BasicErrorController(ErrorAttributes errorAttributes, ErrorProperties errorProperties) {
    this(errorAttributes, errorProperties, Collections.emptyList());
  }

  /**
   * Create a new {@link BasicErrorController} instance.
   *
   * @param errorAttributes the error attributes
   * @param errorProperties configuration properties
   * @param errorViewResolvers error view resolvers
   */
  public BasicErrorController(ErrorAttributes errorAttributes,
          ErrorProperties errorProperties, List<ErrorViewResolver> errorViewResolvers) {
    super(errorAttributes, errorProperties, errorViewResolvers);
  }

  @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
  public Object errorHtml(RequestContext request) {
    HttpStatus status = getStatus(request);
    var model = Collections.unmodifiableMap(getErrorAttributes(request, MediaType.TEXT_HTML));
    request.setStatus(status);
    return resolveErrorView(request, status, model);
  }

  @RequestMapping
  public ResponseEntity<Map<String, Object>> error(RequestContext request) {
    HttpStatus status = getStatus(request);
    if (status == HttpStatus.NO_CONTENT) {
      return new ResponseEntity<>(status);
    }
    var body = getErrorAttributes(request, MediaType.ALL);
    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<String> mediaTypeNotAcceptable(RequestContext request) {
    HttpStatus status = getStatus(request);
    return ResponseEntity.status(status).build();
  }

}
