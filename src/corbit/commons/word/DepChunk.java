package corbit.commons.word;

public class DepChunk extends TaggedChunk
{
	public int headBegin;
	public int headEnd;
	public ArcLabel arcLabel;

	public DepChunk(UnsegmentedSentence sent, int begin, int end, String tag, int headBegin, int headEnd, ArcLabel arcLabel)
	{
		super(sent, begin, end, tag);
		this.headBegin = headBegin;
		this.headEnd = headEnd;
		this.arcLabel = arcLabel;
	}

	public DepChunk(DepChunk dw)
	{
		this(dw.sent, dw.begin, dw.end, dw.tag, dw.headBegin, dw.headEnd, dw.arcLabel);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof DepChunk))
			return false;
		DepChunk dw = (DepChunk)obj;
		return (super.equals(dw) && headBegin == dw.headBegin && headEnd == dw.headEnd && arcLabel == dw.arcLabel);
	}

	@Override
	public int hashCode()
	{
		int hash = super.hashCode();
		int k = 31;
		hash = hash * k + headBegin;
		hash = hash * k + headEnd;
		hash = hash * k + arcLabel.hashCode();
		return hash;
	}

	@Override
	public String toString()
	{
		return super.toString() + ":" + headBegin + ":" + headEnd;
	}
}
