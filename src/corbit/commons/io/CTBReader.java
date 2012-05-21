package corbit.commons.io;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import corbit.commons.dict.TagDictionary;
import corbit.commons.util.Statics;
import corbit.commons.word.ArcLabel;
import corbit.commons.word.ParsedSentence;
import corbit.commons.word.SentenceBuilder;

public class CTBReader extends ParseReader
{
	public CTBReader(String sFile, TagDictionary dict)
	{
		super(sFile, dict.generateTagSet(), new TreeSet<String>(Arrays.asList(dict.getArcLabels())));
	}

	public ParsedSentence readFromString(String l)
	{
		String[] ss = l.split(" ");
		SentenceBuilder sb = new SentenceBuilder(m_posSet, m_labelSet);

		for (int i = 0; i < ss.length; ++i)
		{
			Pattern re = Pattern.compile("(.*?):\\((.*?)\\)_\\((.*?)\\)_\\((.*?)\\)(?:_\\((.*?)\\))?");
			Matcher mc = re.matcher(ss[i]);

			if (!mc.matches() || mc.groupCount() < 4)
				return null;

			int iIndex = Integer.parseInt(mc.group(1));
			int iHead = Integer.parseInt(mc.group(2));
			String sForm = Normalizer.normalize(mc.group(3), Normalizer.Form.NFKC);
			String sPos = mc.group(4);
			String sLabel = mc.groupCount() == 5 ? mc.group(5) : null;
			ArcLabel label = sLabel != null ? ArcLabel.getLabel(sLabel) : null;
			sb.addWord(iIndex, sForm, sPos, iHead, label);
		}

		return sb.compile();
	}

	@Override
	protected void iterate() throws InterruptedException
	{
		int iSentence = 0;
		FileEnum fe = new FileEnum(m_sFile);

		try
		{
			for (String l: fe)
			{
				l = Statics.trimSpecial(l);
				if (l.length() == 0)
					continue;
				ParsedSentence sent = readFromString(l);
				++iSentence;

				if (sent == null)
				{
					System.err.println(String.format("Error found at line %d. Skipping.", iSentence));
					yieldBreak();
				}
				else
					yieldReturn(sent);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			fe.shutdown();
		}
	}

}
