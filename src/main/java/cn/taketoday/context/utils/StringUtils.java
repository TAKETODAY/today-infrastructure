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
package cn.taketoday.context.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import cn.taketoday.context.Constant;

/**
 * @author TODAY <br>
 *         2018-06-26 21:19:09
 */
public abstract class StringUtils {

    private static final int caseDiff = ('a' - 'A');
    private static BitSet dontNeedEncoding;

    static {

        /* The list of characters that are not encoded has been determined as follows:
         *
         * RFC 2396 states: ----- Data characters that are allowed in a URI but do not
         * have a reserved purpose are called unreserved. These include upper and lower
         * case letters, decimal digits, and a limited set of punctuation marks and
         * symbols.
         *
         * unreserved = alphanum | mark
         *
         * mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
         *
         * Unreserved characters can be escaped without changing the semantics of the
         * URI, but this should not be done unless the URI is being used in a context
         * that does not allow the unescaped character to appear. -----
         *
         * It appears that both Netscape and Internet Explorer escape all special
         * characters from this list with the exception of "-", "_", ".", "*". While it
         * is not clear why they are escaping the other characters, perhaps it is safest
         * to assume that there might be contexts in which the others are unsafe if not
         * escaped. Therefore, we will use the same list. It is also noteworthy that
         * this is consistent with O'Reilly's "HTML: The Definitive Guide" (page 164).
         *
         * As a last note, Intenet Explorer does not encode the "@" character which is
         * clearly not unreserved according to the RFC. We are being consistent with the
         * RFC in this matter, as is Netscape. */
        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set(' '); // encoding a space to a + is done in the encode() method

        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('*');
    }

    public final static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public final static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public final static boolean isArrayNotEmpty(String... strs) {
        return strs != null && strs.length != 0;
    }

    public final static boolean isArrayEmpty(String... strs) {
        return strs == null || strs.length == 0;
    }

    /**
     * Split with {@link Constant#SPLIT_REGEXP}
     * 
     * @param source
     *            source string
     * @return if source is null this will returns null
     */
    public static String[] split(String source) {
        if (source == null) { // fix
            return null;
        }
        return source.split(Constant.SPLIT_REGEXP);
    }

    public static String decodeUrl(String s) {

        final int numChars = s.length();

        final StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
        final Charset charset = Constant.DEFAULT_CHARSET;

        int i = 0;
        char c;
        boolean needToChange = false;
        byte[] bytes = null;
        while (i < numChars) {
            switch (c = s.charAt(i)) {
                case '+' : {
                    sb.append(' ');
                    i++;
                    needToChange = true;
                    break;
                }
                case '%' : {
                    try {
                        // (numChars-i)/3 is an upper bound for the number
                        // of remaining bytes
                        if (bytes == null) bytes = new byte[(numChars - i) / 3];
                        int pos = 0;
                        while (((i + 2) < numChars) && (c == '%')) {
                            int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                            if (v < 0) throw new IllegalArgumentException("Illegal hex characters in escape (%) pattern - negative value");
                            bytes[pos++] = (byte) v;
                            i += 3;
                            if (i < numChars) c = s.charAt(i);
                        }
                        // A trailing, incomplete byte encoding such as
                        // "%x" will cause an exception to be thrown
                        if ((i < numChars) && (c == '%')) throw new IllegalArgumentException("Incomplete trailing escape (%) pattern");
                        sb.append(new String(bytes, 0, pos, charset));
                    }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Illegal hex characters in escape (%) pattern - " + e.getMessage());
                    }
                    needToChange = true;
                    break;
                }
                default: {
                    sb.append(c);
                    i++;
                    break;
                }
            }
        }
        return (needToChange ? sb.toString() : s);
    }

    public static String encodeUrl(String s) {

        boolean needToChange = false;
        final int length = s.length();
        final StringBuilder out = new StringBuilder(length);
        final CharArrayWriter charArrayWriter = new CharArrayWriter();

        final BitSet dontNeedEncoding = StringUtils.dontNeedEncoding;
        final int caseDiff = StringUtils.caseDiff;
        final Charset charset = Constant.DEFAULT_CHARSET;

        for (int i = 0; i < length;) {
            int c = s.charAt(i);
            // System.out.println("Examining character: " + c);
            if (dontNeedEncoding.get(c)) {
                if (c == ' ') {
                    c = '+';
                    needToChange = true;
                }
                // System.out.println("Storing: " + c);
                out.append((char) c);
                i++;
                continue;
            }
            // convert to external encoding before hex conversion
            do {
                charArrayWriter.write(c);
                /* If this character represents the start of a Unicode surrogate pair, then pass
                 * in two characters. It's not clear what should be done if a bytes reserved in
                 * the surrogate pairs range occurs outside of a legal surrogate pair. For now,
                 * just treat it as if it were any other character. */
                if (c >= 0xD800 && c <= 0xDBFF && (i + 1) < length) {
                    // System.out.println(Integer.toHexString(c) + " is high surrogate");
                    int d = (int) s.charAt(i + 1);
                    // System.out.println("\tExamining " + Integer.toHexString(d));
                    if (d >= 0xDC00 && d <= 0xDFFF) {
                        // System.out.println("\t" + Integer.toHexString(d) + " is low surrogate");
                        charArrayWriter.write(d);
                        i++;
                    }
                }
                i++;
            } while (i < length && !dontNeedEncoding.get((c = (int) s.charAt(i))));

            charArrayWriter.flush();
            byte[] ba = new String(charArrayWriter.toCharArray()).getBytes(charset);
            for (int j = 0; j < ba.length; j++) {
                out.append('%');
                char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                // converting to use uppercase letter as part of
                // the hex value if ch is a letter.
                if (Character.isLetter(ch)) {
                    ch -= caseDiff;
                }
                out.append(ch);
                ch = Character.forDigit(ba[j] & 0xF, 16);
                if (Character.isLetter(ch)) {
                    ch -= caseDiff;
                }
                out.append(ch);
            }
            charArrayWriter.reset();
            needToChange = true;
        }
        return (needToChange ? out.toString() : s);
    }

    /**
     * Use StringTokenizer to split string to string array
     * 
     * @param str
     *            Input string
     * @param delimiter
     *            Input delimiter
     * @return Returns the splitted string array
     */
    public static String[] tokenizeToStringArray(final String str, final String delimiter) {

        if (str == null) {
            return Constant.EMPTY_STRING_ARRAY;
        }

        StringTokenizer st = new StringTokenizer(str, delimiter);
        List<String> tokens = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            tokens.add(token);
        }
        return toStringArray(tokens);
    }

    /**
     * {@link Collection} to string array
     * 
     * @param collection
     *            All element must be a string
     * @return String array
     */
    public static String[] toStringArray(Collection<String> collection) {
        return collection.toArray(Constant.EMPTY_STRING_ARRAY);
    }

    /**
     * Use default delimiter:',' append array to a string
     * 
     * @param array
     *            Input array object
     */
    public static String arrayToString(Object[] array) {
        return arrayToString(array, ",");
    }

    /**
     * Array to string
     * 
     * @param array
     *            Input array object
     * @param delimiter
     *            Delimiter string
     */
    public static String arrayToString(final Object[] array, final String delimiter) {
        if (array == null) {
            return null;
        }
        final int length = array.length;
        if (length == 1) {
            return array[0].toString();
        }

        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(array[i]);
            if (i != length - 1) {
                builder.append(delimiter);
            }
        }
        return builder.toString();
    }

    /**
     * Check properties file name
     * 
     * @param fileName
     *            Input file name
     * @return checked properties file name
     */
    public static String checkPropertiesName(final String fileName) {
        return fileName.endsWith(Constant.PROPERTIES_SUFFIX) ? fileName : fileName + Constant.PROPERTIES_SUFFIX;
    }

    /**
     * Use {@link UUID} to get random uuid string
     * 
     * @return Random uuid string
     */
    public static String getUUIDString() {
        return UUID.randomUUID().toString();
    }

    /**
     * Read the {@link InputStream} to text string
     * 
     * @param inputStream
     *            Input stream
     * @return String
     * @throws IOException
     *             If can't read the string
     */
    public static String readAsText(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        buffer = null;
        return result.toString();
    }

    /**
     * Replace all "\\" to "/"
     * 
     * @param path
     *            Input path string
     * @return Replace path
     */
    public static String cleanPath(final String path) {
        if (isEmpty(path)) {
            return path;
        }
        return path.replace(Constant.WINDOWS_PATH_SEPARATOR, Constant.PATH_SEPARATOR);
    }

    /**
     * Check Url, format url like :
     * 
     * <pre>
     * users    -> /users
     * /users   -> /users
     * </pre>
     * 
     * @param url
     *            Input url
     * @return
     */
    public static String checkUrl(String url) {
        return StringUtils.isEmpty(url) ? Constant.BLANK : (url.charAt(0) == '/' ? url : '/' + url);
    }

    /**
     * Append line to {@link StringBuilder}
     * 
     * @param reader
     *            String line read from {@link BufferedReader}
     * @param builder
     *            The {@link StringBuilder} append to
     * @throws IOException
     *             If an I/O error occurs
     */
    public static void appendLine(final BufferedReader reader, final StringBuilder builder) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
    }
}
