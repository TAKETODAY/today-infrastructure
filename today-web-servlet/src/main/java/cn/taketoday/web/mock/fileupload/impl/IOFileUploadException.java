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

import java.io.IOException;

import cn.taketoday.web.mock.fileupload.FileUploadException;

/**
 * Thrown to indicate an IOException.
 */
public class IOFileUploadException extends FileUploadException {

    /**
     * The exceptions UID, for serializing an instance.
     */
    private static final long serialVersionUID = 1749796615868477269L;

    /**
     * The exceptions cause; we overwrite the parent
     * classes field, which is available since Java
     * 1.4 only.
     */
    private final IOException cause;

    /**
     * Creates a new instance with the given cause.
     *
     * @param pMsg The detail message.
     * @param pException The exceptions cause.
     */
    public IOFileUploadException(final String pMsg, final IOException pException) {
        super(pMsg);
        cause = pException;
    }

    /**
     * Returns the exceptions cause.
     *
     * @return The exceptions cause, if any, or null.
     */
    @SuppressWarnings("sync-override") // Field is final
    @Override
    public Throwable getCause() {
        return cause;
    }

}