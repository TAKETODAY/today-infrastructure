/**
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
package cn.taketoday.web.resolver;

import java.util.List;
import java.util.Map;

import cn.taketoday.context.utils.DataSize;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.exception.FileSizeExceededException;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.utils.WebUtils;

/**
 * For Multipart {@link MultipartFile}
 *
 * @author TODAY 2019-07-11 23:14
 */
public abstract class AbstractMultipartResolver
        extends AbstractParameterResolver implements ParameterResolver {

  protected final MultipartConfiguration multipartConfiguration;

  public AbstractMultipartResolver(MultipartConfiguration multipartConfig) {
    this.multipartConfiguration = multipartConfig;
  }

  /**
   * @throws FileSizeExceededException
   *         upload file size exceeded
   * @see MultipartConfiguration#getMaxRequestSize()
   */
  @Override
  protected Object resolveInternal(final RequestContext context, final MethodParameter parameter) throws Throwable {
    if (WebUtils.isMultipart(context)) {
      final DataSize maxRequestSize = getMultipartConfiguration().getMaxRequestSize();
      // exceed max size?
      if (maxRequestSize.toBytes() < context.getContentLength()) {
        throw new FileSizeExceededException(maxRequestSize, null)
                .setActual(DataSize.of(context.getContentLength()));
      }
      try {
        return resolveInternal(context, parameter, context.multipartFiles());
      }
      finally {
        cleanupMultipart(context);
      }
    }
    return null;
  }

  @Override
  protected Object missingParameter(MethodParameter parameter) {
    throw new MissingMultipartFileException(parameter);
  }

  protected Object resolveInternal(final RequestContext context,
                                   final MethodParameter parameter,
                                   final Map<String, List<MultipartFile>> multipartFiles) throws Throwable {

    final List<MultipartFile> resolved = multipartFiles.get(parameter.getName());
    if (resolved != null) {
      return resolveInternal(context, parameter, resolved);
    }
    return null;
  }

  /**
   * @param multipartFiles
   *         none null multipart files
   */
  protected Object resolveInternal(final RequestContext context,
                                   final MethodParameter parameter,
                                   final List<MultipartFile> multipartFiles) throws Throwable {
    return null;
  }

  @Override
  public abstract boolean supports(final MethodParameter parameter);

  protected void cleanupMultipart(final RequestContext request) {}

  public MultipartConfiguration getMultipartConfiguration() {
    return multipartConfiguration;
  }

}
