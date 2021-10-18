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
package cn.taketoday.core.io;

import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for resolving a location pattern (for example, an
 * Ant-style path pattern) into Resource objects.
 *
 * <p>
 * Can be used with any sort of location pattern (e.g.
 * "/WEB-INF/*-context.xml"): Input patterns have to match the strategy
 * implementation. This interface just specifies the conversion method rather
 * than a specific pattern format.
 *
 * <p>
 * This interface also suggests a new resource prefix "classpath*:" for all
 * matching resources from the class path. Note that the resource location is
 * expected to be a path without placeholders in this case (e.g. "/beans.xml");
 * JAR files or classes directories can contain multiple files of the same name.
 *
 * @author Juergen Hoeller
 * @author TODAY <br>
 * 2019-12-05 12:52
 * @since 2.1.7
 */
public interface ResourceLoader {

  /** Pseudo URL prefix for loading from the class path: "classpath:". */
  String CLASSPATH_URL_PREFIX = "classpath:";

  /**
   * Return a Resource handle for the specified resource location.
   * <p>
   * The handle should always be a reusable resource descriptor, allowing for
   * multiple {@link Resource#getInputStream()} calls.
   * <p>
   * <ul>
   * <li>Must support fully qualified URLs, e.g. "file:C:/test.dat".
   * <li>Must support classpath pseudo-URLs, e.g. "classpath:test.dat".
   * <li>Should support relative file paths, e.g. "WEB-INF/test.dat". (This will
   * be implementation-specific, typically provided by an ApplicationContext
   * implementation.)
   * </ul>
   * <p>
   * Note that a Resource handle does not imply an existing resource; you need to
   * invoke {@link Resource#exists} to check for existence.
   *
   * @param location the resource location
   * @return a corresponding Resource handle (never {@code null})
   * @see #CLASSPATH_URL_PREFIX
   * @see Resource#exists()
   * @see Resource#getInputStream()
   */
  Resource getResource(String location);

  /**
   * Expose the {@link ClassLoader} used by this {@code ResourceLoader}.
   * <p>Clients which need to access the {@code ClassLoader} directly can do so
   * in a uniform manner with the {@code ResourceLoader}, rather than relying
   * on the thread context {@code ClassLoader}.
   *
   * @return the {@code ClassLoader}
   * (only {@code null} if even the system {@code ClassLoader} isn't accessible)
   * @see cn.taketoday.util.ClassUtils#getDefaultClassLoader()
   * @see cn.taketoday.util.ClassUtils#forName(String, ClassLoader)
   */
  @Nullable
  ClassLoader getClassLoader();

}
