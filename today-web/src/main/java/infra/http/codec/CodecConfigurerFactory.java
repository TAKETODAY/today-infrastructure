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

package infra.http.codec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import infra.beans.BeanUtils;
import infra.core.io.ClassPathResource;
import infra.core.io.PropertiesUtils;
import infra.util.ClassUtils;

/**
 * Internal delegate for loading the default codec configurer class names.
 * Models a loose relationship with the default implementations in the support
 * package, literally only needing to know the default class name to use.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ClientCodecConfigurer#create()
 * @see ServerCodecConfigurer#create()
 * @since 4.0
 */
final class CodecConfigurerFactory {

  private static final String DEFAULT_CONFIGURERS_PATH = "CodecConfigurer.properties";
  private static final HashMap<Class<?>, Class<?>> defaultCodecConfigurers = new HashMap<>(4);

  static {
    try {
      Properties props = PropertiesUtils.loadProperties(
              new ClassPathResource(DEFAULT_CONFIGURERS_PATH, CodecConfigurerFactory.class));
      for (String ifcName : props.stringPropertyNames()) {
        String implName = props.getProperty(ifcName);
        Class<?> ifc = ClassUtils.forName(ifcName, CodecConfigurerFactory.class.getClassLoader());
        Class<?> impl = ClassUtils.forName(implName, CodecConfigurerFactory.class.getClassLoader());
        defaultCodecConfigurers.put(ifc, impl);
      }
    }
    catch (IOException | ClassNotFoundException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private CodecConfigurerFactory() { }

  @SuppressWarnings("unchecked")
  public static <T extends CodecConfigurer> T create(Class<T> ifc) {
    Class<?> impl = defaultCodecConfigurers.get(ifc);
    if (impl == null) {
      throw new IllegalStateException("No default codec configurer found for " + ifc);
    }
    return (T) BeanUtils.newInstance(impl);
  }

}
