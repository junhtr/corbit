package corbit.commons.word;

import java.util.Arrays;

import corbit.commons.util.Statics;

public class UnsegmentedSentence
{
	int hash = 0;
	final char[] chars;

	public UnsegmentedSentence(char[] _chars)
	{
		chars = _chars;
	}

	public int length()
	{
		return chars.length;
	}

	public String substring(int begin, int end)
	{
		return new String(chars, begin, end - begin);
	}

	public String substringWithPadding(int begin, int end)
	{
		String s = "";
		if (begin < 0)
			s += Statics.charMultiplyBy('$', -begin);
		begin = Math.max(0, begin);
		s += new String(chars, begin, Math.min(chars.length, end) - begin);
		if (end > chars.length)
			s += Statics.charMultiplyBy('$', end - chars.length);
		return s;
	}

	public char charAt(int index)
	{
		return chars[index];
	}

	public char what(int index)
	{
		return charAtIgnoreRange(index);
	}

	public char charAtIgnoreRange(int index)
	{
		if (index >= 0 && index < chars.length)
			return chars[index];
		else
			return '$';
	}

	public int charTypeAt(int index)
	{
		if (index >= 0 && index < chars.length)
			return Statics.getCharType(chars[index]);
		else
			return 0;
	}

	@Override
	public String toString()
	{
		return new String(chars);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof UnsegmentedSentence))
			return false;
		else
			return Arrays.equals(chars, ((UnsegmentedSentence)obj).chars);
	}

	@Override
	public int hashCode()
	{
		if (hash != 0)
			return hash;
		else
			return (hash = Arrays.hashCode(chars));
	}
}
