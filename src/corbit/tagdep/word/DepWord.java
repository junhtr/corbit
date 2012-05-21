package corbit.tagdep.word;

public class DepWord extends Word
{
	public int index;
	public int head;
	public DepTreeSentence sent;

	public DepWord(DepTreeSentence sent, int index, String form, String pos, int head)
	{
		super(form, pos);
		this.sent = sent;
		this.index = index;
		this.head = head;
	}

	public DepWord(DepWord dw)
	{
		this(dw.sent, dw.index, dw.form, dw.pos, dw.head);
	}

	public DepWord()
	{
		this(null, -1, null, null, -1);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof DepWord)) return false;
		DepWord dw = (DepWord)obj;
		if (index != dw.index ||
				head != dw.head ||
				sent != dw.sent ||
				!super.equals((Word)dw))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		return (index << head | index >> (32 - head)) ^ sent.hashCode() ^ super.hashCode();
	}

	@Override
	public String toString()
	{
		return index + ":" + super.toString() + "/" + (head != -2 ? head : "");
	}
}
