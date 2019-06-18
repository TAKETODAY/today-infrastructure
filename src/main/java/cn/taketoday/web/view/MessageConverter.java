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
package cn.taketoday.web.view;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author TODAY <br>
 *         2019-06-18 14:39
 */
public interface MessageConverter<T> {

    /**
     * Read an object of the given type from the given input message, and returns
     * it.
     * 
     * @param clazz
     *            the type of object to return. This type must have previously been
     *            passed to the {@link #canRead canRead} method of this interface,
     *            which must have returned {@code true}.
     * @param inputMessage
     *            the HTTP input message to read from
     * @return the converted object
     * @throws IOException
     *             in case of I/O errors
     */
    T read(Class<? extends T> targetType, String body) throws IOException;

    /**
     * Write an given object to the given output message.
     * 
     * @param t
     *            the object to write to the output message. The type of this object
     *            must have previously been passed to the {@link #canWrite canWrite}
     *            method of this interface, which must have returned {@code true}.
     * @param contentType
     *            the content type to use when writing. May be {@code null} to
     *            indicate that the default content type of the converter must be
     *            used. If not {@code null}, this media type must have previously
     *            been passed to the {@link #canWrite canWrite} method of this
     *            interface, which must have returned {@code true}.
     * @param outputMessage
     *            the message to write to
     * @throws IOException
     *             in case of I/O errors
     */
    void write(T t, OutputStream outputMessage) throws IOException;
}
