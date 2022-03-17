/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.support;

import cn.taketoday.beans.BeansException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;

/**
 * Standalone XML application context, taking the context definition files
 * from the file system or from URLs, interpreting plain paths as relative
 * file system locations (e.g. "mydir/myfile.txt"). Useful for test harnesses
 * as well as for standalone environments.
 *
 * <p><b>NOTE:</b> Plain paths will always be interpreted as relative
 * to the current VM working directory, even if they start with a slash.
 * (This is consistent with the semantics in a Servlet container.)
 * <b>Use an explicit "file:" prefix to enforce an absolute file path.</b>
 *
 * <p>The config location defaults can be overridden via {@link #getConfigLocations},
 * Config locations can either denote concrete files like "/myfiles/context.xml"
 * or Ant-style patterns like "/myfiles/*-context.xml" (see the
 * {@link cn.taketoday.core.AntPathMatcher} javadoc for pattern details).
 *
 * <p>Note: In case of multiple config locations, later bean definitions will
 * override ones defined in earlier loaded files. This can be leveraged to
 * deliberately override certain bean definitions via an extra XML file.
 *
 * <p><b>This is a simple, one-stop shop convenience ApplicationContext.
 * Consider using the {@link GenericApplicationContext} class in combination
 * with an {@link cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader}
 * for more flexible context setup.</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getResource
 * @see #getResourceByPath
 * @see GenericApplicationContext
 * @since 4.0 2022/3/6 22:14
 */
public class FileSystemXmlApplicationContext extends AbstractXmlApplicationContext {

  /**
   * Create a new FileSystemXmlApplicationContext for bean-style configuration.
   *
   * @see #setConfigLocation
   * @see #setConfigLocations
   * @see #afterPropertiesSet()
   */
  public FileSystemXmlApplicationContext() { }

  /**
   * Create a new FileSystemXmlApplicationContext for bean-style configuration.
   *
   * @param parent the parent context
   * @see #setConfigLocation
   * @see #setConfigLocations
   * @see #afterPropertiesSet()
   */
  public FileSystemXmlApplicationContext(ApplicationContext parent) {
    super(parent);
  }

  /**
   * Create a new FileSystemXmlApplicationContext, loading the definitions
   * from the given XML file and automatically refreshing the context.
   *
   * @param configLocation file path
   * @throws BeansException if context creation failed
   */
  public FileSystemXmlApplicationContext(String configLocation) throws BeansException {
    this(new String[] { configLocation }, true, null);
  }

  /**
   * Create a new FileSystemXmlApplicationContext, loading the definitions
   * from the given XML files and automatically refreshing the context.
   *
   * @param configLocations array of file paths
   * @throws BeansException if context creation failed
   */
  public FileSystemXmlApplicationContext(String... configLocations) throws BeansException {
    this(configLocations, true, null);
  }

  /**
   * Create a new FileSystemXmlApplicationContext with the given parent,
   * loading the definitions from the given XML files and automatically
   * refreshing the context.
   *
   * @param configLocations array of file paths
   * @param parent the parent context
   * @throws BeansException if context creation failed
   */
  public FileSystemXmlApplicationContext(String[] configLocations, ApplicationContext parent) throws BeansException {
    this(configLocations, true, parent);
  }

  /**
   * Create a new FileSystemXmlApplicationContext, loading the definitions
   * from the given XML files.
   *
   * @param configLocations array of file paths
   * @param refresh whether to automatically refresh the context,
   * loading all bean definitions and creating all singletons.
   * Alternatively, call refresh manually after further configuring the context.
   * @throws BeansException if context creation failed
   * @see #refresh()
   */
  public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
    this(configLocations, refresh, null);
  }

  /**
   * Create a new FileSystemXmlApplicationContext with the given parent,
   * loading the definitions from the given XML files.
   *
   * @param configLocations array of file paths
   * @param refresh whether to automatically refresh the context,
   * loading all bean definitions and creating all singletons.
   * Alternatively, call refresh manually after further configuring the context.
   * @param parent the parent context
   * @throws BeansException if context creation failed
   * @see #refresh()
   */
  public FileSystemXmlApplicationContext(
          String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
          throws BeansException {

    super(parent);
    setConfigLocations(configLocations);
    if (refresh) {
      refresh();
    }
  }

  /**
   * Resolve resource paths as file system paths.
   * <p>Note: Even if a given path starts with a slash, it will get
   * interpreted as relative to the current VM working directory.
   * This is consistent with the semantics in a Servlet container.
   *
   * @param path the path to the resource
   * @return the Resource handle
   * @see cn.taketoday.web.context.support.XmlWebApplicationContext#getResourceByPath
   */
  @Override
  protected Resource getResourceByPath(String path) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    return new FileBasedResource(path);
  }

}
