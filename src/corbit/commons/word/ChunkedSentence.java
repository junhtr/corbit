package corbit.commons.word;

import java.util.ArrayList;

public class ChunkedSentence extends ArrayList<DepChunk>
{
	private static final long serialVersionUID = 1L;
	UnsegmentedSentence sent;

	public ChunkedSentence(UnsegmentedSentence sent)
	{
		super();
		this.sent = sent;
	}
}
