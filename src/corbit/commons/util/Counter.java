package corbit.commons.util;

import java.util.LinkedHashMap;

public class Counter<T> extends LinkedHashMap<T,Integer>
{
	private static final long serialVersionUID = 1L;

	public void increment(T val)
	{
		if (containsKey(val))
			super.put(val, super.get(val) + 1);
		else
			super.put(val, 1);
	}

	public int getCount(T val)
	{
		if (super.containsKey(val))
			return super.get(val);
		else
			return 0;
	}
}
