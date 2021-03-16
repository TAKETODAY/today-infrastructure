/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.event.ApplicationListener;

/**
 * 
 * @author Today <br>
 *         2018-11-08 20:31
 */
public class ApplicationListenerTest {

    private long start;

    @Before
    public void start() {
        start = System.currentTimeMillis();
    }

    @After
    public void end() {
        System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
    }

    boolean i = false;

    @Test
    public void testAddApplicationListener() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

        try (ConfigurableApplicationContext applicationContext = new StandardApplicationContext()) {

            applicationContext.addApplicationListener(new ApplicationListener<ContextStartedEvent>() {

                @Override
                public void onApplicationEvent(ContextStartedEvent event) {
                    i = true;
                    System.err.println(i);
                }
            });

            applicationContext.loadContext();

            assert i;
        }
    }

    @Test
    public void testLoadMetaInfoListeners() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

        try (ConfigurableApplicationContext applicationContext = new StandardApplicationContext()) {
            applicationContext.loadContext();
        } // auto close

        assert testLoadedMetaInfoListener;
    }

    private static boolean testLoadedMetaInfoListener = false;

    public static class ContextCloseMetaInfoListener implements ApplicationListener<ContextCloseEvent>, Ordered {

        @Override
        public void onApplicationEvent(ContextCloseEvent event) {
            System.err.println("context is closing");
            testLoadedMetaInfoListener = true;
        }

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE;
        }
    }

}
