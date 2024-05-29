/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.persistence.query;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.sql.DataSource;

import cn.taketoday.context.ResourceLoaderAware;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/24 16:13
 */
public class MapperFactory implements ResourceLoaderAware {

  private ResourceLoader resourceLoader = new DefaultResourceLoader();

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> mapperClass) {
    return (T) Proxy.newProxyInstance(mapperClass.getClassLoader(),
            new Class[] { mapperClass }, new MapperHandler(mapperClass));
  }

  class MapperHandler implements InvocationHandler {

    private final Class<?> mapperClass;

    private DataSource dataSource;

    public MapperHandler(Class<?> mapperClass) {
      this.mapperClass = mapperClass;
      var annotation = MergedAnnotations.from(mapperClass).get(MapperLocation.class);
      if (annotation.isPresent()) {
        String stringValue = annotation.getStringValue();
        Resource resource = resourceLoader.getResource(stringValue);
        Assert.state(resource.exists(), "Mapper resource not found");





      }
      else {
        // TODO
      }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String name = method.getName();

      return null;
    }

  }

}
