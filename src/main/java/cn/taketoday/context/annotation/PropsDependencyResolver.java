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
package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.factory.support.AnnotationDependencyResolvingStrategy;
import cn.taketoday.beans.factory.support.DependencyDescriptor;
import cn.taketoday.beans.factory.support.DependencyResolvingContext;
import cn.taketoday.beans.factory.support.DependencyResolvingStrategy;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.util.CollectionUtils;

/**
 * Handle {@link Props} annotated on dependency
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/17 15:38
 */
@Deprecated
public class PropsDependencyResolver
        extends AnnotationDependencyResolvingStrategy implements DependencyResolvingStrategy {

  private final PropsReader propsReader;

  public PropsDependencyResolver(ApplicationContext context) {
    this.propsReader = new PropsReader(context);
  }

  public PropsDependencyResolver(PropsReader propsReader) {
    this.propsReader = propsReader;
  }

  @Override
  public boolean supports(Executable executable) {
    return executable instanceof Method method
            && method.getReturnType() == void.class
            && super.supports(executable);
  }

  @Override
  public void resolveDependency(DependencyDescriptor descriptor, DependencyResolvingContext resolvingContext) {
    // @Props on a bean (pojo) which has already created
    Props annotation = descriptor.getAnnotation(Props.class);
    if (annotation != null) {
      Object dependency = resolvingContext.getDependency();
      DefaultProps props = new DefaultProps(annotation);
      if (dependency != null) {
        // fill props even though already a dependency
        dependency = propsReader.read(props, dependency);
      }
      else {
        // process map
        if (descriptor.isMap()) {
          Properties properties = propsReader.readMap(props);
          dependency = adaptMap(properties, descriptor.getDependencyType());
        }
        else {
          dependency = propsReader.read(props, descriptor.getDependencyType());
        }
      }
      resolvingContext.setDependencyResolved(dependency);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static Map adaptMap(Map map, Class<?> type) {
    if (type != Map.class) {
      Map newMap = CollectionUtils.createMap(type, map.size());
      newMap.putAll(map);
      map = newMap;
    }
    return map;
  }

  @Override
  @SuppressWarnings({ "unchecked" })
  protected Class<? extends Annotation>[] getSupportedAnnotations() {
    return new Class[] { Props.class };
  }

}
