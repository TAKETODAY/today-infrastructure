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
package test.context.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.InvalidPathException;

import org.junit.Test;

import cn.taketoday.context.io.JarEntryResource;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY <br>
 *         2019-05-15 14:04
 */
public class ResourceUtilsTest {

    @Test
    public void testGetRelativePath() throws IOException {
        final String relativePath = ResourceUtils.getRelativePath("D:/java/", "1.txt");
        final String relativePath1 = ResourceUtils.getRelativePath("D:/java", "1.txt");
        final String relativePath2 = ResourceUtils.getRelativePath("D:/java/2.txt", "1.txt");

        System.err.println(relativePath);
        assert relativePath.equals("D:/java/1.txt");

        System.err.println(relativePath1);
        assert relativePath1.equals("D:/1.txt");

        System.err.println(relativePath2);
        assert relativePath2.equals("D:/java/1.txt");

        assert ResourceUtils.getRelativePath("index", "TODAY").equals("TODAY");

    }

    @Test
    public void testGetResource() throws IOException {

//		final Resource resource = ResourceUtils.getResource("/META-INF/maven/cn.taketoday/today-expression/pom.properties");
        Resource resource = ResourceUtils.getResource("classpath:/META-INF/maven/cn.taketoday/today-expression/pom.properties");

        System.err.println(resource);
        Resource createRelative = resource.createRelative("pom.xml");
        System.err.println(createRelative);

        assert createRelative.exists();
        assert resource.exists();

        resource = ResourceUtils.getResource("file:/G:/Projects/Git/github/today-context/src/main/resources/META-INF/ignore/jar-prefix");

        System.err.println(resource);

        assert resource.exists();

        System.err.println(StringUtils.readAsText(resource.getInputStream()));

        resource = ResourceUtils.getResource("jar:file:/G:/Projects/Git/github/today-context/src/test/resources/test.jar!/META-INF/");
        System.err.println(resource);

        if (resource instanceof JarEntryResource) {

            JarEntryResource jarEntryResource = (JarEntryResource) resource.createRelative(
                    "/maven/cn.taketoday/today-expression/pom.properties");
            if (jarEntryResource.exists()) {
                System.out.println(StringUtils.readAsText(jarEntryResource.getInputStream()));
            }

            System.err.println(jarEntryResource);
        }
        // location is empty
        final Resource classpath = ResourceUtils.getResource("");
        assert classpath.createRelative("/info.properties").exists();
        // start with '/'
        assert ResourceUtils.getResource("/info.properties").exists();
        assert ResourceUtils.getResource("classpath:info.properties").exists();

        try {
            ResourceUtils.getResource("today://info");
        }
        catch (InvalidPathException e) {
            System.err.println(e);
        }
        ResourceUtils.getResource("info.properties");

        try {
            ResourceUtils.getResource("info"); // ConfigurationException
        }
        catch (FileNotFoundException e) {
            System.err.println(e);
        }

        // getResource(URL)

//        final Resource taketoday = ResourceUtils.getResource(new URL("https://taketoday.cn"));
//
//        assert taketoday.exists();
//        assert StringUtils.readAsText(taketoday.getInputStream()) != null;
//        System.err.println(StringUtils.readAsText(taketoday.getInputStream()));

    }

}
