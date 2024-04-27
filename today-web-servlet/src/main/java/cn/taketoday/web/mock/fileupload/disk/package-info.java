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

/**
 *    <p>
 *      A disk-based implementation of the
 *      {@link cn.taketoday.web.mock.fileupload.FileItem FileItem}
 *      interface. This implementation retains smaller items in memory, while
 *      writing larger ones to disk. The threshold between these two is
 *      configurable, as is the location of files that are written to disk.
 *    </p>
 *    <p>
 *      In typical usage, an instance of
 *      {@link cn.taketoday.web.mock.fileupload.disk.DiskFileItemFactory DiskFileItemFactory}
 *      would be created, configured, and then passed to a
 *      {@link cn.taketoday.web.mock.fileupload.FileUpload FileUpload}
 *      implementation.
 *    </p>
 *    <p>
 *      The following code fragment demonstrates this usage.
 *    </p>
 * <pre>
 *        DiskFileItemFactory factory = new DiskFileItemFactory();
 *        // maximum size that will be stored in memory
 *        factory.setSizeThreshold(4096);
 *        // the location for saving data that is larger than getSizeThreshold()
 *        factory.setRepository(new File("/tmp"));
 *
 *        ServletFileUpload upload = new ServletFileUpload(factory);
 * </pre>
 *    <p>
 *      Please see the FileUpload
 *      <a href="https://commons.apache.org/fileupload/using.html" target="_top">User Guide</a>
 *      for further details and examples of how to use this package.
 *    </p>
 */
package cn.taketoday.web.mock.fileupload.disk;
