package corbit.commons.word;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArcLabel
{
	private static final Map<String,ArcLabel> labels = new ConcurrentHashMap<String,ArcLabel>();
	private final String label;

	public static ArcLabel getLabel(String sLabel)
	{
		if (!labels.containsKey(sLabel))
			labels.put(sLabel, new ArcLabel(sLabel));
		return labels.get(sLabel);
	}

	private ArcLabel(String sLabel)
	{
		label = sLabel;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ArcLabel))
			return false;
		ArcLabel l = (ArcLabel)o;
		return label.equals(l.label);
	}

	@Override
	public int hashCode()
	{
		return label.hashCode();
	}

	@Override
	public String toString()
	{
		return label;
	}
}
