package corbit.commons.word;

import java.util.List;

public class IndexTree extends IndexWord
{
	public final List<IndexWord> children;

	public IndexTree(IndexWord w, List<IndexWord> children)
	{
		super(w);
		this.children = children;
	}
}
