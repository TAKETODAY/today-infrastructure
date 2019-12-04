/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
import java.net.URL;

import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.io.ResourceFilter;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.mapping.ResourceMapping;
import cn.taketoday.web.resource.WebResource;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-05-17 11:26
 */
public class DefaultResourceResolver implements ResourceResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultResourceResolver.class);

    private final PathMatcher pathMatcher;

    @Autowired
    public DefaultResourceResolver(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }

    @Override
    public WebResource resolveResource(String requestPath, ResourceMapping resourceMapping) throws Throwable {

        if (StringUtils.isEmpty(requestPath) || isInvalidPath(requestPath)) {
            return null;
        }

        String matchedPattern = null;
        final PathMatcher pathMatcher = this.pathMatcher;
        for (final String requestPathPattern : resourceMapping.getPathPatterns()) {
            if (pathMatcher.match(requestPathPattern, requestPath)) {
                matchedPattern = requestPathPattern;
                break;
            }
        }

        final String extractPathWithinPattern;
        if (pathMatcher.isPattern(matchedPattern)) {
            extractPathWithinPattern = pathMatcher.extractPathWithinPattern(matchedPattern, requestPath);
        }
        else {
            extractPathWithinPattern = requestPath;
        }

        if (StringUtils.isEmpty(extractPathWithinPattern)) {
            return null;
        }

        // log.debug("resource: [{}]", extractPathWithinPattern);
        for (String location : resourceMapping.getLocations()) {
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
        return false;
    }

    public static class DefaultDelegateWebResource implements WebResource {

        private final String etag;
        private final String name;
        private final long contentLength;
        private final String contentType;

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
            this.contentType = WebUtils.resolveFileContentType(resource.getLocation().getPath());
            this.etag = WebUtils.getEtag(getName(), contentLength(), lastModified());
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
            return contentType;
        }

        @Override
        public String getETag() {
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
    }
}
