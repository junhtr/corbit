package corbit.commons.io;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import corbit.commons.word.IndexWord;
import corbit.commons.word.ParsedSentence;

public class ParseWriter
{
	private PrintWriter sw;

	public ParseWriter(String sFile) throws FileNotFoundException, UnsupportedEncodingException
	{
		sw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sFile), "UTF-8"));
	}

	public void writeParse(ParsedSentence sent, ParsedSentence gsent, boolean bGoldPos, boolean bGoldHead)
	{
		for (int i = 0; i < sent.size(); ++i)
		{
			IndexWord wo = sent.get(i);
			IndexWord wg = gsent.getWord(sent.getBeginIndex(i), sent.getEndIndex(i));
			int ghead = wg != null ? wg.head : -2;
			int gheadBegin = ghead != -2 ? gsent.getBeginIndex(ghead) : -2;
			int gheadEnd = ghead != -2 ? gsent.getEndIndex(ghead) : -2;
			int headIdx = gheadBegin != -2 && gheadEnd != -2 ? sent.getWordIndex(gheadBegin, gheadEnd) : -2;
			int head = bGoldHead
					? wg != null
							? ghead == -1
									? -1
									: headIdx
							: -2
					: wo.head;
			sw.print(String.format("%d:(%d)_(%s)_(%s) ", i, head, wo.form, bGoldPos ? wg.tag : wo.tag));
		}
		sw.println();
	}

	public void dispose()
	{
		sw.close();
		sw = null;
	}
}
