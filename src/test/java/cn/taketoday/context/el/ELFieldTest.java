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
package test.context.el;

import java.util.Date;

import javax.el.ELProcessor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.el.ValueELContext;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author TODAY <br>
 *         2019-02-23 20:58
 */
@ToString
@Singleton
public class ELFieldTest {

    private static long start;

    @BeforeClass
    public static void start() {
        start = System.currentTimeMillis();
    }

    @AfterClass
    public static void end() {

        System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
    }

    @After
    public void ends() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @Value("${235.1}")
    private double testDouble;

    @Value("${235.1}")
    private float testFloat;

    @Value(required = false, value = "${user}")
    private User user;

    @Value(value = "${env['site.name']}")
    private String siteName;

    StandardApplicationContext applicationContext = new StandardApplicationContext("");

    @Getter
    @Setter
    @ToString
    static class User {
        Integer id;
        String sex;
        Integer age;
        String passwd;
        Date brithday;
        String userId;
        String userName;
    }

    @Test
    public void test_Number() {

        applicationContext.loadContext("test.context.el");

        ELFieldTest bean = applicationContext.getBean(getClass());
        System.err.println(bean.testFloat);
        System.err.println(bean.testDouble);

        assert bean.testFloat == 235.1f;
        assert bean.testDouble == 235.1;
    }

    @Test
    public void testEnv() {

        ELProcessor processor = new ELProcessor();
        applicationContext.getEnvironment().setELProcessor(processor);

        processor.getELManager().setELContext(new ValueELContext(applicationContext));

        User user = new User();
        user.setAge(20)//
                .setBrithday(new Date())//
                .setId(1);

        processor.defineBean("user", user);

        applicationContext.loadContext("test.context.el");

        ELFieldTest bean = applicationContext.getBean(getClass());
        System.err.println(bean);
        assert bean.user == user;
        assert bean.siteName.equals("TODAY BLOG");
    }

    @Test
    public void testDefineBean() {

        ELProcessor processor = new ELProcessor();
        applicationContext.getEnvironment().setELProcessor(processor);

        processor.getELManager().setELContext(new ValueELContext(applicationContext));

        User user = new User();
        user.setAge(20)//
                .setBrithday(new Date())//
                .setId(1);

        processor.defineBean("user", user);

        applicationContext.loadContext("test.context.el");

        ELFieldTest bean = applicationContext.getBean(getClass());
        System.err.println(bean);
        assert bean.user == user;
    }

}
