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

package cn.taketoday.web.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.io.AbstractResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * Resolver that delegates to the chain, and if a resource is found, it then
 * attempts to find an encoded (e.g. gzip, brotli) variant that is acceptable
 * based on the "Accept-Encoding" request header.
 *
 * <p>The list of supported {@link #setContentCodings(List) contentCodings} can
 * be configured, in order of preference, and each coding must be associated
 * with {@link #setExtensions(Map) extensions}.
 *
 * <p>Note that this resolver must be ordered ahead of a
 * {@link VersionResourceResolver} with a content-based, version strategy to
 * ensure the version calculation is not impacted by the encoding.
 *
 * @author Rossen Stoyanchev
 */
public class EncodedResourceResolver extends AbstractResourceResolver {

  /**
   * The default content codings.
   */
  public static final List<String> DEFAULT_CODINGS = Arrays.asList("br", "gzip");

  private final ArrayList<String> contentCodings = new ArrayList<>(DEFAULT_CODINGS);

  private final LinkedHashMap<String, String> extensions = new LinkedHashMap<>();

  public EncodedResourceResolver() {
    this.extensions.put("gzip", ".gz");
    this.extensions.put("br", ".br");
  }

  /**
   * Configure the supported content codings in order of preference. The first
   * coding that is present in the {@literal "Accept-Encoding"} header for a
   * given request, and that has a file present with the associated extension,
   * is used.
   * <p><strong>Note:</strong> Each coding must be associated with a file
   * extension via {@link #registerExtension} or {@link #setExtensions}. Also
   * customizations to the list of codings here should be matched by
   * customizations to the same list in {@link CachingResourceResolver} to
   * ensure encoded variants of a resource are cached under separate keys.
   * <p>By default this property is set to {@literal ["br", "gzip"]}.
   *
   * @param codings one or more supported content codings
   */
  public void setContentCodings(List<String> codings) {
    Assert.notEmpty(codings, "At least one content coding expected");
    this.contentCodings.clear();
    this.contentCodings.addAll(codings);
  }

  /**
   * Return a read-only list with the supported content codings.
   */
  public List<String> getContentCodings() {
    return contentCodings;
  }

  /**
   * Configure mappings from content codings to file extensions. A dot "."
   * will be prepended in front of the extension value if not present.
   * <p>By default this is configured with {@literal ["br" -> ".br"]} and
   * {@literal ["gzip" -> ".gz"]}.
   *
   * @param extensions the extensions to use.
   * @see #registerExtension(String, String)
   */
  public void setExtensions(Map<String, String> extensions) {
    for (Map.Entry<String, String> entry : extensions.entrySet()) {
      registerExtension(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Return a read-only map with coding-to-extension mappings.
   */
  public Map<String, String> getExtensions() {
    return extensions;
  }

  /**
   * Java config friendly alternative to {@link #setExtensions(Map)}.
   *
   * @param coding the content coding
   * @param extension the associated file extension
   */
  public void registerExtension(String coding, String extension) {
    this.extensions.put(coding, extension.startsWith(".") ? extension : "." + extension);
  }

  @Override
  protected Resource resolveResourceInternal(
          @Nullable RequestContext request, String requestPath,
          List<? extends Resource> locations, ResourceResolvingChain chain) {

    Resource resource = chain.resolveResource(request, requestPath, locations);
    if (resource == null || request == null) {
      return resource;
    }

    String acceptEncoding = getAcceptEncoding(request);
    if (acceptEncoding == null) {
      return resource;
    }

    for (String coding : contentCodings) {
      if (acceptEncoding.contains(coding)) {
        try {
          String extension = getExtension(coding);
          Resource encoded = new EncodedResource(resource, coding, extension);
          if (encoded.exists()) {
            return encoded;
          }
        }
        catch (IOException ex) {
          if (logger.isTraceEnabled()) {
            logger.trace("No {} resource for [{}]", coding, resource.getName(), ex);
          }
        }
      }
    }

    return resource;
  }

  @Nullable
  private String getAcceptEncoding(RequestContext request) {
    String header = request.getHeaders().getFirst(HttpHeaders.ACCEPT_ENCODING);
    return header != null ? header.toLowerCase() : null;
  }

  private String getExtension(String coding) {
    String extension = this.extensions.get(coding);
    if (extension == null) {
      throw new IllegalStateException("No file extension associated with content coding " + coding);
    }
    return extension;
  }

  @Override
  protected String resolveUrlPathInternal(
          String resourceUrlPath, List<? extends Resource> locations, ResourceResolvingChain chain) {
    return chain.resolveUrlPath(resourceUrlPath, locations);
  }

  /**
   * An encoded {@link HttpResource}.
   */
  static final class EncodedResource extends AbstractResource implements HttpResource {

    private final String coding;
    private final Resource encoded;
    private final Resource original;

    EncodedResource(Resource original, String coding, String extension) throws IOException {
      this.original = original;
      this.coding = coding;
      this.encoded = original.createRelative(original.getName() + extension);
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return this.encoded.getInputStream();
    }

    @Override
    public boolean exists() {
      return this.encoded.exists();
    }

    @Override
    public boolean isReadable() {
      return this.encoded.isReadable();
    }

    @Override
    public boolean isOpen() {
      return this.encoded.isOpen();
    }

    @Override
    public URL getURL() throws IOException {
      return encoded.getURL();
    }

    @Override
    public URI getURI() throws IOException {
      return this.encoded.getURI();
    }

    @Override
    public File getFile() throws IOException {
      return this.encoded.getFile();
    }

    @Override
    public long contentLength() throws IOException {
      return this.encoded.contentLength();
    }

    @Override
    public long lastModified() throws IOException {
      return this.encoded.lastModified();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
      return this.encoded.createRelative(relativePath);
    }

    @Override
    @Nullable
    public String getName() {
      return this.original.getName();
    }

    @Override
    public HttpHeaders getResponseHeaders() {
      HttpHeaders headers;
      if (this.original instanceof HttpResource original) {
        headers = original.getResponseHeaders();
      }
      else {
        headers = HttpHeaders.create();
      }
      headers.add(HttpHeaders.CONTENT_ENCODING, this.coding);
      headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
      return headers;
    }
  }

}
