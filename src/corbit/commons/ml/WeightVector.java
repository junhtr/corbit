package corbit.commons.ml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import bak.pcj.IntIterator;
import corbit.commons.util.Statics;

public class WeightVector
{
	protected static final int DEFAULT_CAPACITY = 1 << 24;
	protected double[] vector;
	protected int capacity;

	public WeightVector()
	{
		vector = new double[DEFAULT_CAPACITY];
		capacity = DEFAULT_CAPACITY;
	}

	public WeightVector(WeightVector v)
	{
		vector = Arrays.copyOf(v.vector, v.vector.length);
		capacity = v.capacity;
	}

	public void clear()
	{
		Arrays.fill(vector, 0.0d);
	}

	public double getWithCheck(int i)
	{
		return (i < capacity) ? vector[i] : 0.0;
	}

	public double get(int i)
	{
		return vector[i];
	}

	public void put(int i, double d)
	{
		ensureCapacity(i);
		vector[i] = d;
	}

	private void ensureCapacity(int i)
	{
		if (i >= capacity)
		{
			int newCapacity = capacity << 1;
			while (newCapacity <= i)
				newCapacity = newCapacity << 1;
			resize(newCapacity);
		}
	}

	private void resize(int i)
	{
//		System.err.println("WeightVector resized to " + i);
		vector = Arrays.copyOf(vector, i);
		capacity = i;
	}

	public void append(IntFeatVector v)
	{
		IntIterator it = v.keySet().iterator();
		while (it.hasNext())
		{
			int i = it.next();
			ensureCapacity(i);
			vector[i] = vector[i] + v.get(i);
		}
	}

	public void subtract(IntFeatVector v)
	{
		IntIterator it = v.keySet().iterator();
		while (it.hasNext())
		{
			int i = it.next();
			ensureCapacity(i);
			vector[i] = vector[i] - v.get(i);
		}
	}

	public double score(IntFeatVector v)
	{
		double dScore = 0;
		IntIterator it = v.keySet().iterator();
		while (it.hasNext())
		{
			int i = it.next();
			if (i < capacity)
				dScore += get(i) * v.get(i);
		}
		return dScore;
	}

	public void save(PrintWriter sw)
	{
//		int iNumElem = 0;
		int iMaxIndex = 0;
		for (int i = 0; i < capacity; ++i)
		{
			double d = get(i);
			if (d != 0.0d)
			{
				sw.println(i + "\t" + d);
				if (i > iMaxIndex)
					iMaxIndex = i;
//				++iNumElem;
			}
		}
		sw.println("END_OF_WEIGHT");
//		System.err.println(String.format("Number of non-zero feature weights: %d/%d.", iNumElem, iMaxIndex));
	}

	public void load(BufferedReader sr) throws IOException
	{
		int iNumElem = 0;
		int iMaxIndex = -1;
		while (true)
		{
			String s = Statics.trimSpecial(sr.readLine());
			if (s.equals("END_OF_WEIGHT"))
				break;
			String[] ss = s.split("\t");
			int idx = Integer.parseInt(ss[0]);
			this.put(idx, Double.parseDouble(ss[1]));
			if (idx > iMaxIndex)
				iMaxIndex = idx;
			++iNumElem;
		}
		System.err.println(String.format("%d/%d weights loaded.", iNumElem, iMaxIndex));
	}
}
