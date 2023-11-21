/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.docker.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.message.StatusLine;

import cn.taketoday.buildpack.platform.io.Content;
import cn.taketoday.buildpack.platform.io.IOConsumer;
import cn.taketoday.buildpack.platform.json.SharedObjectMapper;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Abstract base class for {@link HttpTransport} implementations backed by a
 * {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Mike Smithson
 * @author Scott Frederick
 */
abstract class HttpClientTransport implements HttpTransport {

  static final String REGISTRY_AUTH_HEADER = "X-Registry-Auth";

  private final HttpClient client;

  private final HttpHost host;

  protected HttpClientTransport(HttpClient client, HttpHost host) {
    Assert.notNull(client, "Client is required");
    Assert.notNull(host, "Host is required");
    this.client = client;
    this.host = host;
  }

  /**
   * Perform an HTTP GET operation.
   *
   * @param uri the destination URI
   * @return the operation response
   */
  @Override
  public Response get(URI uri) {
    return execute(new HttpGet(uri));
  }

  /**
   * Perform an HTTP POST operation.
   *
   * @param uri the destination URI
   * @return the operation response
   */
  @Override
  public Response post(URI uri) {
    return execute(new HttpPost(uri));
  }

  /**
   * Perform an HTTP POST operation.
   *
   * @param uri the destination URI
   * @param registryAuth registry authentication credentials
   * @return the operation response
   */
  @Override
  public Response post(URI uri, String registryAuth) {
    return execute(new HttpPost(uri), registryAuth);
  }

  /**
   * Perform an HTTP POST operation.
   *
   * @param uri the destination URI
   * @param contentType the content type to write
   * @param writer a content writer
   * @return the operation response
   */
  @Override
  public Response post(URI uri, String contentType, IOConsumer<OutputStream> writer) {
    return execute(new HttpPost(uri), contentType, writer);
  }

  /**
   * Perform an HTTP PUT operation.
   *
   * @param uri the destination URI
   * @param contentType the content type to write
   * @param writer a content writer
   * @return the operation response
   */
  @Override
  public Response put(URI uri, String contentType, IOConsumer<OutputStream> writer) {
    return execute(new HttpPut(uri), contentType, writer);
  }

  /**
   * Perform an HTTP DELETE operation.
   *
   * @param uri the destination URI
   * @return the operation response
   */
  @Override
  public Response delete(URI uri) {
    return execute(new HttpDelete(uri));
  }

  private Response execute(HttpUriRequestBase request, String contentType, IOConsumer<OutputStream> writer) {
    request.setEntity(new WritableHttpEntity(contentType, writer));
    return execute(request);
  }

  private Response execute(HttpUriRequestBase request, String registryAuth) {
    if (StringUtils.hasText(registryAuth)) {
      request.setHeader(REGISTRY_AUTH_HEADER, registryAuth);
    }
    return execute(request);
  }

  private Response execute(HttpUriRequest request) {
    try {
      ClassicHttpResponse response = this.client.executeOpen(this.host, request, null);
      int statusCode = response.getCode();
      if (statusCode >= 400 && statusCode <= 500) {
        HttpEntity entity = response.getEntity();
        Errors errors = (statusCode != 500) ? getErrorsFromResponse(entity) : null;
        Message message = getMessageFromResponse(entity);
        StatusLine statusLine = new StatusLine(response);
        throw new DockerEngineException(this.host.toHostString(), request.getUri(), statusCode,
                statusLine.getReasonPhrase(), errors, message);
      }
      return new HttpClientResponse(response);
    }
    catch (IOException | URISyntaxException ex) {
      throw new DockerConnectionException(this.host.toHostString(), ex);
    }
  }

  private Errors getErrorsFromResponse(HttpEntity entity) {
    try {
      return SharedObjectMapper.get().readValue(entity.getContent(), Errors.class);
    }
    catch (IOException ex) {
      return null;
    }
  }

  private Message getMessageFromResponse(HttpEntity entity) {
    try {
      return (entity.getContent() != null)
             ? SharedObjectMapper.get().readValue(entity.getContent(), Message.class) : null;
    }
    catch (IOException ex) {
      return null;
    }
  }

  HttpHost getHost() {
    return this.host;
  }

  /**
   * {@link HttpEntity} to send {@link Content} content.
   */
  private static class WritableHttpEntity extends AbstractHttpEntity {

    private final IOConsumer<OutputStream> writer;

    WritableHttpEntity(String contentType, IOConsumer<OutputStream> writer) {
      super(contentType, "UTF-8");
      this.writer = writer;
    }

    @Override
    public boolean isRepeatable() {
      return false;
    }

    @Override
    public long getContentLength() {
      if (this.getContentType() != null && this.getContentType().equals("application/json")) {
        return calculateStringContentLength();
      }
      return -1;
    }

    @Override
    public InputStream getContent() throws UnsupportedOperationException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
      this.writer.accept(outputStream);
    }

    @Override
    public boolean isStreaming() {
      return true;
    }

    private int calculateStringContentLength() {
      try {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        this.writer.accept(bytes);
        return bytes.toByteArray().length;
      }
      catch (IOException ex) {
        return -1;
      }
    }

    @Override
    public void close() throws IOException {
    }

  }

  /**
   * An HTTP operation response.
   */
  private static class HttpClientResponse implements Response {

    private final ClassicHttpResponse response;

    HttpClientResponse(ClassicHttpResponse response) {
      this.response = response;
    }

    @Override
    public InputStream getContent() throws IOException {
      return this.response.getEntity().getContent();
    }

    @Override
    public void close() throws IOException {
      this.response.close();
    }

  }

}
