package corbit.commons.util;

public class Pair<K, V>
{
	public final K first;
	public final V second;

	public Pair(K key, V val)
	{
		this.first = key;
		this.second = val;
	}

	@Override
	public String toString()
	{
		return "<" + first.toString() + ", " + second.toString() + ">";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof Pair))
			return false;
		@SuppressWarnings("rawtypes") Pair p = (Pair)obj;
		return first.equals(p.first) && second.equals(p.second);
	}

	@Override
	public int hashCode()
	{
		return first.hashCode() * 17 + second.hashCode() + 1;
	}
}
