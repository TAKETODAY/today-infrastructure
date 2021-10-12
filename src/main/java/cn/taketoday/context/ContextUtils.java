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
package cn.taketoday.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * ApplicationContext Utils
 * <p>
 *
 * This class provides el, {@link Properties} loading, {@link Parameter}
 * resolving
 *
 * @author TODAY <br>
 * 2019-01-16 20:04
 */
public abstract class ContextUtils {

  // META-INF
  // ----------------------

  /**
   * Scan classes set from META-INF/xxx
   *
   * @param resource
   *         Resource file start with 'META-INF'
   *
   * @return Class set from META-INF/xxx
   *
   * @throws ApplicationContextException
   *         If any {@link IOException} occurred
   */
  public static Set<Class<?>> loadFromMetaInfo(final String resource) {
    Assert.notNull(resource, "META-INF resource must not be null");

    if (resource.startsWith("META-INF")) {

      Set<Class<?>> ret = new HashSet<>();
      ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
      Charset charset = Constant.DEFAULT_CHARSET;
      try {
        Enumeration<URL> resources = classLoader.getResources(resource);
        while (resources.hasMoreElements()) {
          URL url = resources.nextElement();
          String className = null;
          try (BufferedReader reader = //
                  new BufferedReader(new InputStreamReader(url.openStream(), charset))) {

            while ((className = reader.readLine()) != null) {
              if (StringUtils.isNotEmpty(className)) { // @since 3.0 FIX empty lines
                ret.add(classLoader.loadClass(className));
              }
            }
          }
          catch (ClassNotFoundException e) {
            throw new ConfigurationException("Class file: '" + className + "' not in " + url);
          }
        }
        return ret;
      }
      catch (IOException e) {
        throw new ApplicationContextException("Exception occurred when load from '" + resource + '\'', e);
      }
    }
    throw new ConfigurationException("Resource must start with 'META-INF'");
  }

  /**
   * Scan beans set from META-INF/xxx
   *
   * @param resource
   *         Resource file start with 'META-INF'
   *
   * @return bean set from META-INF/xxx
   *
   * @throws ApplicationContextException
   *         If any {@link IOException} occurred
   * @since 3.0
   */
  public static <T> Set<T> loadBeansFromMetaInfo(String resource) {
    return loadBeansFromMetaInfo(resource, ApplicationContextHolder.getLastStartupContext());
  }

  @SuppressWarnings("unchecked")
  public static <T> Set<T> loadBeansFromMetaInfo(String resource, BeanFactory beanFactory) {
    Set<Class<?>> classes = loadFromMetaInfo(resource);
    Set<T> ret = new HashSet<>();
    for (Class<?> aClass : classes) {
      Object obj = BeanUtils.newInstance(aClass, beanFactory);
      ret.add((T) obj);
    }
    return ret;
  }

}
