/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.context.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import cn.taketoday.context.Constant;
import cn.taketoday.context.io.ClassPathResource;
import cn.taketoday.context.io.FileBasedResource;
import cn.taketoday.context.io.JarEntryResource;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.io.UrlBasedResource;

/**
 * @author TODAY <br>
 *         2019-05-15 13:37
 * @since 2.1.6
 */
public abstract class ResourceUtils {

    /**
     * Get {@link Resource} with given location
     * 
     * @param location
     *            resource location
     * @throws IOException
     *             if any IO exception occurred
     */
    public static Resource getResource(final String location) {

        // fix location is empty
        if (StringUtils.isEmpty(location)) {
            return new ClassPathResource(Constant.BLANK);
        }

        if (location.startsWith(Constant.CLASS_PATH_PREFIX)) {
            final String path = location.substring(Constant.CLASS_PATH_PREFIX.length());
            if (path.charAt(0) == Constant.PATH_SEPARATOR) {
                return new ClassPathResource(path.substring(1));
            }
            return new ClassPathResource(path);
        }

        try {
            return getResource(new URL(location));
        }
        catch (IOException e) {
            return new ClassPathResource(location);
        }
    }

    public static Resource getResource(final URL url) {
        final String protocol = url.getProtocol();
        if (Constant.PROTOCOL_FILE.equals(protocol)) {
            return new FileBasedResource(url.getPath());
        }
        if (Constant.PROTOCOL_JAR.equals(protocol)) {
            return new JarEntryResource(url);
        }
        return new UrlBasedResource(url);
    }

    /**
     * Get {@link Resource} from a file
     * 
     * @param file
     *            source
     * @return a {@link FileBasedResource}
     * @throws IOException
     */
    public static Resource getResource(final File file) {
        return new FileBasedResource(file);
    }

    /**
     * Create a new relative path from a file path.
     * <p>
     * Note: When building relative path, it makes a difference whether the
     * specified resource base path here ends with a slash or not. In the case of
     * "C:/dir1/", relative paths will be built underneath that root: e.g. relative
     * path "dir2" -> "C:/dir1/dir2". In the case of "C:/dir1", relative paths will
     * apply at the same directory level: relative path "dir2" -> "C:/dir2".
     * 
     */
    public static String getRelativePath(final String path, final String relativePath) {

        final int separatorIndex = path.lastIndexOf(Constant.PATH_SEPARATOR);

        if (separatorIndex > 0) {

            final StringBuilder newPath = new StringBuilder(path.substring(0, separatorIndex));

            if (relativePath.charAt(0) != Constant.PATH_SEPARATOR) {
                newPath.append(Constant.PATH_SEPARATOR);
            }
            return newPath.append(relativePath).toString();
        }
        return relativePath;
    }

    // 

    /**
     * Determine whether the given URL points to a resource in the file system, i.e.
     * has protocol "file", "vfsfile" or "vfs".
     * 
     * @param url
     *            the URL to check
     * @return whether the URL has been identified as a file system URL
     */
    public static boolean isFileURL(final URL url) {
        String protocol = url.getProtocol();
        return (Constant.URL_PROTOCOL_FILE.equals(protocol) || Constant.URL_PROTOCOL_VFSFILE.equals(protocol));
    }

    /**
     * Determine whether the given URL points to a resource in a jar file. i.e. has
     * protocol "jar", "war, ""zip", "vfszip" or "wsjar".
     * 
     * @param url
     *            the URL to check
     * @return whether the URL has been identified as a JAR URL
     */
    public static boolean isJarURL(final URL url) {
        final String protocol = url.getProtocol();
        return (Constant.URL_PROTOCOL_JAR.equals(protocol) || Constant.URL_PROTOCOL_WAR.equals(protocol) ||
                Constant.URL_PROTOCOL_ZIP.equals(protocol) || Constant.URL_PROTOCOL_VFSZIP.equals(protocol) ||
                Constant.URL_PROTOCOL_WSJAR.equals(protocol));
    }

    /**
     * Determine whether the given URL points to a jar file itself, that is, has
     * protocol "file" and ends with the ".jar" extension.
     * 
     * @param url
     *            the URL to check
     * @return whether the URL has been identified as a JAR file URL
     * @since 4.1
     */
    public static boolean isJarFileURL(final URL url) {
        return (Constant.URL_PROTOCOL_FILE.equals(url.getProtocol()) &&
                url.getPath().toLowerCase().endsWith(Constant.JAR_FILE_EXTENSION));
    }

    /**
     * Extract the URL for the actual jar file from the given URL (which may point
     * to a resource in a jar file or to a jar file itself).
     * 
     * @param jarUrl
     *            the original URL
     * @return the URL for the actual jar file
     * @throws MalformedURLException
     *             if no valid jar file URL could be extracted
     */
    public static URL extractJarFileURL(final URL jarUrl) throws MalformedURLException {
        String urlFile = jarUrl.getFile();
        int separatorIndex = urlFile.indexOf(Constant.JAR_URL_SEPARATOR);
        if (separatorIndex != -1) {
            String jarFile = urlFile.substring(0, separatorIndex);
            try {
                return new URL(jarFile);
            }
            catch (MalformedURLException ex) {
                // Probably no protocol in original jar URL, like "jar:C:/mypath/myjar.jar".
                // This usually indicates that the jar file resides in the file system.
                if (!jarFile.startsWith("/")) {
                    jarFile = "/" + jarFile;
                }
                return new URL(Constant.FILE_URL_PREFIX + jarFile);
            }
        }
        else {
            return jarUrl;
        }
    }

    /**
     * Set the {@link URLConnection#setUseCaches "useCaches"} flag on the given
     * connection, preferring {@code false} but leaving the flag at {@code true} for
     * JNLP based resources.
     * 
     * @param con
     *            the URLConnection to set the flag on
     */
    public static void useCachesIfNecessary(final URLConnection con) {
        con.setUseCaches(con.getClass().getSimpleName().startsWith("JNLP"));
    }

    /**
     * Create a URI instance for the given URL, replacing spaces with "%20" URI
     * encoding first.
     * 
     * @param url
     *            the URL to convert into a URI instance
     * @return the URI instance
     * @throws URISyntaxException
     *             if the URL wasn't a valid URI
     * @see java.net.URL#toURI()
     */
    public static URI toURI(final URL url) throws URISyntaxException {
        return toURI(url.toString());
    }

    /**
     * Create a URI instance for the given location String, replacing spaces with
     * "%20" URI encoding first.
     * 
     * @param location
     *            the location String to convert into a URI instance
     * @return the URI instance
     * @throws URISyntaxException
     *             if the location wasn't a valid URI
     */
    public static URI toURI(final String location) throws URISyntaxException {
        return new URI(StringUtils.replace(location, " ", "%20"));
    }

    /**
     * Return whether the given resource location is a URL: either a special
     * "classpath" pseudo URL or a standard URL.
     * 
     * @param resourceLocation
     *            the location String to check
     * @return whether the location qualifies as a URL
     * @see #CLASSPATH_URL_PREFIX
     * @see java.net.URL
     */
    public static boolean isUrl(String resourceLocation) {
        if (resourceLocation == null) {
            return false;
        }
        if (resourceLocation.startsWith(Constant.CLASS_PATH_PREFIX)) {
            return true;
        }
        try {
            new URL(resourceLocation);
            return true;
        }
        catch (MalformedURLException ex) {
            return false;
        }
    }
}
