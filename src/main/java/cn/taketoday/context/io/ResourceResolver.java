/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.context.io;

import java.io.IOException;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Constant;

/**
 * Strategy interface for resolving a location pattern (for example, an
 * Ant-style path pattern) into Resource objects.
 *
 * <p>
 * This is an extension to the
 * {@link ResourceLoader} interface. A passed-in
 * ResourceLoader (for example, an
 * {@link ApplicationContext} passed in via
 * {@link ResourceLoaderAware} when running in a
 * context) can be checked whether it implements this extended interface too.
 *
 * <p>
 * {@link PathMatchingResourcePatternResolver} is a standalone implementation
 * that is usable outside an ApplicationContext, also used by
 * {@link ResourceArrayPropertyEditor} for populating Resource array bean
 * properties.
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
 * @since 2.1.7
 * 
 * @author TODAY <br>
 *         2019-12-05 12:52
 */
public interface ResourceResolver {

    /** Pseudo URL prefix for loading from the class path: "classpath:". */
    String CLASSPATH_URL_PREFIX = Constant.CLASS_PATH_PREFIX;

    /**
     * Pseudo URL prefix for all matching resources from the class path:
     * "classpath*:" This differs from ResourceLoader's classpath URL prefix in that
     * it retrieves all matching resources for a given name (e.g. "/beans.xml"), for
     * example in the root of all deployed JAR files.
     */
    String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

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
     * @param location
     *            the resource location
     * @return a corresponding Resource handle (never {@code null})
     * @see #CLASSPATH_URL_PREFIX
     * @see Resource#exists()
     * @see Resource#getInputStream()
     */
    Resource getResource(String location);

    /**
     * Expose the ClassLoader used by this ResourceLoader.
     * <p>
     * Clients which need to access the ClassLoader directly can do so in a uniform
     * manner with the ResourceLoader, rather than relying on the thread context
     * ClassLoader.
     * 
     * @return the ClassLoader (only {@code null} if even the system ClassLoader
     *         isn't accessible)
     */
    ClassLoader getClassLoader();

    /**
     * Resolve the given location pattern into Resource objects.
     * <p>
     * Overlapping resource entries that point to the same physical resource should
     * be avoided, as far as possible. The result should have set semantics.
     * 
     * @param locationPattern
     *            the location pattern to resolve
     * @return the corresponding Resource objects
     * @throws IOException
     *             in case of I/O errors
     */
    Resource[] getResources(String locationPattern) throws IOException;

}
