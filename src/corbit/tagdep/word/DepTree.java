package corbit.tagdep.word;

import java.util.ArrayList;
import java.util.List;

public class DepTree extends DepWord
{

	public final List<DepTree> children;

	public DepTree(DepTreeSentence sent, int index, String form, String pos,
			int head)
	{
		super(sent, index, form, pos, head);
		children = new ArrayList<DepTree>();
	}

	public DepTree(DepTree t)
	{
		super(t);
		children = new ArrayList<DepTree>(t.children);
	}

	public DepTree(DepWord w)
	{
		super(w);
		children = new ArrayList<DepTree>();
	}

	public DepTree()
	{
		super();
		children = new ArrayList<DepTree>();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof DepTree))
			return false;
		DepTree dt = (DepTree)obj;
		if (!super.equals((DepWord)dt) || children.size() != dt.children.size())
			return false;
		for (int i = 0; i < children.size(); ++i)
			if (!children.get(i).equals(dt.children.get(i)))
				return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		int h = super.hashCode();
		for (int i = 0; i < children.size(); ++i)
			h = (h << 1 | h >> 31) ^ children.get(i).hashCode();
		return h;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append(' ');
		if (children.size() > 0)
		{
			sb.append("{ ");
			for (int i = 0; i < children.size(); ++i)
			{
				sb.append(children.get(i).toString());
				if (i < children.size() - 1)
					sb.append(", ");
			}
			sb.append("} ");
		}
		return sb.toString();
	}
}
