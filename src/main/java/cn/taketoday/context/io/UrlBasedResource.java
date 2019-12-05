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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import cn.taketoday.context.utils.ResourceUtils;
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

    /**
     * This implementation opens an InputStream for the given URL.
     * <p>
     * It sets the {@code useCaches} flag to {@code false}, mainly to avoid jar file
     * locking on Windows.
     * 
     * @see java.net.URL#openConnection()
     * @see java.net.URLConnection#setUseCaches(boolean)
     * @see java.net.URLConnection#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws IOException {
        final URLConnection con = this.url.openConnection();
        ResourceUtils.useCachesIfNecessary(con);
        try {
            return con.getInputStream();
        }
        catch (IOException ex) {
            // Close the HTTP connection (if applicable).
            if (con instanceof HttpURLConnection) {
                ((HttpURLConnection) con).disconnect();
            }
            throw ex;
        }
    }

    @Override
    public URL getLocation() {
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

    /**
     * This implementation creates a {@code UrlResource}, delegating to
     * {@link #createRelativeURL(String)} for adapting the relative path.
     * 
     * @see #createRelativeURL(String)
     */
    @Override
    public UrlBasedResource createRelative(String relativePath) throws IOException {
        return new UrlBasedResource(createRelativeURL(relativePath));
    }

    /**
     * This delegate creates a {@code java.net.URL}, applying the given path
     * relative to the path of the underlying URL of this resource descriptor. A
     * leading slash will get dropped; a "#" symbol will get encoded.
     * 
     * @see #createRelative(String)
     * @see java.net.URL#URL(java.net.URL, String)
     */
    protected URL createRelativeURL(String relativePath) throws MalformedURLException {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        // # can appear in filenames, java.net.URL should not treat it as a fragment
        relativePath = StringUtils.replace(relativePath, "#", "%23");
        // Use the URL constructor for applying the relative path as a URL spec
        return new URL(this.url, relativePath);
    }

    @Override
    public String toString() {
        return "UrlBasedResource: ".concat(url.toString());
    }

}
