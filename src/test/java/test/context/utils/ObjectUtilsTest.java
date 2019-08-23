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
package test.context.utils;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ObjectUtils;

/**
 * 
 * @author Today <br>
 *         2018-07-12 20:46:41
 */
public class ObjectUtilsTest {

    private long start;

    @Before
    public void start() {
        start = System.currentTimeMillis();
    }

    @After
    public void end() {
        System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
    }

    @Test
    public void testIsEmpty() throws ConfigurationException {
        // null
        assert ObjectUtils.isEmpty(null);
        assert !ObjectUtils.isNotEmpty(null);
        assert ObjectUtils.isEmpty((Object) null);
        assert !ObjectUtils.isNotEmpty((Object) null);

        // string array
        assert ObjectUtils.isEmpty(new String[0]);
        assert !ObjectUtils.isNotEmpty(new String[0]);
        assert !ObjectUtils.isEmpty(new String[] { "TODAY" });
        assert !ObjectUtils.isEmpty((Object) new String[] { "TODAY" });

        assert !ObjectUtils.isEmpty("TODAY");
        assert ObjectUtils.isNotEmpty("TODAY");
        assert ObjectUtils.isNotEmpty("TODAY");

        // collections
        assert ObjectUtils.isEmpty(Collections.emptySet());
        assert !ObjectUtils.isNotEmpty(Collections.emptySet());
        assert ObjectUtils.isEmpty(Collections.emptyMap());
        assert !ObjectUtils.isNotEmpty(Collections.emptyMap());
        assert ObjectUtils.isEmpty(Collections.emptyList());
        assert !ObjectUtils.isNotEmpty(Collections.emptyList());

    }

}
