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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.utils.DataSize;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.exception.FileSizeExceededException;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.multipart.DefaultMultipartFile;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 * 2019-07-11 07:59
 */
public class DefaultMultipartResolver extends AbstractMultipartResolver {

  @Autowired
  public DefaultMultipartResolver(MultipartConfiguration multipartConfiguration) {
    super(multipartConfiguration);
  }

  @Override
  public boolean supports(final MethodParameter parameter) {
    final Class<?> parameterClass = parameter.getParameterClass();
    return parameterClass == MultipartFile.class //
            || parameterClass == DefaultMultipartFile.class;
  }

  @Override
  protected Object resolveInternal(final RequestContext context, //
                                   final MethodParameter parameter,
                                   final List<MultipartFile> files) throws Throwable {
    return files.get(0);
  }

  /**
   * @author TODAY <br>
   * 2019-07-12 18:18
   */
  public static class CollectionMultipartResolver extends AbstractMultipartResolver {

    @Autowired
    public CollectionMultipartResolver(MultipartConfiguration multipartConfiguration) {
      super(multipartConfiguration);
    }

    @Override
    public boolean supports(final MethodParameter parameter) {

      final Class<?> parameterClass = parameter.getParameterClass();

      return (parameterClass == Collection.class || parameterClass == List.class || parameterClass == Set.class) //
              && (parameter.isGenericPresent(MultipartFile.class, 0) //
              || parameter.isGenericPresent(DefaultMultipartFile.class, 0));
    }

    @Override
    protected Object resolveInternal(final RequestContext context, //
                                     final MethodParameter parameter,
                                     final List<MultipartFile> multipartFiles) throws Throwable //
    {
      if (parameter.getParameterClass() == Set.class) {
        return new HashSet<>(multipartFiles);
      }
      //for Collection List
      return multipartFiles;
    }
  }

  /**
   * @author TODAY <br>
   * 2019-07-12 17:43
   */
  public static class ArrayMultipartResolver extends AbstractMultipartResolver {

    @Autowired
    public ArrayMultipartResolver(MultipartConfiguration configuration) {
      super(configuration);
    }

    @Override
    public boolean supports(final MethodParameter parameter) {

      final Class<?> parameterClass;

      return parameter.isArray() //
              && ((parameterClass = parameter.getParameterClass().getComponentType()) == MultipartFile.class //
              || parameterClass == DefaultMultipartFile.class);
    }

    @Override
    protected Object resolveInternal(final RequestContext context,
                                     final MethodParameter parameter,
                                     final List<MultipartFile> multipartFiles) {

      return multipartFiles.toArray(new MultipartFile[multipartFiles.size()]);
    }
  }

  /**
   * @author TODAY <br>
   * 2019-07-11 23:35
   */
  public static class MapMultipartParameterResolver extends MapParameterResolver implements ParameterResolver {

    private final MultipartConfiguration multipartConfiguration;

    @Autowired
    public MapMultipartParameterResolver(MultipartConfiguration multipartConfiguration) {
      this.multipartConfiguration = multipartConfiguration;
    }

    @Override
    protected boolean supportsInternal(MethodParameter parameter) {

      if (parameter.isGenericPresent(String.class, 0)) { // Map<String, >
        if (parameter.isGenericPresent(List.class, 1)) { // Map<String, List<>>

          final Type type = parameter.getGenericityClass()[1];
          if (type instanceof ParameterizedType) {
            Type t = ((ParameterizedType) type).getActualTypeArguments()[0];
            return t.equals(MultipartFile.class) || t.equals(DefaultMultipartFile.class);
          }
        }
        else {
          return parameter.isGenericPresent(MultipartFile.class, 1) //
                  || parameter.isGenericPresent(DefaultMultipartFile.class, 1);
        }
      }
      return false;
    }

    @Override
    public Object resolveParameter(final RequestContext context, final MethodParameter parameter) throws Throwable {

      if (WebUtils.isMultipart(context)) {
        if (multipartConfiguration.getMaxRequestSize().toBytes() < context.contentLength()) { // exceed max size?
          throw new FileSizeExceededException(multipartConfiguration.getMaxRequestSize(), null)//
                  .setActual(DataSize.of(context.contentLength()));
        }

        try {
          final Map<String, List<MultipartFile>> multipartFiles = context.multipartFiles();

          if (multipartFiles.isEmpty()) {
            throw new MissingMultipartFileException(parameter);
          }

          if (parameter.isGenericPresent(List.class, 1)) {
            return multipartFiles;
          }
          final HashMap<String, MultipartFile> files = new HashMap<>();
          for (final Map.Entry<String, List<MultipartFile>> listEntry : multipartFiles.entrySet()) {
            files.put(listEntry.getKey(), listEntry.getValue().get(0));
          }

          return files;
        }
        finally {
          cleanupMultipart(context);
        }
      }
      throw WebUtils.newBadRequest("This is not a multipart request", parameter.getName(), null);
    }

    @Override
    public int getOrder() {
      return LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 80;
    }

    protected void cleanupMultipart(final RequestContext request) {}
  }

}
