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
package cn.taketoday.web.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * Store {@link HandlerMethod}
 * 
 * @author TODAY <br>
 *         2018-07-1 20:47:06
 */
@Singleton
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HandlerMethodRegistry implements HandlerRegistry {

//    private static final Logger log = LoggerFactory.getLogger(HandlerMethodRegistry.class);

    /** regex **/
    private RegexMapping[] regexMappings;

    private final Map<String, HandlerMethod> handlerMappings;

    public HandlerMethodRegistry() {
        this(new HashMap<>(1024));
    }

    public HandlerMethodRegistry(int initialCapacity) {
        this(new HashMap<>(initialCapacity));
    }

    public HandlerMethodRegistry(Map<String, HandlerMethod> mappings) {
        this.handlerMappings = mappings;
    }

    public HandlerMethodRegistry setRegexMappings(Map<String, HandlerMethod> regexMappings) {
        this.regexMappings = new RegexMapping[regexMappings.size()];
        int i = 0;
        for (Entry<String, HandlerMethod> entry : regexMappings.entrySet()) {
            this.regexMappings[i++] = new RegexMapping(Pattern.compile(entry.getKey()), entry.getValue());
        }
        return this;
    }

    public final RegexMapping[] getRegexMappings() {
        return regexMappings;
    }

    /**
     * Get HandlerMapping count.
     * 
     * @return HandlerMapping count
     */
    public int size() {
        return handlerMappings.size();
    }

    public final HandlerMethod get(String key) {
        return handlerMappings.get(key);
    }

    @Override
    public String toString() {
        return handlerMappings.toString();
    }

    public final Map<String, HandlerMethod> getHandlerMappings() {
        return handlerMappings;
    }

    /**
     * Looking for {@link InterceptableHandlerMethod}
     * 
     * @param context
     * 
     * @return mapped {@link InterceptableHandlerMethod}
     * @since 2.3.7
     */
    @Override
    public Object lookup(final RequestContext context) {

        String key = context.method().concat(context.requestURI());
        final HandlerMethod ret = handlerMappings.get(key); // handler mapping

        if (ret == null) {
            // path variable
            key = StringUtils.decodeUrl(key);// decode
            for (final RegexMapping regex : regexMappings) {
                // TODO path matcher pathMatcher.match(requestURI, requestURI)
                if (regex.pattern.matcher(key).matches()) {
                    return regex.handlerMapping;
                }
            }
            return null;
        }
        return ret;
    }

    static final class RegexMapping {

        /**
         * @since 2.3.7
         */
        public final Pattern pattern;
        public final HandlerMethod handlerMapping;

        public RegexMapping(Pattern pattern, HandlerMethod handlerMapping) {
            this.pattern = pattern;
            this.handlerMapping = handlerMapping;
        }
    }

}
