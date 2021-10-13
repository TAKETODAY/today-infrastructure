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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Autowired;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.ServletPartMultipartFile;

import static cn.taketoday.web.resolver.DataBinderMapParameterResolver.isMap;

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
  public boolean supportsParameter(final MethodParameter parameter) {
    return supportsMultipart(parameter.getParameterClass());
  }

  protected static boolean supportsMultipart(final Class<?> type) {
    return type == MultipartFile.class || type == ServletPartMultipartFile.class;
  }

  @Override
  protected Object resolveInternal(final RequestContext context,
                                   final MethodParameter parameter,
                                   final List<MultipartFile> files) {
    return files.get(0);
  }

  public static void register(
          ParameterResolvingStrategies resolvers, MultipartConfiguration multipartConfig) {
    resolvers.add(new DefaultMultipartResolver(multipartConfig),
                  new ArrayMultipartResolver(multipartConfig),
                  new CollectionMultipartResolver(multipartConfig),
                  new MapMultipartParameterResolver(multipartConfig));
  }

  /**
   * @author TODAY <br>
   * 2019-07-12 18:18
   */
  static class CollectionMultipartResolver extends AbstractMultipartResolver {

    @Autowired
    public CollectionMultipartResolver(MultipartConfiguration multipartConfiguration) {
      super(multipartConfiguration);
    }

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
      final Class<?> parameterClass = parameter.getParameterClass();
      return CollectionUtils.isCollection(parameterClass) && (
              parameter.isGenericPresent(MultipartFile.class, 0)
                      || parameter.isGenericPresent(ServletPartMultipartFile.class, 0)
      );
    }

    @Override
    protected Object resolveInternal(final RequestContext context,
                                     final MethodParameter parameter,
                                     final List<MultipartFile> multipartFiles) //
    {
      final Class<?> parameterClass = parameter.getParameterClass();
      if (parameterClass == Collection.class || parameterClass == List.class) {
        //for Collection List
        return multipartFiles;
      }
      final Collection<MultipartFile> ret = CollectionUtils.createCollection(parameterClass, multipartFiles.size());
      ret.addAll(multipartFiles);
      return ret;
    }
  }

  /**
   * @author TODAY <br>
   * 2019-07-12 17:43
   */
  static class ArrayMultipartResolver extends AbstractMultipartResolver {

    @Autowired
    public ArrayMultipartResolver(MultipartConfiguration configuration) {
      super(configuration);
    }

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
      if (parameter.isArray()) {
        final Class<?> componentType = parameter.getComponentType();
        return DefaultMultipartResolver.supportsMultipart(componentType);
      }
      return false;
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
  static class MapMultipartParameterResolver
          extends AbstractMultipartResolver implements ParameterResolvingStrategy {

    @Autowired
    public MapMultipartParameterResolver(MultipartConfiguration multipartConfig) {
      super(multipartConfig);
    }

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
      if (isMap(parameter) || parameter.is(MultiValueMap.class)) {
        if (parameter.isGenericPresent(String.class, 0)) { // Map<String, >
          Class<?> target = null;
          if (parameter.isGenericPresent(List.class, 1)) { // Map<String, List<>>
            final Type generic = parameter.getGeneric(1);
            if (generic instanceof ParameterizedType) {
              Type t = ((ParameterizedType) generic).getActualTypeArguments()[0];
              target = (Class<?>) t;
            }
          }
          else {
            target = (Class<?>) parameter.getGeneric(1);
          }
          return supportsMultipart(target);
        }
      }
      return false;
    }

    @Override
    protected Object resolveInternal(
            final RequestContext context, final MethodParameter parameter,
            final MultiValueMap<String, MultipartFile> multipartFiles) throws Throwable {
      // Map<String, List<MultipartFile>>
      if (parameter.is(MultiValueMap.class) || parameter.isGenericPresent(List.class, 1)) {
        return multipartFiles;
      }

      // Map<String, MultipartFile>
      return multipartFiles.toSingleValueMap();
    }

  }

}
