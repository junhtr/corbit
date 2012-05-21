package corbit.tagdep.handler;

import java.util.Set;
import java.util.TreeSet;

import corbit.commons.util.Statics;

public abstract class AtomicFeatures
{
	public final TreeSet<String> fvdelay;

	protected final int numFeatures;
	protected String[] features;
	protected int hash;

	protected AtomicFeatures(int n, TreeSet<String> _fvdelay)
	{
		numFeatures = n;
		features = new String[n];
		hash = 0;
		fvdelay = _fvdelay;
	}

	public String get(int n)
	{
		return features[n];
	}

	@Override
	public int hashCode()
	{
		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		// check the equality of the type
		if (obj == null || !(obj instanceof AtomicFeatures))
			return false;
		AtomicFeatures atoms = (AtomicFeatures)obj;

		// return hash == atoms.hash;

		// check the equality of features
		for (int i = 0; i < features.length; ++i)
		{
			String s1 = features[i];
			String s2 = atoms.features[i];
			if ((s1 == null ^ s1 == null) || s1 != null && !s1.equals(s2)) return false;
		}
		// check the equality of delayed features
		Set<String> fvdelay2 = atoms.fvdelay;
		if (fvdelay != null)
		{
			if (fvdelay == null) return false;
			return (Statics.setEquals(fvdelay, fvdelay2));
		}
		else if (fvdelay2 != null) return false;
		return true;
	}

	protected void setHash()
	{
		hash = hash();
	}

	protected int hash()
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < features.length; ++i)
		{
			sb.append(features[i]);
			sb.append(' ');
		}
		if (fvdelay != null)
		{
			for (String s : fvdelay)
			{
				sb.append(s);
				sb.append(' ');
			}
		}
		return sb.toString().hashCode();
	}

}