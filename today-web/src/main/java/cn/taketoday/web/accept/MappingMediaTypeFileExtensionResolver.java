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

package cn.taketoday.web.accept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.taketoday.lang.Nullable;
import cn.taketoday.http.MediaType;

/**
 * An implementation of {@code MediaTypeFileExtensionResolver} that maintains
 * lookups between file extensions and MediaTypes in both directions.
 *
 * <p>Initially created with a map of file extensions and media types.
 * Subsequently subclasses can use {@link #addMapping} to add more mappings.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MappingMediaTypeFileExtensionResolver implements MediaTypeFileExtensionResolver {

  private final CopyOnWriteArrayList<String> allFileExtensions = new CopyOnWriteArrayList<>();
  private final ConcurrentHashMap<String, MediaType> mediaTypes = new ConcurrentHashMap<>(64);
  private final ConcurrentHashMap<MediaType, List<String>> fileExtensions = new ConcurrentHashMap<>(64);

  /**
   * Create an instance with the given map of file extensions and media types.
   */
  public MappingMediaTypeFileExtensionResolver(@Nullable Map<String, MediaType> mediaTypes) {
    if (mediaTypes != null) {
      HashSet<String> allFileExtensions = new HashSet<>(mediaTypes.size());
      for (Map.Entry<String, MediaType> entry : mediaTypes.entrySet()) {
        String extension = entry.getKey();
        MediaType mediaType = entry.getValue();
        String lowerCaseExtension = extension.toLowerCase(Locale.ENGLISH);
        this.mediaTypes.put(lowerCaseExtension, mediaType);
        addFileExtension(mediaType, lowerCaseExtension);
        allFileExtensions.add(lowerCaseExtension);
      }
      this.allFileExtensions.addAll(allFileExtensions);
    }
  }

  public Map<String, MediaType> getMediaTypes() {
    return this.mediaTypes;
  }

  protected List<MediaType> getAllMediaTypes() {
    return new ArrayList<>(this.mediaTypes.values());
  }

  /**
   * Map an extension to a MediaType. Ignore if extension already mapped.
   */
  protected void addMapping(String extension, MediaType mediaType) {
    MediaType previous = this.mediaTypes.putIfAbsent(extension, mediaType);
    if (previous == null) {
      addFileExtension(mediaType, extension);
      this.allFileExtensions.add(extension);
    }
  }

  private void addFileExtension(MediaType mediaType, String extension) {
    this.fileExtensions.computeIfAbsent(mediaType, key -> new CopyOnWriteArrayList<>())
            .add(extension);
  }

  @Override
  public List<String> resolveFileExtensions(MediaType mediaType) {
    List<String> fileExtensions = this.fileExtensions.get(mediaType);
    return (fileExtensions != null ? fileExtensions : Collections.emptyList());
  }

  @Override
  public List<String> getAllFileExtensions() {
    return Collections.unmodifiableList(this.allFileExtensions);
  }

  /**
   * Use this method for a reverse lookup from extension to MediaType.
   *
   * @return a MediaType for the extension, or {@code null} if none found
   */
  @Nullable
  protected MediaType lookupMediaType(String extension) {
    return this.mediaTypes.get(extension.toLowerCase(Locale.ENGLISH));
  }

}
