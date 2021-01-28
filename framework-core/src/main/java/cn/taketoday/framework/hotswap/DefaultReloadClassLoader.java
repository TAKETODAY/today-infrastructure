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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.framework.hotswap;

import java.net.URL;
import java.net.URLClassLoader;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;

/**
 *
 * @author TODAY <br>
 *         2019-06-12 10:03
 */
public class DefaultReloadClassLoader extends URLClassLoader {

  private static final Logger log = LoggerFactory.getLogger(DefaultReloadClassLoader.class);

  private final ClassLoader parent;
  private final DefaultClassResolver hotSwapResolver;

  static {
    registerAsParallelCapable();
  }

  public DefaultReloadClassLoader(URL[] urls, ClassLoader parent, DefaultClassResolver hotSwapResolver) {
    super(urls, parent);
    this.parent = parent;
    this.hotSwapResolver = hotSwapResolver;
  }

  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

    synchronized (getClassLoadingLock(name)) {

      Class<?> c = findLoadedClass(name);
      if (c != null) {
        return c;
      }

      if (hotSwapResolver.isHotSwapClass(name)) {
        if (log.isTraceEnabled()) {
          log.trace("Hot Swap Class: [{}]", name);
        }
        c = super.findClass(name);
        if (c != null) {
          if (resolve) {
            resolveClass(c);
          }
          return c;
        }
      }
      return parent.loadClass(name);
    }
  }

}
