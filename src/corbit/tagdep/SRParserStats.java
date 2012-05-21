package corbit.tagdep;

public class SRParserStats
{
	private static SRParserStats m_inst;

	static
	{
		m_inst = new SRParserStats();
	}

	private SRParserStats()
	{}

	public static SRParserStats getInstance()
	{
		return m_inst;
	}
}
