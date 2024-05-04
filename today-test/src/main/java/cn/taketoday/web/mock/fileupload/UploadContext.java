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
package cn.taketoday.web.mock.fileupload;

/**
 * Enhanced access to the request information needed for file uploads,
 * which fixes the Content Length data access in {@link RequestContext}.
 *
 * The reason of introducing this new interface is just for backward compatibility
 * and it might vanish for a refactored 2.x version moving the new method into
 * RequestContext again.
 *
 * @since FileUpload 1.3
 */
public interface UploadContext extends RequestContext {

  /**
   * Retrieve the content length of the request.
   *
   * @return The content length of the request.
   * @since FileUpload 1.3
   */
  long contentLength();

}
