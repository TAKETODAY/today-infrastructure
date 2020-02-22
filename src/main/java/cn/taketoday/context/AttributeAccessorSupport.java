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
package cn.taketoday.context;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.StringUtils;

/**
 * Support class for {@link AttributeAccessor AttributeAccessors}, providing a
 * base implementation of all methods. To be extended by subclasses.
 *
 * <p>
 * {@link Serializable} if subclasses and all attribute values are
 * {@link Serializable}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.1.7
 * @author TODAY <br>
 *         2020-02-22 12:47
 */
public abstract class AttributeAccessorSupport implements AttributeAccessor, Serializable {
    private static final long serialVersionUID = 1L;

    /** Map with String keys and Object values. */
    private LinkedHashMap<String, Object> attributes;

    @Override
    public void setAttribute(String name, Object value) {
        Assert.notNull(name, "Name must not be null");
        if (value != null) {
            getAttributes().put(name, value);
        }
        else {
            removeAttribute(name);
        }
    }

    @Override
    public Object getAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return getAttributes().get(name);
    }

    @Override
    public Object removeAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return getAttributes().remove(name);
    }

    @Override
    public boolean hasAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return getAttributes().containsKey(name);
    }

    @Override
    public String[] attributeNames() {
        return StringUtils.toStringArray(getAttributes().keySet());
    }

    /**
     * Copy the attributes from the supplied AttributeAccessor to this accessor.
     * 
     * @param source
     *            the AttributeAccessor to copy from
     */
    protected void copyAttributesFrom(AttributeAccessor source) {
        Assert.notNull(source, "Source must not be null");
        String[] attributeNames = source.attributeNames();
        for (String attributeName : attributeNames) {
            setAttribute(attributeName, source.getAttribute(attributeName));
        }
    }

    @Override
    public boolean equals(Object other) {
        return (this == other
                || (other instanceof AttributeAccessorSupport &&
                    getAttributes().equals(((AttributeAccessorSupport) other).getAttributes())));
    }

    @Override
    public int hashCode() {
        return getAttributes().hashCode();
    }

    public Map<String, Object> getAttributes() {
        final LinkedHashMap<String, Object> attributes = this.attributes;
        if (attributes == null) {
            return this.attributes = new LinkedHashMap<>();
        }
        return attributes;
    }

}
