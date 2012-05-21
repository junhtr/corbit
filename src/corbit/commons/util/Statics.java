package corbit.commons.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Statics
{
	private static final double EPS = 0.00000001;
	private static final long SEED = 13;

	public static <T> void shuffle(List<T> r)
	{
		Random rnd = new Random();
		rnd.setSeed(SEED);
		Collections.shuffle(r, rnd);
	}

	public static <T> void increment(Map<T,Integer> s, T key)
	{
		if (s.containsKey(key))
		{
			int i = s.get(key);
			s.put(key, i + 1);
		}
		else
			s.put(key, 1);
	}

	public static <T> void swap(T[] a, T[] b)
	{
		if (a.length != b.length)
			throw new IllegalArgumentException();
		for (int i = 0; i < a.length; ++i)
		{
			T c = a[i];
			a[i] = b[i];
			b[i] = c;
		}
	}

	public static <T> void swap(T[] a, T[] b, int idx)
	{
		if (a.length != b.length)
			throw new IllegalArgumentException();
		T c = a[idx];
		a[idx] = b[idx];
		b[idx] = c;
	}

	public static String[] mergeArray(String[] a, String[] b)
	{
		String[] c = new String[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}

	public static double harmonicMean(double d1, double d2)
	{
		return d1 > 0.0 && d2 > 0.0 ? 2.0 / (1.0 / d1 + 1.0 / d2) : 0.0;
	}

	public static String strReplace(String str, String s, String t)
	{
		int idx = str.indexOf(s);
		if (idx == -1)
			return str;
		else
			return str.substring(0, idx) + t + str.substring(idx + s.length());
	}

	public static boolean doubleEquals(double a, double b)
	{
		if (Math.abs(a - b) < EPS)
			return true;
		else
			return false;
	}

	public static String trimSpecial(String s)
	{
		while (true) {
			int iLen = s.length();
			if (iLen == 0)
				break;
			char c;
			if ((c = s.charAt(iLen - 1)) == ' ' || c == '\r' || c == '\n')
				s = s.substring(0, iLen - 1);
			else if ((c = s.charAt(0)) == ' ' || c == '\r' || c == '\n' || c == 0xfeff)
				s = s.substring(1, iLen);
			else
				break;
		}
		return s;
	}

	public static <T> T[] fillArray(T[] a, T v)
	{
		for (int i = 0; i < a.length; ++i)
			a[i] = v;
		return a;
	}

	public static <T> boolean arrayEquals(T[] a, T[] b)
	{
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; ++i)
			if (!a[i].equals(b[i]))
				return false;
		return true;
	}

	public static <K, V> boolean dictEquals(Map<K,V> d1, Map<K,V> d2)
	{
		if (d1.size() != d2.size())
			return false;
		for (K k: d1.keySet())
			if (!d2.containsKey(k) || !d2.get(k).equals(d1.get(k)))
				return false;
		return true;
	}

	public static <T> boolean setEquals(Set<T> k1, Set<T> k2)
	{
		if (k1.size() != k2.size())
			return false;
		for (T k: k1)
			if (!k2.contains(k))
				return false;
		return true;
	}

	public static boolean arrayEquals(double[] a, double[] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; ++i)
			if (a[i] != b[i])
				return false;
		return true;
	}

	public static boolean arrayEquals(int[] a, int[] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; ++i)
			if (a[i] != b[i])
				return false;
		return true;
	}

	public static String charMultiplyBy(char c, int n)
	{
		char[] cc = new char[n];
		Arrays.fill(cc, c);
		return new String(cc);
	}

	public static int multiByteLength(String s)
	{
		int n = 0;
		for (int i = 0; i < s.length(); ++i)
			if (s.charAt(i) > 0x7ff)
				n += 2;
			else
				n++;
		return n;
	}

	public static String strJoin(String[] a, String delim)
	{
		int sz = a.length;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < sz; ++i)
		{
			sb.append(a[i]);
			if (i < sz - 1)
				sb.append(delim);
		}
		return sb.toString();
	}

	public static String strJoin(List<String> a, String delim)
	{
		int sz = a.size();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < sz; ++i)
		{
			sb.append(a.get(i));
			if (i < sz - 1)
				sb.append(delim);
		}
		return sb.toString();
	}

	public static double max(double[] a)
	{
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < a.length; ++i)
			if (a[i] > max)
				max = a[i];
		return max;
	}

	public static <T> boolean arrayContains(T[] a, T v)
	{
		for (int i = 0; i < a.length; ++i)
			if (a[i].equals(v))
				return true;
		return false;
	}

	public static int getCharType(char c)
	{
		if (c >= '0' && c <= '9' || c >= '０' && c <= '９' ||
				c == '一' || c == '二' || c == '三' || c == '四' || c == '五' ||
				c == '六' || c == '七' || c == '八' || c == '九' || c == '十' ||
				c == '百' || c == '千' || c == '万' || c == '亿' ||
				c == '〇' || c == '零')
			return 1; // digit
		else if (c == '年' || c == '月' || c == '日' || c == '时' || c == '分' || c == '秒')
			return 2; // date/time
		else if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' ||
				c >= 'ａ' && c <= 'ｚ' || c >= 'Ａ' && c <= 'Ｚ')
			return 3; // alphabet
		else if (c >= 0x0020 && c < 0x0030 ||
				c >= 0x003a && c < 0x0041 ||
				c >= 0x005b && c < 0x0061 ||
				c >= 0x007b && c < 0x0080 ||
				c >= 0x2010 && c < 0x2070 ||
				c <= 0x3000 && c < 0x3040)
			return 4; // punctuation
		else
			return 5; // other characters
	}

	public static boolean isSpace(char c)
	{
		return (c == 0x3000 || c == ' ');
	}

}
