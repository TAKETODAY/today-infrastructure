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
package cn.taketoday.context.bean;

import java.util.Objects;

import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.StringUtils;
import lombok.Getter;

/***
 * 
 * Reference to a bean
 * 
 * @author Today <br>
 *         2018-06-23 11:27:30
 */
@Getter
public final class BeanReference {

    /** reference name */
    private final String name;
    /** property is required? **/
    private final boolean required;

    /** record reference type @since v2.1.2 */
    private final Class<?> referenceClass;

    /** record if property is prototype @since v2.1.6 */
    private boolean prototype = false;

    public void applyPrototype() {
        this.prototype = true;
    }

    public BeanReference(String name, boolean required, Class<?> referenceClass) {
        this.name = name;
        if (StringUtils.isEmpty(name)) {
            throw new ContextException("Bean name can't be empty");
        }
        this.required = required;
        this.referenceClass = referenceClass;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, required, referenceClass, prototype);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof BeanReference) {
            BeanReference other = (BeanReference) obj;

            return (other.required != this.required && //
                    other.name.equals(this.name) && //
                    ContextUtils.equals(other.referenceClass, referenceClass));
        }

        return false;
    }

}
