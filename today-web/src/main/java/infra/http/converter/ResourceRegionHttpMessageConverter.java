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

package infra.http.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import infra.core.io.InputStreamResource;
import infra.core.io.Resource;
import infra.core.io.ResourceRegion;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.MediaTypeFactory;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.MimeTypeUtils;
import infra.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageConverter} that can write a single
 * {@link ResourceRegion} or Collections of {@link ResourceRegion ResourceRegions}.
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ResourceRegionHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

  public ResourceRegionHttpMessageConverter() {
    super(MediaType.ALL);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected MediaType getDefaultContentType(Object object) {
    Resource resource = null;
    if (object instanceof ResourceRegion resourceRegion) {
      resource = resourceRegion.getResource();
    }
    else {
      Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
      if (!regions.isEmpty()) {
        resource = regions.iterator().next().getResource();
      }
    }
    return MediaTypeFactory.getMediaType(resource)
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
  }

  @Override
  public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
    return false;
  }

  @Override
  public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
    return false;
  }

  @Override
  public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected ResourceRegion readInternal(Class<?> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
    return canWrite(clazz, null, mediaType);
  }

  @Override
  public boolean canWrite(@Nullable Type type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
    if (!(type instanceof ParameterizedType parameterizedType)) {
      return type instanceof Class<?> c && ResourceRegion.class.isAssignableFrom(c);
    }
    if (!(parameterizedType.getRawType() instanceof Class<?> rawType)) {
      return false;
    }
    if (!(Collection.class.isAssignableFrom(rawType))) {
      return false;
    }
    if (parameterizedType.getActualTypeArguments().length != 1) {
      return false;
    }
    Type typeArgument = parameterizedType.getActualTypeArguments()[0];
    if (!(typeArgument instanceof Class<?> typeArgumentClass)) {
      return false;
    }
    return ResourceRegion.class.isAssignableFrom(typeArgumentClass);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {
    if (object instanceof ResourceRegion resourceRegion) {
      writeResourceRegion(resourceRegion, outputMessage);
    }
    else {
      Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
      if (regions.size() == 1) {
        writeResourceRegion(regions.iterator().next(), outputMessage);
      }
      else {
        writeResourceRegionCollection(regions, outputMessage);
      }
    }
  }

  protected void writeResourceRegion(ResourceRegion region, HttpOutputMessage outputMessage) throws IOException {
    HttpHeaders headers = outputMessage.getHeaders();

    long start = region.getPosition();
    long end = start + region.getCount() - 1;
    Resource resource = region.getResource();
    long resourceLength = resource.contentLength();
    end = Math.min(end, resourceLength - 1);
    long rangeLength = end - start + 1;
    headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + '-' + end + '/' + resourceLength);
    headers.setContentLength(rangeLength);

    if (outputMessage.supportsZeroCopy() && resource.isFile()) {
      outputMessage.sendFile(resource.getFile(), region.getPosition(), region.getCount());
    }
    else {
      try (InputStream in = resource.getInputStream()) {
        StreamUtils.copyRange(in, outputMessage.getBody(), start, end);
      }
    }
  }

  private void writeResourceRegionCollection(Collection<ResourceRegion> resourceRegions, HttpOutputMessage outputMessage) throws IOException {
    Assert.notNull(resourceRegions, "Collection of ResourceRegion should not be null");
    HttpHeaders responseHeaders = outputMessage.getHeaders();

    MediaType contentType = responseHeaders.getContentType();
    String boundaryString = MimeTypeUtils.generateMultipartBoundaryString();
    responseHeaders.setOrRemove(HttpHeaders.CONTENT_TYPE, "multipart/byteranges; boundary=" + boundaryString);
    OutputStream out = outputMessage.getBody();

    Resource resource = null;
    InputStream in = null;
    long inputStreamPosition = 0;

    try {
      for (ResourceRegion region : resourceRegions) {
        long start = region.getPosition() - inputStreamPosition;
        if (start < 0 || resource != region.getResource()) {
          if (in != null) {
            in.close();
          }
          resource = region.getResource();
          in = resource.getInputStream();
          inputStreamPosition = 0;
          start = region.getPosition();
        }
        long end = start + region.getCount() - 1;
        // Writing MIME header.
        println(out);
        print(out, "--" + boundaryString);
        println(out);
        if (contentType != null) {
          print(out, "Content-Type: " + contentType);
          println(out);
        }
        long resourceLength = region.getResource().contentLength();
        end = Math.min(end, resourceLength - inputStreamPosition - 1);
        print(out, "Content-Range: bytes " + region.getPosition() + '-'
                + (region.getPosition() + region.getCount() - 1) + '/' + resourceLength);
        println(out);
        println(out);
        // Printing content
        StreamUtils.copyRange(in, out, start, end);
        inputStreamPosition += (end + 1);
      }
    }
    finally {
      try {
        if (in != null) {
          in.close();
        }
      }
      catch (IOException ex) {
        // ignore
      }
    }

    println(out);
    print(out, "--" + boundaryString + "--");
  }

  private static void println(OutputStream os) throws IOException {
    os.write('\r');
    os.write('\n');
  }

  private static void print(OutputStream os, String buf) throws IOException {
    os.write(buf.getBytes(StandardCharsets.US_ASCII));
  }

  @Override
  @SuppressWarnings("unchecked")
  protected boolean supportsRepeatableWrites(Object object) {
    if (object instanceof ResourceRegion resourceRegion) {
      return supportsRepeatableWrites(resourceRegion);
    }
    else {
      Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
      for (ResourceRegion region : regions) {
        if (!supportsRepeatableWrites(region)) {
          return false;
        }
      }
      return true;
    }
  }

  private boolean supportsRepeatableWrites(ResourceRegion region) {
    return !(region.getResource() instanceof InputStreamResource);
  }
}
