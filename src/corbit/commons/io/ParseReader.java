package corbit.commons.io;

import java.util.Set;

import corbit.commons.util.Generator;
import corbit.commons.word.ParsedSentence;

public abstract class ParseReader extends Generator<ParsedSentence>
{
	protected String m_sFile;
	protected final Set<String> m_posSet;
	protected final Set<String> m_labelSet;

	public enum Format { CTB, MALT }
	
	public ParseReader(String sFile, Set<String> setPos, Set<String> setLabel)
	{
		m_sFile = sFile;
		m_posSet = setPos;
		m_labelSet = setLabel;
	}
}
