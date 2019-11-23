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
import java.util.RandomAccess;
import java.util.regex.Pattern;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.web.Constant;

/**
 * Store {@link HandlerMapping}
 * 
 * @author TODAY <br>
 *         2018-07-1 20:47:06
 */
@MissingBean(value = Constant.HANDLER_MAPPING_REGISTRY, type = HandlerMappingRegistry.class)
public class HandlerMappingRegistry implements RandomAccess {

    /** regex **/
    private RegexMapping[] regexMappings;

    private final Map<String, HandlerMapping> handlerMappings;

    public HandlerMappingRegistry() {
        this(new HashMap<>(1024));
    }

    public HandlerMappingRegistry(int initialCapacity) {
        this(new HashMap<>(initialCapacity));
    }

    public HandlerMappingRegistry(Map<String, HandlerMapping> mappings) {
        this.handlerMappings = mappings;
    }

    public HandlerMappingRegistry setRegexMappings(Map<String, HandlerMapping> regexMappings) {
        this.regexMappings = new RegexMapping[regexMappings.size()];
        int i = 0;
        for (Entry<String, HandlerMapping> entry : regexMappings.entrySet()) {
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

    public final HandlerMapping get(String key) {
        return handlerMappings.get(key);
    }

    @Override
    public String toString() {
        return handlerMappings.toString();
    }

    public final Map<String, HandlerMapping> getHandlerMappings() {
        return handlerMappings;
    }

}
