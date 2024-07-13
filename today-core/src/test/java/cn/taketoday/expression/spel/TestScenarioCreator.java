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

package cn.taketoday.expression.spel;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.GregorianCalendar;

import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.expression.spel.testresources.Inventor;
import cn.taketoday.expression.spel.testresources.PlaceOfBirth;

/**
 * Builds an evaluation context for test expressions.
 * Features of the test evaluation context are:
 * <ul>
 * <li>The root context object is an Inventor instance {@link Inventor}
 * </ul>
 */
class TestScenarioCreator {

  public static StandardEvaluationContext getTestEvaluationContext() {
    StandardEvaluationContext testContext = new StandardEvaluationContext();
    setupRootContextObject(testContext);
    populateVariables(testContext);
    populateFunctions(testContext);
    try {
      populateMethodHandles(testContext);
    }
    catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return testContext;
  }

  /**
   * Register some Java reflect methods as well known functions that can be called from an expression.
   *
   * @param testContext the test evaluation context
   */
  private static void populateFunctions(StandardEvaluationContext testContext) {
    try {
      testContext.registerFunction("isEven",
              TestScenarioCreator.class.getDeclaredMethod("isEven", int.class));
      testContext.registerFunction("reverseInt",
              TestScenarioCreator.class.getDeclaredMethod("reverseInt", int.class, int.class, int.class));
      testContext.registerFunction("reverseString",
              TestScenarioCreator.class.getDeclaredMethod("reverseString", String.class));
      testContext.registerFunction("varargsFunction",
              TestScenarioCreator.class.getDeclaredMethod("varargsFunction", String[].class));
      testContext.registerFunction("varargsFunction2",
              TestScenarioCreator.class.getDeclaredMethod("varargsFunction2", int.class, String[].class));
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Register some Java {@code MethodHandle} as well known functions that can be called from an expression.
   *
   * @param testContext the test evaluation context
   */
  private static void populateMethodHandles(StandardEvaluationContext testContext) throws NoSuchMethodException, IllegalAccessException {
    // #message(template, args...)
    MethodHandle message = MethodHandles.lookup().findVirtual(String.class, "formatted",
            MethodType.methodType(String.class, Object[].class));
    testContext.registerFunction("message", message);
    // #messageTemplate(args...)
    MethodHandle messageWithParameters = message.bindTo("This is a %s message with %s words: <%s>");
    testContext.registerFunction("messageTemplate", messageWithParameters);
    // #messageTemplateBound()
    MethodHandle messageBound = messageWithParameters
            .bindTo(new Object[] { "prerecorded", 3, "Oh Hello World", "ignored" });
    testContext.registerFunction("messageBound", messageBound);

    //#messageStatic(template, args...)
    MethodHandle messageStatic = MethodHandles.lookup().findStatic(TestScenarioCreator.class,
            "message", MethodType.methodType(String.class, String.class, String[].class));
    testContext.registerFunction("messageStatic", messageStatic);
    //#messageStaticTemplate(args...)
    MethodHandle messageStaticPartiallyBound = messageStatic.bindTo("This is a %s message with %s words: <%s>");
    testContext.registerFunction("messageStaticTemplate", messageStaticPartiallyBound);
    //#messageStaticBound()
    MethodHandle messageStaticFullyBound = messageStaticPartiallyBound
            .bindTo(new String[] { "prerecorded", "3", "Oh Hello World", "ignored" });
    testContext.registerFunction("messageStaticBound", messageStaticFullyBound);

    // #formatObjectVarargs(format, args...)
    MethodHandle formatObjectVarargs = MethodHandles.lookup().findStatic(TestScenarioCreator.class,
            "formatObjectVarargs", MethodType.methodType(String.class, String.class, Object[].class));
    testContext.registerFunction("formatObjectVarargs", formatObjectVarargs);

    // #formatObjectVarargs(format, args...)
    MethodHandle formatPrimitiveVarargs = MethodHandles.lookup().findStatic(TestScenarioCreator.class,
            "formatPrimitiveVarargs", MethodType.methodType(String.class, String.class, int[].class));
    testContext.registerFunction("formatPrimitiveVarargs", formatPrimitiveVarargs);

    // #add(int, int)
    MethodHandle add = MethodHandles.lookup().findStatic(TestScenarioCreator.class,
            "add", MethodType.methodType(int.class, int.class, int.class));
    testContext.registerFunction("add", add);
  }

  /**
   * Register some variables that can be referenced from the tests
   *
   * @param testContext the test evaluation context
   */
  private static void populateVariables(StandardEvaluationContext testContext) {
    testContext.setVariable("answer", 42);
  }

  /**
   * Create the root context object, an Inventor instance. Non-qualified property
   * and method references will be resolved against this context object.
   *
   * @param context the evaluation context in which to set the root object
   */
  private static void setupRootContextObject(StandardEvaluationContext context) {
    GregorianCalendar c = new GregorianCalendar();
    c.set(1856, 7, 9);
    Inventor tesla = new Inventor("Nikola Tesla", c.getTime(), "Serbian");
    tesla.setPlaceOfBirth(new PlaceOfBirth("SmilJan"));
    tesla.setInventions("Telephone repeater", "Rotating magnetic field principle",
            "Polyphase alternating-current system", "Induction motor", "Alternating-current power transmission",
            "Tesla coil transformer", "Wireless communication", "Radio", "Fluorescent lights");
    context.setRootObject(tesla);
  }

  // These methods are registered in the test context and therefore accessible through function calls
  // in test expressions

  public static String isEven(int i) {
    return ((i % 2) == 0 ? "y" : "n");
  }

  public static int[] reverseInt(int i, int j, int k) {
    return new int[] { k, j, i };
  }

  public static String reverseString(String input) {
    return new StringBuilder(input).reverse().toString();
  }

  public static String varargsFunction(String... strings) {
    return Arrays.toString(strings);
  }

  public static String varargsFunction2(int i, String... strings) {
    return i + "-" + Arrays.toString(strings);
  }

  public static String message(String template, String... args) {
    return template.formatted((Object[]) args);
  }

  public static String formatObjectVarargs(String format, Object... args) {
    return String.format(format, args);
  }

  public static String formatPrimitiveVarargs(String format, int... nums) {
    Object[] args = new Object[nums.length];
    for (int i = 0; i < nums.length; i++) {
      args[i] = nums[i];
    }
    return String.format(format, args);
  }

  public static int add(int x, int y) {
    return x + y;
  }

}
