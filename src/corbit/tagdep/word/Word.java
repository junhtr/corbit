package corbit.tagdep.word;

public class Word
{
	public String form;
	public String pos;

	public Word(String form, String pos)
	{
		this.form = form;
		this.pos = pos;
	}

	@Override
	public String toString()
	{
		return form + "/" + (pos != null ? pos : "--");
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof Word)) return false;
		Word w = (Word)obj;
		if (!w.form.equals(form) || !w.pos.equals(pos)) return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		return 19 + toString().hashCode();
	}
}
