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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;
import cn.taketoday.buildpack.platform.docker.configuration.DockerHost;
import cn.taketoday.buildpack.platform.docker.configuration.ResolvedDockerHost;
import cn.taketoday.buildpack.platform.io.IOConsumer;

/**
 * HTTP transport used for docker access.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface HttpTransport {

  /**
   * Perform an HTTP GET operation.
   *
   * @param uri the destination URI (excluding any host/port)
   * @return the operation response
   * @throws IOException on IO error
   */
  Response get(URI uri) throws IOException;

  /**
   * Perform an HTTP POST operation.
   *
   * @param uri the destination URI (excluding any host/port)
   * @return the operation response
   * @throws IOException on IO error
   */
  Response post(URI uri) throws IOException;

  /**
   * Perform an HTTP POST operation.
   *
   * @param uri the destination URI (excluding any host/port)
   * @param registryAuth registry authentication credentials
   * @return the operation response
   * @throws IOException on IO error
   */
  Response post(URI uri, String registryAuth) throws IOException;

  /**
   * Perform an HTTP POST operation.
   *
   * @param uri the destination URI (excluding any host/port)
   * @param contentType the content type to write
   * @param writer a content writer
   * @return the operation response
   * @throws IOException on IO error
   */
  Response post(URI uri, String contentType, IOConsumer<OutputStream> writer) throws IOException;

  /**
   * Perform an HTTP PUT operation.
   *
   * @param uri the destination URI (excluding any host/port)
   * @param contentType the content type to write
   * @param writer a content writer
   * @return the operation response
   * @throws IOException on IO error
   */
  Response put(URI uri, String contentType, IOConsumer<OutputStream> writer) throws IOException;

  /**
   * Perform an HTTP DELETE operation.
   *
   * @param uri the destination URI (excluding any host/port)
   * @return the operation response
   * @throws IOException on IO error
   */
  Response delete(URI uri) throws IOException;

  /**
   * Create the most suitable {@link HttpTransport} based on the {@link DockerHost}.
   *
   * @param dockerHost the Docker host information
   * @return a {@link HttpTransport} instance
   */
  static HttpTransport create(DockerHostConfiguration dockerHost) {
    ResolvedDockerHost host = ResolvedDockerHost.from(dockerHost);
    HttpTransport remote = RemoteHttpClientTransport.createIfPossible(host);
    return (remote != null) ? remote : LocalHttpClientTransport.create(host);
  }

  /**
   * An HTTP operation response.
   */
  interface Response extends Closeable {

    /**
     * Return the content of the response.
     *
     * @return the response content
     * @throws IOException on IO error
     */
    InputStream getContent() throws IOException;

  }

}
