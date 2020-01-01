/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.context.io;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * 
 * @author TODAY <br>
 *         2019-05-14 19:55
 * @since 2.1.6
 */
public interface Resource extends Readable {

    /**
     * Get the name of the resource.
     *
     * @return name
     */
    String getName();

    /**
     * Get content length
     *
     * @return content length
     */
    long contentLength() throws IOException;

    /**
     * Get last modified
     *
     * @return last modified
     */
    long lastModified() throws IOException;

    /**
     * Get location of this resource.
     * 
     * @throws IOException
     *             if the resource is not available
     */
    URL getLocation() throws IOException;

    /**
     * Return a File handle for this resource.
     * 
     * @throws IOException
     *             in case of general resolution/reading failures
     */
    File getFile() throws IOException;

    /**
     * Whether this resource actually exists.
     */
    boolean exists();

    /**
     * Is a directory?
     * 
     * @throws IOException
     */
    boolean isDirectory() throws IOException;

    /**
     * list {@link Resource} under the directory
     * 
     * @return {@link Resource} names
     * @throws IOException
     */
    String[] list() throws IOException;

    /**
     * list {@link Resource} under the directory
     * 
     * @param filter
     *            filter {@link Resource}
     * @return {@link Resource} names
     * @throws IOException
     */
    Resource[] list(ResourceFilter filter) throws IOException;

    /**
     * Create a resource relative to this resource.
     * 
     * @param relativePath
     *            the relative path (relative to this resource)
     * @return the resource handle for the relative resource
     * @throws IOException
     *             if the relative resource cannot be determined
     */
    Resource createRelative(String relativePath) throws IOException;

}
