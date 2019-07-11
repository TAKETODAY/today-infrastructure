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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @author TODAY <br>
 *         2019-07-08 00:12
 * @since 2.1.6
 */
@FunctionalInterface
public interface Readable {

    /**
     * Get the content of the resource as input stream.
     *
     * @return input stream of {@link Resource} content
     *
     * @throws IOException
     *             If an input exception occurs
     */
    InputStream getInputStream() throws IOException;

    /**
     * Get {@link Reader}
     * 
     * @throws IOException
     *             If an input exception occurs
     */
    default Reader getReader() throws IOException {
        return new InputStreamReader(getInputStream());
    }

}
