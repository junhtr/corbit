package corbit.tagdep.io;

import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import corbit.commons.io.Console;
import corbit.commons.io.FileEnum;
import corbit.commons.util.Statics;
import corbit.tagdep.dict.CTBTagDictionary;
import corbit.tagdep.word.DepTree;
import corbit.tagdep.word.DepTreeSentence;

public class CTBReader extends ParseReader
{
	private String m_sFile;

	public CTBReader(String sFile)
	{
		this.m_sFile = sFile;
	}

	@Override
	protected void iterate() throws InterruptedException
	{
		int iSentence = 0;
		FileEnum fe = new FileEnum(m_sFile);
		Set<String> posSet = CTBTagDictionary.copyTagSet();

		try
		{
			for (String l : fe)
			{
				l = Statics.trimSpecial(l);
				if (l.length() == 0) continue;
				++iSentence;
				DepTreeSentence s = new DepTreeSentence();
				String[] ss = l.split(" ");
				for (int i = 0; i < ss.length; ++i)
					s.add(new DepTree());

				for (int i = 0; i < ss.length; ++i)
				{
					Pattern re = Pattern.compile("(.*?):\\((.*?)\\)_\\((.*?)\\)_\\((.*?)\\)");
					Matcher mc = re.matcher(ss[i]);

					if (!mc.matches() || mc.groupCount() < 4)
					{
						Console.writeLine(String.format("Format error at line %d. Skipping.", iSentence));
						s = null;
						break;
					}

					String sForm = Normalizer.normalize(mc.group(3), Normalizer.Form.NFKC);
					String sPos = mc.group(4);

					if (!sPos.startsWith("-") && sPos.contains("-"))
						sPos = sPos.split("-")[0];
					if (!sPos.startsWith("/") && sPos.contains("/"))
						sPos = sPos.split("/")[0];
					if (sPos.equals("X")) // a noisy tag assigned to 'x', as in '130 x 130'
						sPos = "M";
					else if (sPos.equals("NP"))
						sPos = "NN";
					else if (sPos.equals("VP"))
						sPos = "PU";

					if (!posSet.contains(sPos))
					{
						Console.writeLine("Unknown POS: " + sPos);
						break;
					}

					int iIndex = Integer.parseInt(mc.group(1));
					int iHead = Integer.parseInt(mc.group(2));
					DepTree dw = s.get(i);
					dw.sent = s;
					dw.index = iIndex;
					dw.form = sForm;
					dw.pos = sPos;
					dw.head = iHead;

					// if (i != iIndex || iHead < -1 || iHead >= s.size() || iHead == i)
					if (i != iIndex)
					{
						Console.writeLine(String.format("Illegal index at line %d. Skipping.", iSentence));
						s = null;
						break;
					}

					if (iHead >= 0)
						s.get(iHead).children.add(dw);
				}
				yieldReturn(s);
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
