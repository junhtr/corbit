package corbit.commons.word;

public class TaggedChunk extends Chunk
{
	public final String tag;
	public static final String rootTag = "(ROOT)";

	public TaggedChunk(UnsegmentedSentence sent, int begin, int end, String tag)
	{
		super(sent, begin, end);
		this.tag = tag;
	}

	@Override
	public String toString()
	{
		return super.toString() + ":" + (tag != null ? tag : "-");
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof TaggedChunk))
			return false;
		TaggedChunk w = (TaggedChunk)obj;
		return super.equals(w) && tag.equals(w.tag);
	}

	@Override
	public int hashCode()
	{
		int hash = super.hashCode();
		int k = 31;
		hash = hash * k + tag.hashCode();
		return hash;
	}
}
