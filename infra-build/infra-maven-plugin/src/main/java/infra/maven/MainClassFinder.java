/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.maven;

import org.eclipse.sisu.space.asm.Type;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassVisitor;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;
import infra.lang.Nullable;

/**
 * Finds any class with a {@code public static main} method by performing a breadth first
 * search.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class MainClassFinder {

  private static final String DOT_CLASS = ".class";

  private static final Type STRING_ARRAY_TYPE = Type.getType(String[].class);

  private static final Type MAIN_METHOD_TYPE = Type.getMethodType(Type.VOID_TYPE, STRING_ARRAY_TYPE);

  private static final String MAIN_METHOD_NAME = "main";

  private static final FileFilter CLASS_FILE_FILTER = MainClassFinder::isClassFile;

  private static final FileFilter PACKAGE_DIRECTORY_FILTER = MainClassFinder::isPackageDirectory;

  private static boolean isClassFile(File file) {
    return file.isFile() && file.getName().endsWith(DOT_CLASS);
  }

  private static boolean isPackageDirectory(File file) {
    return file.isDirectory() && !file.getName().startsWith(".");
  }

  /**
   * Find the main class from a given directory.
   *
   * @param rootDirectory the root directory to search
   * @return the main class or {@code null}
   * @throws IOException if the directory cannot be read
   */
  public static String findMainClass(File rootDirectory) throws IOException {
    return doWithMainClasses(rootDirectory, MainClass::getName);
  }

  /**
   * Find a single main class from the given {@code rootDirectory}.
   *
   * @param rootDirectory the root directory to search
   * @return the main class or {@code null}
   * @throws IOException if the directory cannot be read
   */
  public static String findSingleMainClass(File rootDirectory) throws IOException {
    return findSingleMainClass(rootDirectory, null);
  }

  /**
   * Find a single main class from the given {@code rootDirectory}. A main class
   * annotated with an annotation with the given {@code annotationName} will be
   * preferred over a main class with no such annotation.
   *
   * @param rootDirectory the root directory to search
   * @param annotationName the name of the annotation that may be present on the main
   * class
   * @return the main class or {@code null}
   * @throws IOException if the directory cannot be read
   */
  @Nullable
  public static String findSingleMainClass(File rootDirectory, String annotationName) throws IOException {
    SingleMainClassCallback callback = new SingleMainClassCallback(annotationName);
    MainClassFinder.doWithMainClasses(rootDirectory, callback);
    return callback.getMainClassName();
  }

  /**
   * Perform the given callback operation on all main classes from the given root
   * directory.
   *
   * @param <T> the result type
   * @param rootDirectory the root directory
   * @param callback the callback
   * @return the first callback result or {@code null}
   * @throws IOException in case of I/O errors
   */
  @Nullable
  static <T> T doWithMainClasses(File rootDirectory, MainClassCallback<T> callback) throws IOException {
    if (!rootDirectory.exists()) {
      return null; // nothing to do
    }
    if (!rootDirectory.isDirectory()) {
      throw new IllegalArgumentException("Invalid root directory '" + rootDirectory + "'");
    }
    String prefix = rootDirectory.getAbsolutePath() + "/";
    Deque<File> stack = new ArrayDeque<>();
    stack.push(rootDirectory);
    while (!stack.isEmpty()) {
      File file = stack.pop();
      if (file.isFile()) {
        try (InputStream inputStream = new FileInputStream(file)) {
          ClassDescriptor classDescriptor = createClassDescriptor(inputStream);
          if (classDescriptor != null && classDescriptor.isMainMethodFound()) {
            String className = convertToClassName(file.getAbsolutePath(), prefix);
            T result = callback.doWith(new MainClass(className, classDescriptor.getAnnotationNames()));
            if (result != null) {
              return result;
            }
          }
        }
      }
      if (file.isDirectory()) {
        pushAllSorted(stack, file.listFiles(PACKAGE_DIRECTORY_FILTER));
        pushAllSorted(stack, file.listFiles(CLASS_FILE_FILTER));
      }
    }
    return null;
  }

  private static void pushAllSorted(Deque<File> stack, File[] files) {
    Arrays.sort(files, Comparator.comparing(File::getName));
    for (File file : files) {
      stack.push(file);
    }
  }

  /**
   * Find the main class in a given jar file.
   *
   * @param jarFile the jar file to search
   * @param classesLocation the location within the jar containing classes
   * @return the main class or {@code null}
   * @throws IOException if the jar file cannot be read
   */
  public static String findMainClass(JarFile jarFile, String classesLocation) throws IOException {
    return doWithMainClasses(jarFile, classesLocation, MainClass::getName);
  }

  /**
   * Find a single main class in a given jar file.
   *
   * @param jarFile the jar file to search
   * @param classesLocation the location within the jar containing classes
   * @return the main class or {@code null}
   * @throws IOException if the jar file cannot be read
   */
  @Nullable
  public static String findSingleMainClass(JarFile jarFile, String classesLocation) throws IOException {
    return findSingleMainClass(jarFile, classesLocation, null);
  }

  /**
   * Find a single main class in a given jar file. A main class annotated with an
   * annotation with the given {@code annotationName} will be preferred over a main
   * class with no such annotation.
   *
   * @param jarFile the jar file to search
   * @param classesLocation the location within the jar containing classes
   * @param annotationName the name of the annotation that may be present on the main
   * class
   * @return the main class or {@code null}
   * @throws IOException if the jar file cannot be read
   */
  @Nullable
  public static String findSingleMainClass(
          JarFile jarFile, String classesLocation, String annotationName) throws IOException {
    SingleMainClassCallback callback = new SingleMainClassCallback(annotationName);
    MainClassFinder.doWithMainClasses(jarFile, classesLocation, callback);
    return callback.getMainClassName();
  }

  /**
   * Perform the given callback operation on all main classes from the given jar.
   *
   * @param <T> the result type
   * @param jarFile the jar file to search
   * @param classesLocation the location within the jar containing classes
   * @param callback the callback
   * @return the first callback result or {@code null}
   * @throws IOException in case of I/O errors
   */
  @Nullable
  static <T> T doWithMainClasses(JarFile jarFile,
          String classesLocation, MainClassCallback<T> callback) throws IOException {
    List<JarEntry> classEntries = getClassEntries(jarFile, classesLocation);
    classEntries.sort(new ClassEntryComparator());
    for (JarEntry entry : classEntries) {
      try (InputStream inputStream = new BufferedInputStream(jarFile.getInputStream(entry))) {
        ClassDescriptor classDescriptor = createClassDescriptor(inputStream);
        if (classDescriptor != null && classDescriptor.isMainMethodFound()) {
          String className = convertToClassName(entry.getName(), classesLocation);
          T result = callback.doWith(new MainClass(className, classDescriptor.getAnnotationNames()));
          if (result != null) {
            return result;
          }
        }
      }
    }
    return null;
  }

  private static String convertToClassName(String name, String prefix) {
    name = name.replace('/', '.');
    name = name.replace('\\', '.');
    name = name.substring(0, name.length() - DOT_CLASS.length());
    if (prefix != null) {
      name = name.substring(prefix.length());
    }
    return name;
  }

  private static List<JarEntry> getClassEntries(JarFile source, String classesLocation) {
    classesLocation = (classesLocation != null) ? classesLocation : "";
    Enumeration<JarEntry> sourceEntries = source.entries();
    List<JarEntry> classEntries = new ArrayList<>();
    while (sourceEntries.hasMoreElements()) {
      JarEntry entry = sourceEntries.nextElement();
      if (entry.getName().startsWith(classesLocation) && entry.getName().endsWith(DOT_CLASS)) {
        classEntries.add(entry);
      }
    }
    return classEntries;
  }

  @Nullable
  private static ClassDescriptor createClassDescriptor(InputStream inputStream) {
    try {
      ClassReader classReader = new ClassReader(inputStream);
      ClassDescriptor classDescriptor = new ClassDescriptor();
      classReader.accept(classDescriptor, ClassReader.SKIP_CODE);
      return classDescriptor;
    }
    catch (IOException ex) {
      return null;
    }
  }

  private static class ClassEntryComparator implements Comparator<JarEntry> {

    @Override
    public int compare(JarEntry o1, JarEntry o2) {
      Integer d1 = getDepth(o1);
      Integer d2 = getDepth(o2);
      int depthCompare = d1.compareTo(d2);
      if (depthCompare != 0) {
        return depthCompare;
      }
      return o1.getName().compareTo(o2.getName());
    }

    private int getDepth(JarEntry entry) {
      return entry.getName().split("/").length;
    }

  }

  private static class ClassDescriptor extends ClassVisitor {

    private final Set<String> annotationNames = new LinkedHashSet<>();

    private boolean mainMethodFound;

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      this.annotationNames.add(Type.getType(desc).getClassName());
      return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if (isAccess(access, Opcodes.ACC_PUBLIC, Opcodes.ACC_STATIC)
              && MAIN_METHOD_NAME.equals(name)
              && MAIN_METHOD_TYPE.getDescriptor().equals(desc)) {
        this.mainMethodFound = true;
      }
      return null;
    }

    private boolean isAccess(int access, int... requiredOpsCodes) {
      for (int requiredOpsCode : requiredOpsCodes) {
        if ((access & requiredOpsCode) == 0) {
          return false;
        }
      }
      return true;
    }

    boolean isMainMethodFound() {
      return this.mainMethodFound;
    }

    Set<String> getAnnotationNames() {
      return this.annotationNames;
    }

  }

  /**
   * Callback for handling {@link MainClass MainClasses}.
   *
   * @param <T> the callback's return type
   */
  interface MainClassCallback<T> {

    /**
     * Handle the specified main class.
     *
     * @param mainClass the main class
     * @return a non-null value if processing should end or {@code null} to continue
     */
    @Nullable
    T doWith(MainClass mainClass);

  }

  /**
   * A class with a {@code main} method.
   */
  static final class MainClass {

    public final String name;

    public final Set<String> annotationNames;

    /**
     * Creates a new {@code MainClass} rather represents the main class with the given
     * {@code name}. The class is annotated with the annotations with the given
     * {@code annotationNames}.
     *
     * @param name the name of the class
     * @param annotationNames the names of the annotations on the class
     */
    MainClass(String name, Set<String> annotationNames) {
      this.name = name;
      this.annotationNames = Set.copyOf(annotationNames);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MainClass other = (MainClass) obj;
      return this.name.equals(other.name);
    }

    public String getName() {
      return name;
    }

    @Override
    public int hashCode() {
      return this.name.hashCode();
    }

    @Override
    public String toString() {
      return this.name;
    }

  }

  /**
   * Find a single main class, throwing an {@link IllegalStateException} if multiple
   * candidates exist.
   */
  private static final class SingleMainClassCallback implements MainClassCallback<Object> {

    private final Set<MainClass> mainClasses = new LinkedHashSet<>();

    private final String annotationName;

    private SingleMainClassCallback(String annotationName) {
      this.annotationName = annotationName;
    }

    @Override
    @Nullable
    public Object doWith(MainClass mainClass) {
      this.mainClasses.add(mainClass);
      return null;
    }

    @Nullable
    private String getMainClassName() {
      var matchingMainClasses = new LinkedHashSet<MainClass>();
      if (this.annotationName != null) {
        for (MainClass mainClass : this.mainClasses) {
          if (mainClass.annotationNames.contains(this.annotationName)) {
            matchingMainClasses.add(mainClass);
          }
        }
      }
      if (matchingMainClasses.isEmpty()) {
        matchingMainClasses.addAll(this.mainClasses);
      }
      if (matchingMainClasses.size() > 1) {
        throw new IllegalStateException(
                "Unable to find a single main class from the following candidates " + matchingMainClasses);
      }
      return matchingMainClasses.isEmpty() ? null : matchingMainClasses.iterator().next().name;
    }

  }

}
