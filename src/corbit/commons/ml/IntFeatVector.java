package corbit.commons.ml;

import corbit.commons.Vocab;
import corbit.commons.io.Console;
import bak.pcj.IntIterator;
import bak.pcj.map.IntKeyDoubleChainedHashMap;

public class IntFeatVector extends IntKeyDoubleChainedHashMap
{
	private static final long serialVersionUID = 1L;

	public IntFeatVector(IntFeatVector v) {
		super(v);
	}

	public IntFeatVector() {
		super();
	}

	public void append(IntFeatVector v)
	{
		IntIterator it = v.keySet().iterator();
		while (it.hasNext())
		{
			int i = it.next();
			put(i, get(i) + v.get(i));
		}
	}

	public void subtract(IntFeatVector v)
	{
		IntIterator it = v.keySet().iterator();
		while (it.hasNext()) {
			int i = it.next();
			put(i, get(i) - v.get(i));
		}
	}

	public static IntFeatVector append(IntFeatVector v1, IntFeatVector v2)
	{
		IntFeatVector v = new IntFeatVector(v1);
		v.append(v2);
		return v;
	}

	public static IntFeatVector subtract(IntFeatVector v1, IntFeatVector v2)
	{
		IntFeatVector v = new IntFeatVector(v1);
		v.subtract(v2);
		return v;
	}

	public static IntFeatVector multiply(IntFeatVector v, double d)
	{
		IntFeatVector r = new IntFeatVector(v);
		IntIterator it = v.keySet().iterator();
		while (it.hasNext()) {
			int i = it.next();
			r.put(i, r.get(i) * d);
		}
		return r;
	}

	public static IntFeatVector divide(IntFeatVector v, double d)
	{
		IntFeatVector r = new IntFeatVector(v);
		IntIterator it = v.keySet().iterator();
		while (it.hasNext()) {
			int i = it.next();
			r.put(i, r.get(i) / d);
		}
		return r;
	}

	public void multiplyBy(double d)
	{
		IntIterator it = keySet().iterator();
		while (it.hasNext()) {
			int i = it.next();
			put(i, get(i) * d);
		}
	}

	public void divideBy(double d)
	{
		IntIterator it = keySet().iterator();
		while (it.hasNext()) {
			int i = it.next();
			put(i, get(i) / d);
		}
	}

	public void print(Vocab voc)
	{
		IntIterator it = keySet().iterator();
		while (it.hasNext()) {
			int i = it.next();
			double d = this.get(i);
			if (d != 0.0d)
				Console.writeLine(voc.get(i) + "\t" + d);
		}
		Console.writeLine();
	}
}
