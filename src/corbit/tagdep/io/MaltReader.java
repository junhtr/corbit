package corbit.tagdep.io;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import corbit.commons.io.Console;
import corbit.commons.io.FileEnum;
import corbit.tagdep.dict.CTBTagDictionary;
import corbit.tagdep.word.DepTree;
import corbit.tagdep.word.DepTreeSentence;

public class MaltReader extends ParseReader
{
	String m_sFile;

	public MaltReader(String sFile) throws FileNotFoundException, UnsupportedEncodingException
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
				List<String> l = new ArrayList<String>();
				String sLine;
				while (!(sLine = fe.next()).equals(""))
					l.add(sLine);

				++iSentence;
				DepTreeSentence s = new DepTreeSentence();
				for (int i = 0; i < l.size(); ++i)
					s.add(new DepTree());
				for (int i = 0; i < l.size(); ++i)
				{
					String[] r = l.get(i).split("\t");
					int iIndex = i;
					int iHead = Integer.parseInt(r[2]) - 1;
					String sForm = Normalizer.normalize(r[0], Normalizer.Form.NFKC);
					String sPos = r[1];

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
					dw.index = iIndex;
					dw.form = sForm;
					dw.pos = sPos;
					dw.head = iHead;

					if (iHead < -1 || iHead >= s.size() || iHead == i)
					{
						Console.writeLine(String.format(
								"Error found at line %d. Skipping.", iSentence));
						s = null;
						break;
					}

					if (iHead != -1)
						s.get(iHead).children.add(dw);
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
