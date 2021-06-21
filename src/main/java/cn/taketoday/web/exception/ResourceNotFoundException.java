/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.exception;

import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.handler.ResourceMatchResult;
import cn.taketoday.web.http.HttpStatus;

import static cn.taketoday.web.Constant.RESOURCE_MATCH_RESULT;

/**
 * @author TODAY 2021/2/5 10:30
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends NotFoundException {
  private static final long serialVersionUID = 1L;

  private final ResourceMatchResult matchResult;

  public ResourceNotFoundException(Throwable cause, ResourceMatchResult matchResult) {
    super(cause);
    this.matchResult = matchResult;
  }

  public ResourceNotFoundException(String message, Throwable cause, ResourceMatchResult matchResult) {
    super(message, cause);
    this.matchResult = matchResult;
  }

  public ResourceNotFoundException(String message, ResourceMatchResult matchResult) {
    super(message);
    this.matchResult = matchResult;
  }

  public ResourceNotFoundException(ResourceMatchResult matchResult) {
    super("Resource Not Found");
    this.matchResult = matchResult;
  }

  public ResourceMatchResult getMatchResult() {
    return matchResult;
  }

  public static ResourceNotFoundException notFound() {
    return notFound((ResourceMatchResult) RequestContextHolder.currentContext().getAttribute(RESOURCE_MATCH_RESULT));
  }

  public static ResourceNotFoundException notFound(ResourceMatchResult matchResult) {
    final String requestPath = matchResult.getRequestPath();
    return notFound("Resource '" + requestPath + "' Not Found", matchResult);
  }

  public static ResourceNotFoundException notFound(String msg, ResourceMatchResult matchResult) {
    return new ResourceNotFoundException(msg, matchResult);
  }

}
