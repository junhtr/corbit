package corbit.commons.word;

public class IndexWord
{
	public final ParsedSentence sent;
	public final int index;
	public int head;
	public final String form;
	public final String tag;
	public final ArcLabel arclabel;

	public IndexWord(ParsedSentence sent, int index, int head, String form, String tag, ArcLabel arclabel)
	{
		this.sent = sent;
		this.index = index;
		this.head = head;
		this.form = form;
		this.tag = tag;
		this.arclabel = arclabel;
	}

	public IndexWord(IndexWord w)
	{
		this.sent = w.sent;
		this.index = w.index;
		this.head = w.head;
		this.form = w.form;
		this.tag = w.tag;
		this.arclabel = w.arclabel;
	}

	public DepChunk toDepChunk()
	{
		int headBegin = head >= 0 ? sent.getBeginIndex(head) : head;
		int headEnd = head >= 0 ? sent.getEndIndex(head) : head;
		return new DepChunk(sent.sequence, sent.getBeginIndex(index), sent.getEndIndex(index), tag, headBegin, headEnd, sent.get(index).arclabel);
	}

	@Override
	public String toString()
	{
		return index + ":" + head + ":" + (arclabel != null ? arclabel + ":" : "") + form + ":" + tag;
	}
}
