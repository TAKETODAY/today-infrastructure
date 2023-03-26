/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;

/**
 * A factory delegate for resolving {@link MediaType} objects
 * from {@link Resource} handles or filenames.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/15 14:33
 */
public abstract class MediaTypeFactory {

  private static final String MIME_TYPES_FILE_NAME = "/cn/taketoday/http/mime.types";

  private static final MultiValueMap<String, MediaType> fileExtensionToMediaTypes = parseMimeTypes();

  private MediaTypeFactory() { }

  /**
   * Parse the {@code mime.types} file found in the resources. Format is:
   * <code>
   * # comments begin with a '#'<br>
   * # the format is &lt;mime type> &lt;space separated file extensions><br>
   * # for example:<br>
   * text/plain    txt text<br>
   * # this would map file.txt and file.text to<br>
   * # the mime type "text/plain"<br>
   * </code>
   *
   * @return a multi-value map, mapping media types to file extensions.
   */
  private static MultiValueMap<String, MediaType> parseMimeTypes() {
    InputStream is = MediaTypeFactory.class.getResourceAsStream(MIME_TYPES_FILE_NAME);
    if (is == null) {
      throw new IllegalStateException(MIME_TYPES_FILE_NAME + " not found in classpath");
    }
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII))) {
      MultiValueMap<String, MediaType> result = MultiValueMap.fromLinkedHashMap();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isEmpty() || line.charAt(0) == '#') {
          continue;
        }
        String[] tokens = StringUtils.tokenizeToStringArray(line, " \t\n\r\f");
        MediaType mediaType = MediaType.parseMediaType(tokens[0]);
        for (int i = 1; i < tokens.length; i++) {
          String fileExtension = tokens[i].toLowerCase(Locale.ENGLISH);
          result.add(fileExtension, mediaType);
        }
      }
      return result;
    }
    catch (IOException ex) {
      throw new IllegalStateException("Could not read " + MIME_TYPES_FILE_NAME, ex);
    }
  }

  /**
   * Determine a media type for the given resource, if possible.
   *
   * @param resource the resource to introspect
   * @return the corresponding media type, or {@code null} if none found
   */
  public static Optional<MediaType> getMediaType(@Nullable Resource resource) {
    return Optional.ofNullable(resource)
            .map(Resource::getName)
            .flatMap(MediaTypeFactory::getMediaType);
  }

  /**
   * Determine a media type for the given file name, if possible.
   *
   * @param filename the file name plus extension
   * @return the corresponding media type, or {@code null} if none found
   */
  public static Optional<MediaType> getMediaType(@Nullable String filename) {
    return getMediaTypes(filename).stream().findFirst();
  }

  /**
   * Determine the media types for the given file name, if possible.
   *
   * @param filename the file name plus extension
   * @return the corresponding media types, or an empty list if none found
   */
  public static List<MediaType> getMediaTypes(@Nullable String filename) {
    List<MediaType> mediaTypes = null;
    String ext = StringUtils.getFilenameExtension(filename);
    if (ext != null) {
      mediaTypes = fileExtensionToMediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
    }
    return mediaTypes != null ? mediaTypes : Collections.emptyList();
  }

}
