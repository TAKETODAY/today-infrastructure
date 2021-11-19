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

import java.util.Properties;

import cn.taketoday.beans.dependency.DependencyInjectionPoint;
import cn.taketoday.beans.dependency.DependencyResolvingContext;
import cn.taketoday.beans.dependency.DependencyResolvingStrategy;
import cn.taketoday.beans.dependency.MapBeanDependencyResolver;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.DefaultProps;

/**
 * Handle {@link Props} annotated on dependency
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/17 15:38
 */
public class PropsDependencyResolvingStrategy implements DependencyResolvingStrategy {

  private final PropsReader propsReader;

  public PropsDependencyResolvingStrategy(ApplicationContext context) {
    this.propsReader = new PropsReader(context);
  }

  public PropsDependencyResolvingStrategy(PropsReader propsReader) {
    this.propsReader = propsReader;
  }

  @Override
  public void resolveDependency(DependencyInjectionPoint injectionPoint, DependencyResolvingContext resolvingContext) {
    // @Props on a bean (pojo) which has already created
    if (injectionPoint.isAnnotationPresent(Props.class)) {
      Object dependency = resolvingContext.getDependency();
      DefaultProps props = new DefaultProps(injectionPoint.getAnnotation(Props.class));
      if (dependency != null) {
        // fill props even though already a dependency
        dependency = propsReader.read(props, dependency);
      }
      else {
        // process map
        if (injectionPoint.isMap()) {
          Properties properties = propsReader.readMap(props);
          dependency = MapBeanDependencyResolver.adaptMap(properties, injectionPoint.getDependencyType());
        }
        else {
          dependency = propsReader.read(props, injectionPoint.getDependencyType());
        }
      }
      resolvingContext.setDependency(dependency);
    }
    // next
  }

}
