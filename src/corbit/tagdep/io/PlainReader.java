package corbit.tagdep.io;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import corbit.commons.io.Console;
import corbit.commons.io.FileEnum;
import corbit.tagdep.dict.CTBTagDictionary;
import corbit.tagdep.word.DepTree;
import corbit.tagdep.word.DepTreeSentence;

class PlainReader extends ParseReader
{
	String m_sFile;

	public PlainReader(String sFile) throws FileNotFoundException, UnsupportedEncodingException
	{
		m_sFile = sFile;
	}

	@Override
	protected void iterate() throws InterruptedException
	{
		int iSentence = 0;
		FileEnum fe = new FileEnum(m_sFile);
		Set<String> posSet = CTBTagDictionary.copyTagSet();
		
		try
		{
			while (fe.hasNext())
			{
				++iSentence;
				String l = fe.next();
				l.replaceAll("\r\n", "");
				l.replaceAll("\n", "");
				DepTreeSentence s = new DepTreeSentence();
				String[] ss = l.split(" ");

				for (int i = 0; i < ss.length; ++i)
					s.add(new DepTree());

				for (int i = 0; i < ss.length; ++i)
				{
					Pattern re = Pattern.compile("(.+)#(.+)");
					Matcher mc = re.matcher(ss[i]);

					if (!mc.matches() || mc.groupCount() < 2)
					{
						Console.writeLine(String.format("Error found at line %d. Skipping.", iSentence));
						s = null;
						break;
					}

					String sForm = Normalizer.normalize(mc.group(1), Normalizer.Form.NFKC);
					String sPos = mc.group(2);

					if (!sPos.startsWith("-") && sPos.contains("-"))
						sPos = sPos.split("-")[0];
					else if (sPos.equals("-NONE-"))
						sPos = "NONE";
					else if (sPos.equals("PU/"))
						sPos = "PU";

					if (!posSet.contains(sPos))
						Console.writeLine("Unknown POS: " + sPos);

					DepTree dw = s.get(i);

					dw.sent = s;
					dw.index = i;
					dw.form = sForm;
					dw.pos = sPos;
					dw.head = -1;
				}
				yieldReturn(s);
			}
		}
		finally
		{
			fe.shutdown();
		}
	}

}
