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

import cn.taketoday.context.Constant;

import java.io.CharArrayWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * 
 * @author Today <br>
 *         2018-06-26 21:19:09
 */
public abstract class StringUtils {

	private static final int caseDiff = ('a' - 'A');
	private static BitSet dontNeedEncoding;

	static {

		/*
		 * The list of characters that are not encoded has been determined as follows:
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
		 * RFC in this matter, as is Netscape.
		 *
		 */
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

	/**
	 * 
	 * @param str
	 * @return
	 */
	public final static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public final static boolean isNotEmpty(String str) {
		return str != null && str.length() != 0;
	}

	/**
	 * 
	 * @param strs
	 *            array of string
	 * @return
	 */
	public final static boolean isArrayNotEmpty(String... strs) {
		return strs != null && strs.length != 0;
	}

	/**
	 * 
	 * @param strs
	 *            array of string
	 * @return
	 */
	public final static boolean isArrayEmpty(String... strs) {
		return strs == null || strs.length == 0;
	}

	/**
	 * @param source
	 * @return
	 */
	public static String[] split(String source) {
		if (source == null) {
			return null;
		}
		return source.split(Constant.SPLIT_REGEXP);
	}

	/**
	 * Decode url
	 * 
	 * @param s
	 *            url
	 * @return
	 */
	public static String decodeUrl(String s) {

		boolean needToChange = false;
		int numChars = s.length();
		StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2 : numChars);
		int i = 0;
		char c;
		byte[] bytes = null;
		while (i < numChars) {
			switch (c = s.charAt(i))
			{
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
						if (bytes == null)
							bytes = new byte[(numChars - i) / 3];
						int pos = 0;
						while (((i + 2) < numChars) && (c == '%')) {
							int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
							if (v < 0)
								throw new IllegalArgumentException("Illegal hex characters in escape (%) pattern - negative value");
							bytes[pos++] = (byte) v;
							i += 3;
							if (i < numChars)
								c = s.charAt(i);
						}
						// A trailing, incomplete byte encoding such as
						// "%x" will cause an exception to be thrown
						if ((i < numChars) && (c == '%'))
							throw new IllegalArgumentException("Incomplete trailing escape (%) pattern");
						sb.append(new String(bytes, 0, pos, Constant.DEFAULT_CHARSET));
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

	/**
	 * 
	 * @param s
	 * @return
	 */
	public static String encodeUrl(String s) {

		boolean needToChange = false;
		final int length = s.length();
		StringBuffer out = new StringBuffer(length);
		CharArrayWriter charArrayWriter = new CharArrayWriter();

		for (int i = 0; i < length;) {
			int c = (int) s.charAt(i);
//			 System.out.println("Examining character: " + c);
			if (dontNeedEncoding.get(c)) {
				if (c == ' ') {
					c = '+';
					needToChange = true;
				}
//				 System.out.println("Storing: " + c);
				out.append((char) c);
				i++;
				continue;
			}
			// convert to external encoding before hex conversion
			do {
				charArrayWriter.write(c);
				/*
				 * If this character represents the start of a Unicode surrogate pair, then pass
				 * in two characters. It's not clear what should be done if a bytes reserved in
				 * the surrogate pairs range occurs outside of a legal surrogate pair. For now,
				 * just treat it as if it were any other character.
				 */
				if (c >= 0xD800 && c <= 0xDBFF) {
//					System.out.println(Integer.toHexString(c) + " is high surrogate");
					if ((i + 1) < length) {
						int d = (int) s.charAt(i + 1);
//						System.out.println("\tExamining " + Integer.toHexString(d));
						if (d >= 0xDC00 && d <= 0xDFFF) {
//							System.out.println("\t" + Integer.toHexString(d) + " is low surrogate");
							charArrayWriter.write(d);
							i++;
						}
					}
				}
				i++;
			} while (i < length && !dontNeedEncoding.get((c = (int) s.charAt(i))));

			charArrayWriter.flush();
			String str = new String(charArrayWriter.toCharArray());
			byte[] ba = str.getBytes(Constant.DEFAULT_CHARSET);
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

	public static String[] tokenizeToStringArray(String str, String delimiters) {
		return tokenizeToStringArray(str, delimiters, true, true);
	}

	/**
	 * Tokenize the given {@code String} into a {@code String} array via a
	 * {@link StringTokenizer}.
	 * <p>
	 * The given {@code delimiters} string can consist of any number of delimiter
	 * characters. Each of those characters can be used to separate tokens. A
	 * delimiter is always a single character; for multi-character delimiters,
	 * consider using {@link #delimitedListToStringArray}.
	 * 
	 * @param str
	 *            the {@code String} to tokenize
	 * @param delimiters
	 *            the delimiter characters, assembled as a {@code String} (each of
	 *            the characters is individually considered as a delimiter)
	 * @param trimTokens
	 *            trim the tokens via {@link String#trim()}
	 * @param ignoreEmptyTokens
	 *            omit empty tokens from the result array (only applies to tokens
	 *            that are empty after trimming; StringTokenizer will not consider
	 *            subsequent delimiters as token in the first place).
	 * @return an array of the tokens
	 * @see java.util.StringTokenizer
	 * @see String#trim()
	 * @see #delimitedListToStringArray
	 */
	public static String[] tokenizeToStringArray(
			String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

		if (str == null) {
			return new String[0];
		}

		StringTokenizer st = new StringTokenizer(str, delimiters);
		List<String> tokens = new ArrayList<>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (trimTokens) {
				token = token.trim();
			}
			if (!ignoreEmptyTokens || token.length() > 0) {
				tokens.add(token);
			}
		}
		return toStringArray(tokens);
	}

	public static String[] toStringArray(Collection<String> collection) {
		return collection.toArray(new String[collection.size()]);
	}

	/////////////////////////
	/**
	 * Convert a {@code Collection} into a delimited {@code String} (e.g. CSV).
	 * <p>
	 * Useful for {@code toString()} implementations.
	 * 
	 * @param coll
	 *            the {@code Collection} to convert
	 * @param delim
	 *            the delimiter to use (typically a ",")
	 * @return the delimited {@code String}
	 */
	public static String collectionToDelimitedString(Collection<?> coll, String delim) {
		return collectionToDelimitedString(coll, delim, "", "");
	}

	/**
	 * Convert a {@code Collection} into a delimited {@code String} (e.g., CSV).
	 * <p>
	 * Useful for {@code toString()} implementations.
	 * 
	 * @param coll
	 *            the {@code Collection} to convert
	 * @return the delimited {@code String}
	 */
	public static String collectionToCommaDelimitedString(Collection<?> coll) {
		return collectionToDelimitedString(coll, ",");
	}

	public static String collectionToDelimitedString(
			Collection<?> coll, String delim, String prefix, String suffix) {

		if ((coll == null || coll.isEmpty())) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		Iterator<?> it = coll.iterator();
		while (it.hasNext()) {
			sb.append(prefix).append(it.next()).append(suffix);
			if (it.hasNext()) {
				sb.append(delim);
			}
		}
		return sb.toString();
	}

}
