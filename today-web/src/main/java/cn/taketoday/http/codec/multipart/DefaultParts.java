/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.ContentDisposition;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Default implementations of {@link Part} and subtypes.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
abstract class DefaultParts {

  /**
   * Create a new {@link FormFieldPart} with the given parameters.
   *
   * @param headers the part headers
   * @param value the form field value
   * @return the created part
   */
  public static FormFieldPart formFieldPart(HttpHeaders headers, String value) {
    Assert.notNull(headers, "Headers must not be null");
    Assert.notNull(value, "Value must not be null");

    return new DefaultFormFieldPart(headers, value);
  }

  /**
   * Create a new {@link Part} or {@link FilePart} with the given parameters.
   * Returns {@link FilePart} if the {@code Content-Disposition} of the given
   * headers contains a filename, or a "normal" {@link Part} otherwise
   *
   * @param headers the part headers
   * @param dataBuffers the content of the part
   * @return {@link Part} or {@link FilePart}, depending on {@link HttpHeaders#getContentDisposition()}
   */
  public static Part part(HttpHeaders headers, Flux<DataBuffer> dataBuffers) {
    Assert.notNull(headers, "Headers must not be null");
    Assert.notNull(dataBuffers, "DataBuffers must not be null");

    return partInternal(headers, new FluxContent(dataBuffers));
  }

  /**
   * Create a new {@link Part} or {@link FilePart} based on the given file.
   * Returns {@link FilePart} if the {@code Content-Disposition} of the given
   * headers contains a filename, or a "normal" {@link Part} otherwise
   *
   * @param headers the part headers
   * @param file the file
   * @param scheduler the scheduler used for reading the file
   * @return {@link Part} or {@link FilePart}, depending on {@link HttpHeaders#getContentDisposition()}
   */
  public static Part part(HttpHeaders headers, Path file, Scheduler scheduler) {
    Assert.notNull(headers, "Headers must not be null");
    Assert.notNull(file, "File must not be null");
    Assert.notNull(scheduler, "Scheduler must not be null");

    return partInternal(headers, new FileContent(file, scheduler));
  }

  private static Part partInternal(HttpHeaders headers, Content content) {
    String filename = headers.getContentDisposition().getFilename();
    if (filename != null) {
      return new DefaultFilePart(headers, content);
    }
    else {
      return new DefaultPart(headers, content);
    }
  }

  /**
   * Abstract base class.
   */
  private static abstract class AbstractPart implements Part {

    private final HttpHeaders headers;

    protected AbstractPart(HttpHeaders headers) {
      Assert.notNull(headers, "HttpHeaders is required");
      this.headers = headers;
    }

    @Override
    public String name() {
      String name = headers().getContentDisposition().getName();
      Assert.state(name != null, "No name available");
      return name;
    }

    @Override
    public HttpHeaders headers() {
      return this.headers;
    }
  }

  /**
   * Default implementation of {@link FormFieldPart}.
   */
  private static class DefaultFormFieldPart extends AbstractPart implements FormFieldPart {

    private final String value;

    public DefaultFormFieldPart(HttpHeaders headers, String value) {
      super(headers);
      this.value = value;
    }

    @Override
    public Flux<DataBuffer> content() {
      return Flux.defer(() -> {
        byte[] bytes = this.value.getBytes(MultipartUtils.charset(headers()));
        return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
      });
    }

    @Override
    public String value() {
      return this.value;
    }

    @Override
    public String toString() {
      String name = headers().getContentDisposition().getName();
      if (name != null) {
        return "DefaultFormFieldPart{" + name() + "}";
      }
      else {
        return "DefaultFormFieldPart";
      }
    }
  }

  /**
   * Default implementation of {@link Part}.
   */
  private static class DefaultPart extends AbstractPart {

    protected final Content content;

    public DefaultPart(HttpHeaders headers, Content content) {
      super(headers);
      this.content = content;
    }

    @Override
    public Flux<DataBuffer> content() {
      return this.content.content();
    }

    @Override
    public Mono<Void> delete() {
      return this.content.delete();
    }

    @Override
    public String toString() {
      String name = headers().getContentDisposition().getName();
      if (name != null) {
        return "DefaultPart{" + name + "}";
      }
      else {
        return "DefaultPart";
      }
    }
  }

  /**
   * Default implementation of {@link FilePart}.
   */
  private static class DefaultFilePart extends DefaultPart implements FilePart {

    public DefaultFilePart(HttpHeaders headers, Content content) {
      super(headers, content);
    }

    @Override
    public String filename() {
      String filename = headers().getContentDisposition().getFilename();
      Assert.state(filename != null, "No filename found");
      return filename;
    }

    @Override
    public Mono<Void> transferTo(Path dest) {
      return this.content.transferTo(dest);
    }

    @Override
    public String toString() {
      ContentDisposition contentDisposition = headers().getContentDisposition();
      String name = contentDisposition.getName();
      String filename = contentDisposition.getFilename();
      if (name != null) {
        return "DefaultFilePart{" + name + " (" + filename + ")}";
      }
      else {
        return "DefaultFilePart{(" + filename + ")}";
      }
    }
  }

  /**
   * Part content abstraction.
   */
  private interface Content {

    Flux<DataBuffer> content();

    Mono<Void> transferTo(Path dest);

    Mono<Void> delete();
  }

  /**
   * {@code Content} implementation based on a flux of data buffers.
   */
  private record FluxContent(Flux<DataBuffer> content) implements Content {

    @Override
    public Mono<Void> transferTo(Path dest) {
      return DataBufferUtils.write(this.content, dest);
    }

    @Override
    public Mono<Void> delete() {
      return Mono.empty();
    }
  }

  /**
   * {@code Content} implementation based on a file.
   */
  private record FileContent(Path file, Scheduler scheduler) implements Content {

    @Override
    public Flux<DataBuffer> content() {
      return DataBufferUtils.readByteChannel(
                      () -> Files.newByteChannel(this.file, StandardOpenOption.READ),
                      DefaultDataBufferFactory.sharedInstance, 1024)
              .subscribeOn(this.scheduler);
    }

    @Override
    public Mono<Void> transferTo(Path dest) {
      return blockingOperation(() -> Files.copy(this.file, dest, StandardCopyOption.REPLACE_EXISTING));
    }

    @Override
    public Mono<Void> delete() {
      return blockingOperation(() -> {
        Files.delete(this.file);
        return null;
      });
    }

    private Mono<Void> blockingOperation(Callable<?> callable) {
      return Mono.<Void>create(sink -> {
                try {
                  callable.call();
                  sink.success();
                }
                catch (Exception ex) {
                  sink.error(ex);
                }
              })
              .subscribeOn(this.scheduler);
    }
  }

}
