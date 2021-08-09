/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.factory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-02-01 10:48
 */
@Setter
@Getter
public class BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    private long start;

    private static ConfigurableApplicationContext applicationContext = //
            new StandardApplicationContext("", "cn.taketoday.context.factory", "test.demo.config");

    private String process;

    @Setter
    @Getter
    private static ConfigurableBeanFactory beanFactory;

    static {
        setBeanFactory(getApplicationContext().getBeanFactory());
    }

    public static ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Before
    public void start() {
        setStart(System.currentTimeMillis());
    }

    @After
    public void end() {
        log.debug("process: [{}] takes {} ms.", getProcess(), (System.currentTimeMillis() - getStart()));
    }

    @AfterClass
    public static void endClass() {
        ConfigurableApplicationContext applicationContext = getApplicationContext();
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}
