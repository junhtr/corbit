package corbit.commons.word;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParsedSentence extends ArrayList<IndexWord>
{
	private static final long serialVersionUID = 1L;
	UnsegmentedSentence sequence = null;
	List<List<IndexWord>> childs = null;

	/** denote the ending character index of the i-th word */
	int[] bounds = null;

	ParsedSentence()
	{}

	public IndexTree getWordWithChild(int idx)
	{
		return new IndexTree(get(idx), childs.get(idx));
	}

	public IndexWord findWord(int begin, int end)
	{
		int idx = getWordIndex(begin, end);
		return idx >= 0 ? get(idx) : null;
	}

	public IndexWord findWord(int begin)
	{
		if (begin == 0)
			return get(0);

		int idx = Arrays.binarySearch(bounds, begin);
		if (idx >= 0)
			return get(idx + 1);
		else
			return null;
	}

	public IndexWord findWordEndingAt(int end)
	{
		int idx = Arrays.binarySearch(bounds, end);
		if (idx >= 0)
			return get(idx);
		else
			return null;
	}

	public int getNumChild(int idx)
	{
		return childs.get(idx).size();
	}

	public int getBeginIndex(int idx)
	{
		return idx < 0 ? idx : idx > 0 ? bounds[idx - 1] : 0;
	}

	public int getEndIndex(int idx)
	{
		return idx < 0 ? idx : bounds[idx];
	}

	public IndexWord getWord(int begin, int end)
	{
		int idx = getWordIndex(begin, end);
		if (idx >= 0)
			return get(idx);
		else
			return null;
	}

	public int getWordIndex(int begin, int end)
	{
		if (bounds == null)
			throw new RuntimeException("Create index first.");

		if (begin < 0 && begin == end)
			return begin;

		int idx = Arrays.binarySearch(bounds, end);
		if (idx < 0)
			return -2;
		int _begin = idx == 0 ? 0 : bounds[idx - 1];
		if (idx >= 0 && _begin == begin)
			return idx;
		else
			return -2;
	}

	void createIndex()
	{
		final int size = size();
		bounds = new int[size];
		childs = new ArrayList<List<IndexWord>>(size);
		int curidx = 0;
		for (int i = 0; i < size; ++i)
		{
			curidx += get(i).form.length();
			bounds[i] = curidx;
			childs.add(new ArrayList<IndexWord>());
		}
		for (int i = 0; i < size; ++i)
		{
			IndexWord dw = get(i);
			if (dw.head >= 0)
				childs.get(dw.head).add(dw);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size(); ++i)
			if (i < size() - 1)
				sb.append(get(i).toString() + ' ');
			else
				sb.append(get(i).toString());
		return sb.toString();
	}

	public UnsegmentedSentence toUnsegmentedSentence(List<String> tags, List<Integer> indices, List<Boolean> bounds)
	{
		StringBuilder builder = new StringBuilder();

		int curidx = 0;
		for (int i = 0; i < size(); ++i)
		{
			IndexWord dw = get(i);
			builder.append(dw.form);
			if (bounds != null)
			{
				for (int j = 0; j < dw.form.length() - 1; ++j)
					bounds.add(false);
				bounds.add(true);
			}
			if (indices != null)
			{
				curidx += dw.form.length();
				indices.add(curidx);
			}
			if (tags != null)
				tags.add(dw.tag);
		}

		return new UnsegmentedSentence(builder.toString().toCharArray());
	}
}
