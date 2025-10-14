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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.function.BiConsumer;

import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;

/**
 * Resource-based implementation of {@link HandlerFunction}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ResourceHandlerFunction implements HandlerFunction<ServerResponse> {

  private static final Set<HttpMethod> SUPPORTED_METHODS =
          Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

  private final Resource resource;

  private final BiConsumer<Resource, HttpHeaders> headersConsumer;

  public ResourceHandlerFunction(Resource resource, BiConsumer<Resource, HttpHeaders> headersConsumer) {
    this.resource = resource;
    this.headersConsumer = headersConsumer;
  }

  @Override
  public ServerResponse handle(ServerRequest request) {
    HttpMethod method = request.method();
    if (HttpMethod.GET == method) {
      return EntityResponse.fromObject(this.resource)
              .headers(headers -> this.headersConsumer.accept(this.resource, headers))
              .build();
    }
    else if (HttpMethod.HEAD == method) {
      Resource headResource = new HeadMethodResource(this.resource);
      return EntityResponse.fromObject(headResource)
              .headers(headers -> this.headersConsumer.accept(this.resource, headers))
              .build();
    }
    else if (HttpMethod.OPTIONS == method) {
      return ServerResponse.ok()
              .allow(SUPPORTED_METHODS).build();
    }
    return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED)
            .allow(SUPPORTED_METHODS).build();
  }

  static class HeadMethodResource implements Resource {

    private static final byte[] EMPTY = new byte[0];

    private final Resource delegate;

    public HeadMethodResource(Resource delegate) {
      this.delegate = delegate;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(EMPTY);
    }

    // delegation

    @Override
    public boolean exists() {
      return this.delegate.exists();
    }

    @Override
    public URL getURL() throws IOException {
      return this.delegate.getURL();
    }

    @Override
    public URI getURI() throws IOException {
      return this.delegate.getURI();
    }

    @Override
    public File getFile() throws IOException {
      return this.delegate.getFile();
    }

    @Override
    public long contentLength() throws IOException {
      return this.delegate.contentLength();
    }

    @Override
    public long lastModified() throws IOException {
      return this.delegate.lastModified();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
      return this.delegate.createRelative(relativePath);
    }

    @Override
    @Nullable
    public String getName() {
      return this.delegate.getName();
    }

    @Override
    public String toString() {
      return this.delegate.toString();
    }
  }

}
