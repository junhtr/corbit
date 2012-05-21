package corbit.tagdep.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import corbit.commons.util.Generator;
import corbit.tagdep.SRParserEvaluator;
import corbit.tagdep.dict.TagDictionary;
import corbit.tagdep.word.DepTreeSentence;
import corbit.tagdep.word.DepWord;

public abstract class ParseReader extends Generator<DepTreeSentence>
{
	public static void evalPos(TagDictionary dict, String sFile, String sGoldFile) throws IOException
	{
		SRParserEvaluator eval = new SRParserEvaluator(dict, false, true);
		PlainReader pr = new PlainReader(sFile);
		CTBReader cr = new CTBReader(sGoldFile);
		try
		{
			while (pr.hasNext() && cr.hasNext())
				eval.evalSentence(pr.next(), cr.next());
			eval.evalTotal();
		}
		finally
		{
			pr.shutdown();
			cr.shutdown();
		}
	}

	public static void ctbToPlain(String sFile, String sOutFile) throws IOException
	{
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sOutFile), "UTF-8"));
		CTBReader cr = new CTBReader(sFile);
		try
		{
			while (cr.hasNext())
			{
				DepTreeSentence s = cr.next();
				for (int i = 0; i < s.size(); ++i)
					pw.print(s.get(i).form + "/" + s.get(i).pos + " ");
				pw.println();
			}
		}
		finally
		{
			cr.shutdown();
		}
		pw.close();
	}

	public static void maltToDep(String sFile, String sOutFile) throws IOException
	{
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sOutFile), "UTF-8"));
		MaltReader mr = new MaltReader(sFile);
		try
		{
			while (mr.hasNext())
			{
				DepTreeSentence s = mr.next();
				for (int i = 0; i < s.size(); ++i)
				{
					DepWord dw = s.get(i);
					pw.print(String.format("{%d}:({%d})_({%s})_({%s}) ", dw.index, dw.head, dw.form, dw.pos));
				}
				pw.println();
			}
		}
		finally
		{
			mr.shutdown();
		}
		pw.close();
	}

}
