package corbit.commons.word;

public class Chunk
{
	public final UnsegmentedSentence sent;
	public final int begin;
	public final int end;
	public final String form;
	public static final String rootForm = "(ROOT)";

	public Chunk(UnsegmentedSentence sent, int begin, int end)
	{
		this.sent = sent;
		this.begin = begin;
		this.end = end;
		this.form = (begin == -1 && end == -1) ? rootForm : sent.substring(begin, end);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof Chunk))
			return false;
		Chunk c = (Chunk)obj;
		return (sent == c.sent && begin == c.begin && end == c.end);
	}

	@Override
	public int hashCode()
	{
		int hash = 17;
		int k = 31;
		hash = hash * k + sent.hashCode();
		hash = hash * k + begin;
		hash = hash * k + end;
		return hash;
	}

	@Override
	public String toString()
	{
		return begin + ":" + end + ":" + form;
	}
}
