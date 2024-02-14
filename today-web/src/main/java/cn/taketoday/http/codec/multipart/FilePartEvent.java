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

package cn.taketoday.http.codec.multipart;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.ContentDisposition;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.MediaTypeFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Represents an event triggered for a file upload. Contains the
 * {@linkplain #filename() filename}, besides the {@linkplain #headers() headers}
 * and {@linkplain #content() content} exposed through {@link PartEvent}.
 *
 * <p>On the client side, instances of this interface can be created via one
 * of the overloaded {@linkplain #create(String, Path) create} methods.
 *
 * <p>On the server side, multipart file uploads trigger one or more
 * {@code FilePartEvent}, as {@linkplain PartEvent described here}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PartEvent
 * @since 4.0 2022/4/22 9:13
 */
public interface FilePartEvent extends PartEvent {

  /**
   * Return the original filename in the client's filesystem.
   * <p><strong>Note:</strong> Please keep in mind this filename is supplied
   * by the client and should not be used blindly. In addition to not using
   * the directory portion, the file name could also contain characters such
   * as ".." and others that can be used maliciously. It is recommended to not
   * use this filename directly. Preferably generate a unique one and save
   * this one somewhere for reference, if necessary.
   *
   * @return the original filename, or the empty String if no file has been chosen
   * in the multipart form, or {@code null} if not defined or not available
   * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>
   * @see <a href="https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload">Unrestricted File Upload</a>
   */
  default String filename() {
    String filename = this.headers().getContentDisposition().getFilename();
    Assert.state(filename != null, "No filename found");
    return filename;
  }

  /**
   * Creates a stream of {@code FilePartEvent} objects based on the given
   * {@linkplain PartEvent#name() name} and resource.
   *
   * @param name the name of the part
   * @param resource the resource
   * @return a stream of events
   */
  static Flux<FilePartEvent> create(String name, Resource resource) {
    return create(name, resource, null);
  }

  /**
   * Creates a stream of {@code FilePartEvent} objects based on the given
   * {@linkplain PartEvent#name() name} and resource.
   *
   * @param name the name of the part
   * @param resource the resource
   * @param headersConsumer used to change default headers. Can be {@code null}.
   * @return a stream of events
   */
  static Flux<FilePartEvent> create(String name, Resource resource, @Nullable Consumer<HttpHeaders> headersConsumer) {
    try {
      return create(name, resource.getFile().toPath(), headersConsumer);
    }
    catch (IOException ex) {
      return Flux.error(ex);
    }
  }

  /**
   * Creates a stream of {@code FilePartEvent} objects based on the given
   * {@linkplain PartEvent#name() name} and file path.
   *
   * @param name the name of the part
   * @param path the file path
   * @return a stream of events
   */
  static Flux<FilePartEvent> create(String name, Path path) {
    return create(name, path, null);
  }

  /**
   * Creates a stream of {@code FilePartEvent} objects based on the given
   * {@linkplain PartEvent#name() name} and file path.
   *
   * @param name the name of the part
   * @param path the file path
   * @param headersConsumer used to change default headers. Can be {@code null}.
   * @return a stream of events
   */
  static Flux<FilePartEvent> create(String name, Path path, @Nullable Consumer<HttpHeaders> headersConsumer) {
    Assert.hasLength(name, "Name must not be empty");
    Assert.notNull(path, "Path is required");

    return Flux.defer(() -> {
      String pathName = path.toString();
      MediaType contentType = MediaTypeFactory.getMediaType(pathName)
              .orElse(MediaType.APPLICATION_OCTET_STREAM);
      String filename = StringUtils.getFilename(pathName);
      if (filename == null) {
        return Flux.error(new IllegalArgumentException("Invalid file: " + pathName));
      }
      Flux<DataBuffer> contents = DataBufferUtils.read(path, DefaultDataBufferFactory.sharedInstance, 8192);

      return create(name, filename, contentType, contents, headersConsumer);
    });
  }

  /**
   * Creates a stream of {@code FilePartEvent} objects based on the given
   * {@linkplain PartEvent#name() name}, {@linkplain #filename()},
   * content-type, and contents.
   *
   * @param partName the name of the part
   * @param filename the filename
   * @param contentType the content-type for the contents
   * @param contents the contents
   * @return a stream of events
   */
  static Flux<FilePartEvent> create(String partName, String filename, MediaType contentType,
          Flux<DataBuffer> contents) {

    return create(partName, filename, contentType, contents, null);
  }

  /**
   * Creates a stream of {@code FilePartEvent} objects based on the given
   * {@linkplain PartEvent#name() name}, {@linkplain #filename()},
   * content-type, and contents.
   *
   * @param partName the name of the part
   * @param filename the filename
   * @param contentType the content-type for the contents
   * @param contents the contents
   * @param headersConsumer used to change default headers. Can be {@code null}.
   * @return a stream of events
   */
  static Flux<FilePartEvent> create(String partName, String filename, MediaType contentType,
          Flux<DataBuffer> contents, @Nullable Consumer<HttpHeaders> headersConsumer) {

    Assert.hasLength(partName, "PartName must not be empty");
    Assert.hasLength(filename, "Filename must not be empty");
    Assert.notNull(contentType, "ContentType is required");
    Assert.notNull(contents, "Contents is required");

    return Flux.defer(() -> {
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setContentType(contentType);

      headers.setContentDisposition(ContentDisposition.formData()
              .name(partName)
              .filename(filename, StandardCharsets.UTF_8)
              .build());

      if (headersConsumer != null) {
        headersConsumer.accept(headers);
      }

      return contents.map(content -> DefaultPartEvents.file(headers, content, false))
              .concatWith(Mono.just(DefaultPartEvents.file(headers)));
    });
  }

}
