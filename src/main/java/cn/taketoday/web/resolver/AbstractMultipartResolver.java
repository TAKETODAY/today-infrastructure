/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.resolver;

import java.util.List;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.FileSizeExceededException;
import cn.taketoday.util.DataSize;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.util.WebUtils;

/**
 * For Multipart {@link MultipartFile}
 *
 * @author TODAY 2019-07-11 23:14
 */
public abstract class AbstractMultipartResolver
        extends AbstractParameterResolver implements ParameterResolvingStrategy {

  protected final MultipartConfiguration multipartConfiguration;

  public AbstractMultipartResolver(MultipartConfiguration multipartConfig) {
    this.multipartConfiguration = multipartConfig;
  }

  /**
   * @throws FileSizeExceededException upload file size exceeded
   * @see MultipartConfiguration#getMaxRequestSize()
   */
  @Override
  protected Object resolveInternal(RequestContext context, ResolvableMethodParameter parameter) throws Throwable {
    if (WebUtils.isMultipart(context)) {
      DataSize maxRequestSize = getMultipartConfiguration().getMaxRequestSize();
      // exceed max size?
      if (maxRequestSize.toBytes() < context.getContentLength()) {
        throw new FileSizeExceededException(maxRequestSize, null)
                .setActual(DataSize.of(context.getContentLength()));
      }
      return resolveInternal(context, parameter, context.multipartFiles());
    }
    return null;
  }

  @Override
  protected Object missingParameter(ResolvableMethodParameter parameter) {
    throw new MissingMultipartFileException(parameter.getName(), parameter.getParameter());
  }

  protected Object resolveInternal(RequestContext context,
                                   ResolvableMethodParameter parameter,
                                   MultiValueMap<String, MultipartFile> multipartFiles) throws Throwable {

    List<MultipartFile> resolved = multipartFiles.get(parameter.getName());
    if (resolved != null) {
      return resolveInternal(context, parameter, resolved);
    }
    return null;
  }

  /**
   * @param multipartFiles none null multipart files
   */
  protected Object resolveInternal(RequestContext context,
                                   ResolvableMethodParameter parameter,
                                   List<MultipartFile> multipartFiles) throws Throwable {
    return null;
  }

  @Override
  public abstract boolean supportsParameter(ResolvableMethodParameter parameter);

  public MultipartConfiguration getMultipartConfiguration() {
    return multipartConfiguration;
  }

}
