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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author TODAY <br>
 *         2019-05-14 22:32
 * @since 2.1.6
 */
public abstract class AbstractResource implements Resource {

    @Override
    public String getName() {
        try {
            return getFile().getName();
        }
        catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean exists() {

        try (InputStream inputStream = getInputStream()) {
            return inputStream != null;
        }
        catch (IOException e) {
            return false;
        }
    }

    @Override
    public URL getLocation() throws IOException {
        throw new FileNotFoundException(getName() + " cannot be resolved to URL");
    }

    @Override
    public File getFile() throws IOException {
        throw new FileNotFoundException(getName() + " cannot be resolved to absolute file path");
    }

    @Override
    public boolean isDirectory() throws IOException {
        return getFile().isDirectory();
    }

    @Override
    public String[] list() throws IOException {
        return getFile().list();
    }

    @Override
    public long contentLength() throws IOException {
        return getFile().length();
    }

    @Override
    public long lastModified() throws IOException {
        return getFile().lastModified();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        throw new FileNotFoundException(relativePath + " cannot be resolved relative file path");
    }

    @Override
    public String toString() {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("{\n\t\"name\":\"");
            builder.append(getName());
            builder.append("\",\n\t\"exists\":\"");
            builder.append(exists());
            builder.append("\",\n\t\"location\":\"");
            builder.append(getLocation());
            builder.append("\",\n\t\"file\":\"");
            builder.append(getFile());
            builder.append("\"\n}");
            return builder.toString();
        }
        catch (IOException e) {
            return super.toString();
        }
    }

}
