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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import cn.taketoday.context.Constant;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY <br>
 *         2019-05-14 22:26
 * @since 2.1.6
 */
public class UrlBasedResource extends AbstractResource {

    /**
     * Original URL, used for actual access.
     */
    private final URL url;

    public UrlBasedResource(URL url) {
        this.url = url;
    }

    public UrlBasedResource(URI uri) throws MalformedURLException {
        this.url = uri.toURL();
    }

    public UrlBasedResource(String path) throws MalformedURLException {
        this.url = new URL(path);
    }

    public UrlBasedResource(String protocol, String location) throws URISyntaxException, MalformedURLException {
        this.url = new URI(protocol, location, null).toURL();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.url.openStream();
    }

    @Override
    public URL getLocation() throws IOException {
        return url;
    }

    @Override
    public File getFile() {
        return new File(url.getPath());
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof UrlBasedResource && this.url.equals(((UrlBasedResource) other).url)));
    }

    @Override
    public int hashCode() {
        return this.url.hashCode();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        if (StringUtils.isEmpty(relativePath)) {
            return this;
        }
        if (relativePath.charAt(0) == Constant.PATH_SEPARATOR) {
            return new UrlBasedResource(new URL(this.url, relativePath.substring(1)));
        }
        return new UrlBasedResource(new URL(this.url, relativePath));
    }
}
