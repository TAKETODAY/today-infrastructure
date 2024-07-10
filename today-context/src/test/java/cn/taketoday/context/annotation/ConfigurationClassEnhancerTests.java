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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureClassLoader;

import cn.taketoday.core.SmartClassLoader;
import cn.taketoday.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/6 17:31
 */
class ConfigurationClassEnhancerTests {

  @Test
  void enhanceReloadedClass() throws Exception {
    ConfigurationClassEnhancer configurationClassEnhancer = new ConfigurationClassEnhancer();
    ClassLoader parentClassLoader = getClass().getClassLoader();
    CustomClassLoader classLoader = new CustomClassLoader(parentClassLoader);
    Class<?> myClass = parentClassLoader.loadClass(MyConfig.class.getName());
    configurationClassEnhancer.enhance(myClass, parentClassLoader);
    Class<?> myReloadedClass = classLoader.loadClass(MyConfig.class.getName());
    Class<?> enhancedReloadedClass = configurationClassEnhancer.enhance(myReloadedClass, classLoader);
    assertThat(enhancedReloadedClass.getClassLoader()).isEqualTo(classLoader);
  }

  @Configuration
  static class MyConfig {

    @Bean
    public String myBean() {
      return "bean";
    }
  }

  static class CustomClassLoader extends SecureClassLoader implements SmartClassLoader {

    CustomClassLoader(ClassLoader parent) {
      super(parent);
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (name.contains("MyConfig")) {
        String path = name.replace('.', '/').concat(".class");
        try (InputStream in = super.getResourceAsStream(path)) {
          byte[] bytes = StreamUtils.copyToByteArray(in);
          if (bytes.length > 0) {
            return defineClass(name, bytes, 0, bytes.length);
          }
        }
        catch (IOException ex) {
          throw new IllegalStateException(ex);
        }
      }
      return super.loadClass(name, resolve);
    }

    @Override
    public boolean isClassReloadable(Class<?> clazz) {
      return clazz.getName().contains("MyConfig");
    }
  }

}