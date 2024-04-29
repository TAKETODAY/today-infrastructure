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
 * <p>A factory interface for creating {@link FileItem} instances. Factories
 * can provide their own custom configuration, over and above that provided
 * by the default file upload implementation.</p>
 */
public interface FileItemFactory {

  /**
   * Create a new {@link FileItem} instance from the supplied parameters and
   * any local factory configuration.
   *
   * @param fieldName The name of the form field.
   * @param contentType The content type of the form field.
   * @param isFormField {@code true} if this is a plain form field;
   * {@code false} otherwise.
   * @param fileName The name of the uploaded file, if any, as supplied
   * by the browser or other client.
   * @return The newly created file item.
   */
  FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName);

}
