/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.web.resolver;

import java.util.List;

import cn.taketoday.context.utils.DataSize;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.exception.FileSizeExceededException;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-07-11 23:14
 */
public abstract class AbstractMultipartResolver implements ParameterResolver {

  private final MultipartConfiguration multipartConfiguration;

  public AbstractMultipartResolver(MultipartConfiguration multipartConfiguration) {
    this.multipartConfiguration = multipartConfiguration;
  }

  @Override
  public final Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {

    if (WebUtils.isMultipart(requestContext)) {

      if (getMultipartConfiguration().getMaxRequestSize().toBytes() < requestContext.contentLength()) { // exceed max size?

        throw new FileSizeExceededException(getMultipartConfiguration().getMaxRequestSize(), null)//
                .setActual(DataSize.of(requestContext.contentLength()));
      }

      try {

        final List<MultipartFile> multipartFiles = requestContext.multipartFiles().get(parameter.getName());
        if (multipartFiles != null) {
          return resolveInternal(requestContext, parameter, multipartFiles);
        }
        throw WebUtils.newBadRequest("Target multipart file must not be null", parameter.getName(), null);
      }
      finally {
        cleanupMultipart(requestContext);
      }
    }
    throw WebUtils.newBadRequest("This is not a multipart request", parameter.getName(), null);
  }

  @Override
  public abstract boolean supports(final MethodParameter parameter);

  //@off
    protected abstract Object resolveInternal(final RequestContext requestContext, //
            final MethodParameter parameter, final List<MultipartFile> multipartFiles) throws Throwable;

    
    protected void cleanupMultipart(final RequestContext request) {}

    public MultipartConfiguration getMultipartConfiguration() {
        return multipartConfiguration;
    }

}
