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
package cn.taketoday.web.mock.fileupload.impl;

import cn.taketoday.web.mock.fileupload.FileUploadException;

/**
 * This exception is thrown, if a requests permitted size
 * is exceeded.
 */
public abstract class SizeException extends FileUploadException {

    /**
     * Serial version UID, being used, if serialized.
     */
    private static final long serialVersionUID = -8776225574705254126L;

    /**
     * The actual size of the request.
     */
    private final long actual;

    /**
     * The maximum permitted size of the request.
     */
    private final long permitted;

    /**
     * Creates a new instance.
     *
     * @param message The detail message.
     * @param actual The actual number of bytes in the request.
     * @param permitted The requests size limit, in bytes.
     */
    protected SizeException(final String message, final long actual, final long permitted) {
        super(message);
        this.actual = actual;
        this.permitted = permitted;
    }

    /**
     * Retrieves the actual size of the request.
     *
     * @return The actual size of the request.
     * @since FileUpload 1.3
     */
    public long getActualSize() {
        return actual;
    }

    /**
     * Retrieves the permitted size of the request.
     *
     * @return The permitted size of the request.
     * @since FileUpload 1.3
     */
    public long getPermittedSize() {
        return permitted;
    }

}