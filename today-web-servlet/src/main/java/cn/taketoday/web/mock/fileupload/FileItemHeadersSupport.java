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
 * Interface that will indicate that {@link FileItem} or {@link FileItemStream}
 * implementations will accept the headers read for the item.
 *
 * @since FileUpload 1.2.1
 *
 * @see FileItem
 * @see FileItemStream
 */
public interface FileItemHeadersSupport {

    /**
     * Returns the collection of headers defined locally within this item.
     *
     * @return the {@link FileItemHeaders} present for this item.
     */
    FileItemHeaders getHeaders();

    /**
     * Sets the headers read from within an item.  Implementations of
     * {@link FileItem} or {@link FileItemStream} should implement this
     * interface to be able to get the raw headers found within the item
     * header block.
     *
     * @param headers the instance that holds onto the headers
     *         for this instance.
     */
    void setHeaders(FileItemHeaders headers);

}
