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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.io;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.context.utils.ResourceUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author TODAY <br>
 *         2019-12-05 23:15
 */
public class PathMatchingResourcePatternResolverTest {

    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private static final String[] CLASSES_IN_IO = new String[] { // 
        "AbstractResource.class", //
        "ClassPathResource.class", //
        "EncodedResource.class", //
        "FileBasedResource.class", //
        "JarEntryResource.class", //
        "JarResource.class", //
        "PathMatchingResourcePatternResolver.class", // 
        "Readable.class", //
        "Resource.class", //
        "ResourceFilter.class", //
        "ResourceResolver.class", //
        "UrlBasedResource.class", //
        "Writable.class", //
        "WritableResource.class", //
        "PathMatchingResourcePatternResolverTest.class", //
        "JarEntryResource$JarEntryInputStream.class", //
    };

    private static final String[] CLASSES_IN_JUNIT_RUNNER = new String[] { //
        "BaseTestRunner.class", //
        "TestRunListener.class", //
        "Version.class", //
    };

//    @Test(expected = FileNotFoundException.class)
//    public void invalidPrefixWithPatternElementInIt() throws IOException {
//        resolver.getResources("xx**:**/*.xy");
//    }

    @Test
    public void singleResourceOnFileSystem() throws IOException {
        Resource[] resources = resolver.getResources("cn/taketoday/context/io/PathMatchingResourcePatternResolverTest.class");
        assertEquals(1, resources.length);
        assertTrue(resources[0].exists());
        assertProtocolAndFilenames(resources, "file", "PathMatchingResourcePatternResolverTest.class");
        
        
        // ---------------------------------------
        
        final Resource[] resources2 = ResourceUtils.getResources("cn/taketoday/context/io/PathMatchingResourcePatternResolverTest.class");
        assertEquals(1, resources2.length);
        assertTrue(resources2[0].exists());
        assertProtocolAndFilenames(resources2, "file", "PathMatchingResourcePatternResolverTest.class");
    }

    @Test
    public void singleResourceInJar() throws IOException {
        Resource[] resources = resolver.getResources("org/junit/Assert.class");
        assertEquals(1, resources.length);
        assertProtocolAndFilenames(resources, "jar", "Assert.class");
    }

    @Test
    public void classpathStarWithPatternOnFileSystem() throws IOException {
        Resource[] resources = resolver.getResources("classpath*:cn/taketoday/context/io/*.class");
        // Have to exclude Clover-generated class files here,
        // as we might be running as part of a Clover test run.
        List<Resource> noCloverResources = new ArrayList<>();
        for (Resource resource : resources) {
            if (!resource.getName().contains("$__CLOVER_")) {
                noCloverResources.add(resource);
            }
        }

        resources = noCloverResources.toArray(new Resource[noCloverResources.size()]);
        assertProtocolAndFilenames(resources, "file", CLASSES_IN_IO);
    }

    @Test
    public void classpathWithPatternInJar() throws IOException {
        Resource[] resources = resolver.getResources("classpath:junit/runner/*.class");
        assertProtocolAndFilenames(resources, "jar", CLASSES_IN_JUNIT_RUNNER);
    }

    @Test
    public void classpathStarWithPatternInJar() throws IOException {
        Resource[] resources = resolver.getResources("classpath*:junit/runner/*.class");
        assertProtocolAndFilenames(resources, "jar", CLASSES_IN_JUNIT_RUNNER);
    }

    @Test
    public void rootPatternRetrievalInJarFiles() throws IOException {
        Resource[] resources = resolver.getResources("classpath*:**/pom.properties");
        boolean found = false;
        for (Resource resource : resources) {
            if (resource.getName().endsWith("pom.properties")) {
                found = true;
            }
        }
        assertTrue("Could not find pom.properties", found);
    }

    private void assertProtocolAndFilenames(Resource[] resources,
                                            String protocol,
                                            String... filenames) throws IOException {

        assertEquals("Correct number of files found", filenames.length, resources.length);
        for (Resource resource : resources) {
            String actualProtocol = resource.getLocation().getProtocol();
            assertEquals(protocol, actualProtocol);
            assertFilenameIn(resource, filenames);
        }
    }

    private void assertFilenameIn(Resource resource, String... filenames) {
        String filename = resource.getName();
        assertTrue(resource + " does not have a filename that matches any of the specified names",
                   Arrays.stream(filenames).anyMatch(filename::endsWith));
    }

}
