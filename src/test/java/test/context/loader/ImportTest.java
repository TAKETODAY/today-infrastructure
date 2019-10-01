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
package test.context.loader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.annotation.PreDestroy;

import org.junit.Test;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.loader.ImportSelector;

/**
 * @author TODAY <br>
 *         2019-10-01 22:19
 */
public class ImportTest {

    @Import(TEST.class)
    public static class ErrorImportTESTBean {

    }

    @Singleton
    @Import(TEST.class)
    public static class ImportTESTBean {

    }

    public static class TEST {

        @Singleton
        @Import(TEST.class)
        public Object objTest() {

            return new Object() {
                @PreDestroy
                void destroy() {
                    System.err.println("objTest destroy");
                }
            };
        }
    }

    @Configuration
    public static class ImportConfigurationBean {

        @Singleton
        @Import(TEST.class)
        public Object obj() {

            return new Object() {

                @PreDestroy
                void destroy() {
                    System.err.println("obj destroy");
                }
            };
        }
    }

    // ImportSelector
    // -------------------------------

    public static class TESTSelector implements ImportSelector {

        @Override
        public String[] selectImports(BeanDefinition importingDef) {
            
            return null;
        }
    }

    //
    @Test
    public void testImportConfiguration() throws BeanDefinitionStoreException, ConfigurationException {

        try (ConfigurableApplicationContext applicationContext = //
                new StandardApplicationContext("", "test.context.loader")) {

            assertFalse(applicationContext.containsBeanDefinition(ErrorImportTESTBean.class));
            assertTrue(applicationContext.containsBeanDefinition(ImportTESTBean.class));
            assertTrue(applicationContext.containsBeanDefinition(TEST.class));

        }
    }

}
