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

import java.util.Collection;
import java.util.List;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.ServletPartMultipartFile;

import static cn.taketoday.web.resolver.DataBinderMapParameterResolver.isMap;

/**
 * @author TODAY <br>
 * 2019-07-11 07:59
 */
public class DefaultMultipartResolver extends AbstractMultipartResolver {

  public DefaultMultipartResolver(MultipartConfiguration multipartConfiguration) {
    super(multipartConfiguration);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return supportsMultipart(resolvable.getParameterType());
  }

  protected static boolean supportsMultipart(Class<?> type) {
    return type == MultipartFile.class || type == ServletPartMultipartFile.class;
  }

  @Override
  protected Object resolveInternal(
          RequestContext context, ResolvableMethodParameter parameter, List<MultipartFile> files) {
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

    public CollectionMultipartResolver(MultipartConfiguration multipartConfiguration) {
      super(multipartConfiguration);
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      Class<?> parameterClass = resolvable.getParameterType();
      if (CollectionUtils.isCollection(parameterClass)) {
        ResolvableType type = resolvable.getResolvableType().asCollection();
        Class<?> elementType = type.getGeneric(0).resolve();
        return elementType == MultipartFile.class
                || elementType == ServletPartMultipartFile.class;
      }
      return false;
    }

    @Override
    protected Object resolveInternal(
            RequestContext context,
            ResolvableMethodParameter parameter,
            List<MultipartFile> multipartFiles) //
    {
      Class<?> parameterClass = parameter.getParameterType();
      if (parameterClass == Collection.class || parameterClass == List.class) {
        //for Collection List
        return multipartFiles;
      }
      Collection<MultipartFile> ret = CollectionUtils.createCollection(parameterClass, multipartFiles.size());
      ret.addAll(multipartFiles);
      return ret;
    }
  }

  /**
   * @author TODAY <br>
   * 2019-07-12 17:43
   */
  static class ArrayMultipartResolver extends AbstractMultipartResolver {

    public ArrayMultipartResolver(MultipartConfiguration configuration) {
      super(configuration);
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      if (resolvable.getParameterType().isArray()) {
        Class<?> componentType = resolvable.getParameterType().getComponentType();
        return DefaultMultipartResolver.supportsMultipart(componentType);
      }
      return false;
    }

    @Override
    protected Object resolveInternal(RequestContext context,
                                     ResolvableMethodParameter parameter,
                                     List<MultipartFile> multipartFiles) {

      return multipartFiles.toArray(new MultipartFile[multipartFiles.size()]);
    }
  }

  /**
   * @author TODAY <br>
   * 2019-07-11 23:35
   */
  static class MapMultipartParameterResolver
          extends AbstractMultipartResolver implements ParameterResolvingStrategy {

    public MapMultipartParameterResolver(MultipartConfiguration multipartConfig) {
      super(multipartConfig);
    }

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      if (isMap(resolvable) || resolvable.getParameterType() == MultiValueMap.class) {
        ResolvableType mapType = resolvable.getResolvableType().asMap();
        ResolvableType keyType = mapType.getGeneric(0);
        if (keyType.is(String.class)) {// Map<String, >
          Class<?> target;
          ResolvableType valueType = mapType.getGeneric(1);
          if (valueType.is(List.class)) { // Map<String, List<>>
            target = valueType.getGeneric(0).resolve();
          }
          else {
            target = valueType.resolve();
          }
          return supportsMultipart(target);
        }
      }
      return false;
    }

    @Override
    protected Object resolveInternal(
            RequestContext context, ResolvableMethodParameter resolvable,
            MultiValueMap<String, MultipartFile> multipartFiles) throws Throwable {

      // MultiValueMap<String, MultipartFile>
      if (resolvable.is(MultiValueMap.class)) {
        return multipartFiles;
      }

      // Map<String, List<MultipartFile>>
      if (resolvable.getResolvableType().asMap().getGeneric(1).resolve() == List.class) {
        return multipartFiles;
      }

      // Map<String, MultipartFile>
      return multipartFiles.toSingleValueMap();
    }

  }

}
