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
package cn.taketoday.context.utils;

import java.io.IOException;
import java.net.URL;

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
	 * @param location
	 * @return
	 * @throws IOException
	 */
	public static Resource getResource(String location) throws IOException {

		if (location.charAt(0) == Constant.PATH_SEPARATOR) {
			return new ClassPathResource(location.substring(1));
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
			return new FileBasedResource(location);
		}
	}

	public static Resource getResource(URL url) throws IOException {

		switch (url.getProtocol()) //@off
		{
			case Constant.PROTOCOL_FILE : 	return new FileBasedResource(url.getPath());
			case Constant.PROTOCOL_JAR :	return new JarEntryResource(url.getPath());
			default:						return new UrlBasedResource(url); //@on
		}
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
	public static String getRelativePath(String path, String relativePath) {

		final int separatorIndex = path.lastIndexOf(Constant.PATH_SEPARATOR);

		if (separatorIndex != -1) {

			final StringBuilder newPath = new StringBuilder(path.substring(0, separatorIndex));

			if (relativePath.charAt(0) != Constant.PATH_SEPARATOR) {
				newPath.append(Constant.PATH_SEPARATOR);
			}
			return newPath.append(relativePath).toString();
		}
		return relativePath;
	}

}
