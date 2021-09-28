/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import cn.taketoday.core.PathMatcher;
import cn.taketoday.core.io.PathMatchingResourcePatternResolver;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceFilter;
import cn.taketoday.core.io.ResourceResolver;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.WebUtils;
import cn.taketoday.web.handler.ResourceMatchResult;

/**
 * @author TODAY <br>
 * 2019-05-17 11:26
 */
public class DefaultResourceResolver implements WebResourceResolver {

  private static final Logger log = LoggerFactory.getLogger(DefaultResourceResolver.class);

  private ResourceResolver resourceResolver = new PathMatchingResourcePatternResolver();

  @Override
  public WebResource resolveResource(final ResourceMatchResult matchResult) {
    if (matchResult == null) {
      return null;
    }
    final String requestPath = matchResult.getRequestPath();

    if (StringUtils.isEmpty(requestPath) || isInvalidPath(requestPath)) {
      return null;
    }

    final String matchedPattern = matchResult.getMatchedPattern();
    final PathMatcher pathMatcher = matchResult.getPathMatcher();

    final String extractPathWithinPattern;
    if (pathMatcher.isPattern(matchedPattern)) {
      extractPathWithinPattern = pathMatcher.extractPathWithinPattern(matchedPattern, requestPath);
      if (StringUtils.isEmpty(extractPathWithinPattern)) {
        return null;
      }
    }
    else {
      extractPathWithinPattern = requestPath;
    }
    final ResourceResolver resourceResolver = getResourceResolver();

    // log.debug("resource: [{}]", extractPathWithinPattern);
    for (final String location : matchResult.getMapping().getLocations()) {
      try {
        // log.debug("look in: [{}]", location);
        final Resource[] resources = resourceResolver.getResources(location);
        if (ObjectUtils.isNotEmpty(resources)) {
          for (final Resource resource : resources) {
            final Resource createRelative = resource.createRelative(extractPathWithinPattern);
            if (createRelative.exists()) {
              // log.debug("Relative Resource: [{}]", createRelative);
              return DefaultDelegateWebResource.create(createRelative);
            }
          }
        }
      }
      catch (IOException ignored) { }
    }
    return null;
  }

  protected boolean isInvalidPath(final String path) {
    if (path.contains("WEB-INF") || path.contains("META-INF")) {
      log.warn("Path with \"WEB-INF\" or \"META-INF\": [{}]", path);
      return true;
    }
    if (path.contains(":/")) {
      String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
      if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
        if (log.isWarnEnabled()) {
          log.warn("Path represents URL or has \"url:\" prefix: [{}]", path);
        }
        return true;
      }
    }
    if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
      if (log.isWarnEnabled()) {
        log.warn("Path contains \"../\" after call to StringUtils#cleanPath: [{}]", path);
      }
      return true;
    }
    return false;
  }

  public ResourceResolver getResourceResolver() {
    return resourceResolver;
  }

  public void setResourceResolver(ResourceResolver resourceResolver) {
    this.resourceResolver = resourceResolver;
  }

  public static class DefaultDelegateWebResource implements WebResource {

    private String etag;
    private final String name;
    private final long contentLength;
    private String contentType;

    private final long lastModified;

    private final Resource resource;

    public static DefaultDelegateWebResource create(Resource resource) throws IOException {
      return new DefaultDelegateWebResource(resource);
    }

    public DefaultDelegateWebResource(Resource resource) throws IOException {
      this.name = resource.getName();
      this.resource = resource;
      this.lastModified = resource.lastModified();
      this.contentLength = resource.contentLength();
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return resource.getInputStream();
    }

    @Override
    public long contentLength() {
      return contentLength;
    }

    @Override
    public String getContentType() {
      if (contentType == null) {
        final MediaType mediaType = MediaType.fromFileName(name);
        if (mediaType != null) {
          return this.contentType = mediaType.toString();
        }
      }
      return contentType;
    }

    @Override
    public String getETag() {
      if (etag == null) {
        etag = WebUtils.getEtag(getName(), contentLength(), lastModified());
      }
      return etag;
    }

    @Override
    public long lastModified() {
      return lastModified;
    }

    @Override
    public URL getLocation() throws IOException {
      return resource.getLocation();
    }

    @Override
    public boolean exists() {
      return resource.exists();
    }

    @Override
    public File getFile() throws IOException {
      return resource.getFile();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
      return resource.createRelative(relativePath);
    }

    @Override
    public boolean isDirectory() throws IOException {
      return resource.isDirectory();
    }

    @Override
    public String[] list() throws IOException {
      return resource.list();
    }

    @Override
    public Resource[] list(ResourceFilter filter) throws IOException {
      return resource.list(filter);
    }

    @Override
    public URI getURI() throws IOException {
      return resource.getURI();
    }
  }
}
