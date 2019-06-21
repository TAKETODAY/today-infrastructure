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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.framework.server.undertow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.framework.config.CompressionConfiguration;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

/**
 * @author TODAY <br>
 *         2019-01-12 17:28
 */
public abstract class UndertowCompressionUtils {

    private static final String WILDCARD_TYPE = "*/*";

    protected static boolean isWildcardType(String mimeType) {
        return WILDCARD_TYPE.equals(mimeType);
    }

    /**
     * Optionally wrap the given {@link HttpHandler} for HTTP compression support.
     * 
     * @param compression
     *            the HTTP compression configuration
     * @param httpHandler
     *            the HTTP handler to wrap
     * @return the wrapped HTTP handler if compression is enabled, or the handler
     *         itself
     */
    public static HttpHandler configureCompression(CompressionConfiguration compression, //
            HttpHandler httpHandler) //
    {

        ContentEncodingRepository repository = new ContentEncodingRepository();

        repository.addEncodingHandler(Constant.GZIP, new GzipEncodingProvider(), 50, Predicates.and(getCompressionPredicates(compression)));

        return new EncodingHandler(repository).setNext(httpHandler);
    }

    private static Predicate[] getCompressionPredicates(CompressionConfiguration compression) {

        final List<Predicate> predicates = new ArrayList<>();

        predicates.add(Predicates.maxContentSize(compression.getMinResponseSize().toBytes()));

        if (compression.getIncludeMethods() != null //
                || compression.getExcludeMethods() != null//
                || compression.getIncludedPaths() != null//
                || compression.getExcludePaths() != null) //
        {
            predicates.add(new RequestPathAndMethodPredicate(compression));
        }

        if (compression.getIncludeAgentPatterns() != null //
                || compression.getExcludeUserAgents() != null//
                || compression.getExcludeAgentPatterns() != null) //
        {
            predicates.add(new UserAgentPredicate(compression));
        }

        predicates.add(new MimeTypesPredicate(compression.getMimeTypes()));
        return predicates.toArray(new Predicate[0]);
    }

    private static <T> boolean contains(T[] targets, Function<T, Boolean> function) {
        if (targets != null) {
            for (T target : targets) {
                if (function.apply(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class UserAgentPredicate implements Predicate {

        private final String[] excludeUserAgents;

        private final Pattern[] excludeAgentPatterns;

        private final Pattern[] includeAgentPatterns;

        private UserAgentPredicate(final CompressionConfiguration compression) {
            this.excludeUserAgents = compression.getExcludeUserAgents();
            String[] excludeAgentPatterns = compression.getExcludeAgentPatterns();

            final List<Pattern> patterns = new ArrayList<>();

            if (excludeAgentPatterns != null) {
                for (String excludeAgentPattern : excludeAgentPatterns) {
                    patterns.add(Pattern.compile(excludeAgentPattern));
                }
                this.excludeAgentPatterns = patterns.toArray(new Pattern[0]);
            }
            else {
                this.excludeAgentPatterns = null;
            }

            patterns.clear();

            final String[] includeAgentPatterns = compression.getIncludeAgentPatterns();
            if (includeAgentPatterns != null) {
                for (String includeAgentPattern : includeAgentPatterns) {
                    patterns.add(Pattern.compile(includeAgentPattern));
                }
                this.includeAgentPatterns = patterns.toArray(new Pattern[0]);
            }
            else {
                this.includeAgentPatterns = null;
            }
        }

        @Override
        public boolean resolve(HttpServerExchange serverExchange) {
            final String userAgent = serverExchange.getRequestHeaders().getFirst(Headers.USER_AGENT);
            return testExcludeUserAgents(userAgent) || testIncludeUserAgents(userAgent);
        }

        boolean testExcludeUserAgents(final String userAgent) {
            return !(contains(excludeUserAgents, excludedUserAgent -> excludedUserAgent.equals(userAgent)) && //
                    contains(excludeAgentPatterns, excludeAgentPattern -> excludeAgentPattern.matcher(userAgent).matches()));
        }

        boolean testIncludeUserAgents(final String userAgent) {

            if (includeAgentPatterns != null) {
                return contains(includeAgentPatterns, //
                        includeAgentPattern -> includeAgentPattern.matcher(userAgent).matches());
            }
            return true;
        }

    }

    private static class RequestPathAndMethodPredicate implements Predicate {

        private final String[] excludePaths;
        private final String[] includedPaths;

        private final String[] excludeMethods;
        private final String[] includeMethods;

        RequestPathAndMethodPredicate(final CompressionConfiguration compression) {
            this.excludePaths = compression.getExcludePaths();
            this.includedPaths = compression.getIncludedPaths();

            this.excludeMethods = compression.getExcludeMethods();
            this.includeMethods = compression.getIncludeMethods();
        }

        @Override
        public boolean resolve(final HttpServerExchange serverExchange) {

            final HttpString requestMethod = serverExchange.getRequestMethod();
            final String requestPath = serverExchange.getRequestPath();

            return testExclude(requestMethod, requestPath) || testInclude(requestMethod, requestPath);
        }

        boolean testExclude(final HttpString requestMethod, final String requestPath) {
            return !(contains(excludePaths, requestPath::equals) //
                    && contains(excludeMethods, requestMethod::equalToString));
        }

        boolean testInclude(final HttpString requestMethod, final String requestPath) {
            return contains(includedPaths, requestPath::equals) //
                    || contains(includeMethods, requestMethod::equalToString);
        }
    }

    private static class MimeTypesPredicate implements Predicate {

        private final String[] mimeTypes;

        MimeTypesPredicate(String... mimeTypes) {
            this.mimeTypes = mimeTypes;
        }

        @Override
        public boolean resolve(HttpServerExchange httpServerExchange) {
            final String contentType = httpServerExchange.getResponseHeaders().getFirst(Constant.CONTENT_TYPE);

            if (StringUtils.isNotEmpty(contentType)) {
                for (String mimeType : this.mimeTypes) {
                    if (matches(mimeType, contentType)) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected static boolean matches(String mimeType, String contentType) {
            if (contentType == null) {
                return false;
            }
            return isWildcardType(mimeType) || isWildcardType(contentType) || mimeType.equals(contentType);
        }
    }

}
