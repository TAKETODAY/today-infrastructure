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

package infra.core.test.tools;

import org.assertj.core.api.AssertProvider;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import infra.core.io.InputStreamSource;
import infra.util.FileCopyUtils;

/**
 * {@link DynamicFile} that holds resource file content and provides
 * {@link ResourceFileAssert} support.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public final class ResourceFile extends DynamicFile implements AssertProvider<ResourceFileAssert> {

  private ResourceFile(String path, String content) {
    super(path, content);
  }

  /**
   * Factory method to create a new {@link ResourceFile} from the given
   * {@link CharSequence}.
   *
   * @param path the relative path of the file or {@code null} to have the
   * path deduced
   * @param charSequence a char sequence containing the file contents
   * @return a {@link ResourceFile} instance
   */
  public static ResourceFile of(String path, CharSequence charSequence) {
    return new ResourceFile(path, charSequence.toString());
  }

  /**
   * Factory method to create a new {@link ResourceFile} from the given
   * {@code byte[]}.
   *
   * @param path the relative path of the file or {@code null} to have the
   * path deduced
   * @param bytes a byte array containing the file contents
   * @return a {@link ResourceFile} instance
   */
  public static ResourceFile of(String path, byte[] bytes) {
    return new ResourceFile(path, new String(bytes, StandardCharsets.UTF_8));
  }

  /**
   * Factory method to create a new {@link ResourceFile} from the given
   * {@link InputStreamSource}.
   *
   * @param path the relative path of the file
   * @param inputStreamSource the source for the file
   * @return a {@link ResourceFile} instance
   */
  public static ResourceFile of(String path, InputStreamSource inputStreamSource) {
    return of(path, appendable -> appendable.append(FileCopyUtils.copyToString(
            new InputStreamReader(inputStreamSource.getInputStream(), StandardCharsets.UTF_8))));
  }

  /**
   * Factory method to create a new {@link SourceFile} from the given
   * {@link WritableContent}.
   *
   * @param path the relative path of the file
   * @param writableContent the content to write to the file
   * @return a {@link ResourceFile} instance
   */
  public static ResourceFile of(String path, WritableContent writableContent) {
    return new ResourceFile(path, toString(writableContent));
  }

  /**
   * AssertJ {@code assertThat} support.
   *
   * @deprecated use {@code assertThat(sourceFile)} rather than calling this
   * method directly.
   */
  @Override
  @Deprecated
  public ResourceFileAssert assertThat() {
    return new ResourceFileAssert(this);
  }

}
