/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.converter;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import infra.core.io.ByteArrayResource;
import infra.core.io.InputStreamResource;
import infra.core.io.Resource;
import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.MediaTypeFactory;
import infra.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can read/write {@link Resource Resources}
 * and supports byte range requests.
 *
 * <p>By default, this converter can read all media types. The {@link MediaType} is used
 * to determine the {@code Content-Type} of written resources.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ResourceHttpMessageConverter extends AbstractHttpMessageConverter<Resource> {

  private final boolean supportsReadStreaming;

  /**
   * Create a new instance of the {@code ResourceHttpMessageConverter}
   * that supports read streaming, i.e. can convert an
   * {@code HttpInputMessage} to {@code InputStreamResource}.
   */
  public ResourceHttpMessageConverter() {
    super(MediaType.ALL);
    this.supportsReadStreaming = true;
  }

  /**
   * Create a new instance of the {@code ResourceHttpMessageConverter}.
   *
   * @param supportsReadStreaming whether the converter should support
   * read streaming, i.e. convert to {@code InputStreamResource}
   */
  public ResourceHttpMessageConverter(boolean supportsReadStreaming) {
    super(MediaType.ALL);
    this.supportsReadStreaming = supportsReadStreaming;
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return Resource.class.isAssignableFrom(clazz);
  }

  @Override
  protected Resource readInternal(Class<? extends Resource> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException //
  {

    if (this.supportsReadStreaming && InputStreamResource.class == clazz) {
      return new InputStreamResource(inputMessage.getBody()) {

        @Nullable
        @Override
        public String getName() {
          return inputMessage.getHeaders().getContentDisposition().getFilename();
        }

        @Override
        public long contentLength() throws IOException {
          long length = inputMessage.getContentLength();
          return (length != -1 ? length : super.contentLength());
        }
      };
    }
    else if (Resource.class == clazz || ByteArrayResource.class.isAssignableFrom(clazz)) {
      byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
      return new ByteArrayResource(body) {
        @Override
        @Nullable
        public String getName() {
          return inputMessage.getHeaders().getContentDisposition().getFilename();
        }
      };
    }
    else {
      throw new HttpMessageNotReadableException("Unsupported resource class: " + clazz, inputMessage);
    }
  }

  @Override
  protected MediaType getDefaultContentType(Resource resource) {
    return MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
  }

  @Nullable
  @Override
  protected Long getContentLength(Resource resource, @Nullable MediaType contentType) throws IOException {
    // Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
    // Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
    if (InputStreamResource.class == resource.getClass()) {
      return null;
    }
    long contentLength = resource.contentLength();
    return (contentLength < 0 ? null : contentLength);
  }

  @Override
  protected void writeInternal(Resource resource, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException //
  {
    if (outputMessage.supportsZeroCopy() && resource.isFile()) {
      File file = resource.getFile();
      outputMessage.sendFile(file);
    }
    else {
      try (InputStream in = resource.getInputStream()) {
        OutputStream out = outputMessage.getBody();
        in.transferTo(out);
        out.flush();
      }
    }
  }

  @Override
  protected boolean supportsRepeatableWrites(Resource resource) {
    return !(resource instanceof InputStreamResource);
  }

}
