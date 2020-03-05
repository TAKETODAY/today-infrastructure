/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.web.resolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.io.ResourceFilter;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.MediaType;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.handler.ResourceMappingMatchResult;
import cn.taketoday.web.resource.WebResource;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-05-17 11:26
 */
public class DefaultResourceResolver implements WebResourceResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultResourceResolver.class);

    @Override
    public WebResource resolveResource(final ResourceMappingMatchResult matchResult) {
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

        // log.debug("resource: [{}]", extractPathWithinPattern);
        for (String location : matchResult.getMapping().getLocations()) {
            try {
                // log.debug("look in: [{}]", location);
                // TODO
                final Resource createRelative = //
                        ResourceUtils.getResource(location).createRelative(extractPathWithinPattern);

                if (createRelative.exists()) {
                    // log.debug("Relative Resource: [{}]", createRelative);
                    return DefaultDelegateWebResource.create(createRelative);
                }
            }
            catch (IOException e) {}
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
                final MediaType mediaType = MediaType.of(name);
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
