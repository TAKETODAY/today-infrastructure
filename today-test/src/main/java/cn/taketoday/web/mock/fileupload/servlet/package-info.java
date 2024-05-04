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
 * <p>
 * An implementation of
 * {@link cn.taketoday.web.mock.fileupload.FileUpload FileUpload}
 * for use in servlets conforming to JSR 53. This implementation requires
 * only access to the servlet's current {@code HttpServletRequest}
 * instance, and a suitable
 * {@link cn.taketoday.web.mock.fileupload.FileItemFactory FileItemFactory}
 * implementation, such as
 * {@link cn.taketoday.web.mock.fileupload.disk.DiskFileItemFactory DiskFileItemFactory}.
 * </p>
 * <p>
 * The following code fragment demonstrates typical usage.
 * </p>
 * <pre>
 *        DiskFileItemFactory factory = new DiskFileItemFactory();
 *        // Configure the factory here, if desired.
 *        ServletFileUpload upload = new ServletFileUpload(factory);
 *        // Configure the uploader here, if desired.
 *        List fileItems = upload.parseRequest(request);
 * </pre>
 * <p>
 * Please see the FileUpload
 * <a href="https://commons.apache.org/fileupload/using.html" target="_top">User Guide</a>
 * for further details and examples of how to use this package.
 * </p>
 */
package cn.taketoday.web.mock.fileupload.servlet;
