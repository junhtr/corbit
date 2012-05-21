package corbit.tagdep.io;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import corbit.tagdep.word.DepTree;
import corbit.tagdep.word.DepTreeSentence;

public class ParseWriter
{
	private PrintWriter sw;

	public ParseWriter(String sFile) throws FileNotFoundException, UnsupportedEncodingException
	{
		sw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sFile), "UTF-8"));
	}

	public void writeParse(DepTreeSentence sent, DepTreeSentence gsent, boolean bGoldPos, boolean bGoldHead)
	{
		assert (sent.size() == gsent.size());
		for (int i = 0; i < sent.size(); ++i)
		{
			DepTree dt = sent.get(i);
			DepTree dg = gsent.get(i);
			sw.print(String.format("%d:(%d)_(%s)_(%s) ", i, bGoldHead ? dg.head : dt.head, dt.form, bGoldPos ? dg.pos : dt.pos));
		}
		sw.println();
	}

	public void dispose()
	{
		sw.close();
		sw = null;
	}
}
