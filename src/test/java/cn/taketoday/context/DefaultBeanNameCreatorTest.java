/*
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
package cn.taketoday.context;

import org.junit.Test;

import cn.taketoday.beans.DefaultBeanNameCreator;

import static org.junit.Assert.assertEquals;

/**
 * @author TODAY <br>
 *         2020-02-25 12:40
 */
public class DefaultBeanNameCreatorTest {

    
    static class test {
        
    }
    
    @Test
    public void testDefaultBeanNameCreator() {
        DefaultBeanNameCreator nameCreator = new DefaultBeanNameCreator();
        
        final String create = nameCreator.create(DefaultBeanNameCreatorTest.class);
       
        assertEquals(create, "defaultBeanNameCreatorTest");
        assertEquals(nameCreator.create(test.class), "test");
        
        DefaultBeanNameCreator fullNameCreator = new DefaultBeanNameCreator(false);
        
        final String full = fullNameCreator.create(DefaultBeanNameCreatorTest.class);
       
        assertEquals(full, "cn.taketoday.context.DefaultBeanNameCreatorTest");
    }

}
