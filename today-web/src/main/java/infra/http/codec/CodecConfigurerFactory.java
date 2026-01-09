/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
