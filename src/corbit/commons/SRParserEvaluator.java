package corbit.commons;

import java.util.Arrays;

import corbit.commons.dict.TagDictionary;
import corbit.commons.io.Console;
import corbit.commons.util.Statics;
import corbit.commons.word.DepChunk;
import corbit.commons.word.ParsedSentence;

public class SRParserEvaluator
{
	final boolean bParse;
	final boolean bLabeled;
	final boolean bShowOutput;
	final boolean bInfreqAsOOV;
	final boolean bDetailed;

	int iNumOutSeg = 0;
	int iNumOutSegIv = 0;
	int iNumOutSegOv = 0;
	int iNumOutSegHub = 0;
	int iNumOutTag = 0;
	int iNumOutTagIv = 0;
	int iNumOutTagOv = 0;
	int iNumOutTagHub = 0;
	int iNumOutDep = 0;
	int iNumOutLDep = 0;
	int iNumOutNrt = 0;
	int iNumOutRoot = 0;
	int iNumOutTree = 0;
	int iNumOutSent = 0;

	int iNumRightSeg = 0;
	int iNumRightSegIv = 0;
	int iNumRightSegOv = 0;
	int iNumRightSegHub = 0;
	int iNumRightTag = 0;
	int iNumRightTagIv = 0;
	int iNumRightTagOv = 0;
	int iNumRightTagHub = 0;
	int iNumRightDep = 0;
	int iNumRightLDep = 0;
	int iNumRightNrt = 0;
	int iNumRightRoot = 0;
	int iNumRightTree = 0;
	int iNumRightSent = 0;

	int iNumSeg = 0;
	int iNumSegIv = 0;
	int iNumSegOv = 0;
	int iNumSegHub = 0;
	int iNumTag = 0;
	int iNumTagIv = 0;
	int iNumTagOv = 0;
	int iNumTagHub = 0;
	int iNumDep = 0;
	int iNumLDep = 0;
	int iNumNrt = 0;
	int iNumRoot = 0;
	int iNumTree = 0;
	int iNumSent = 0;

	final int iPosCount;
	final int iActCount;

	int[][] miPosConfusion;
	int[][] miActConfusion;

	final TagDictionary m_dict;

	public SRParserEvaluator(TagDictionary dict, boolean bParse, boolean bLabeled, boolean bShowOutput, boolean bInfreqAsOOV, boolean bDetailed)
	{
		this.m_dict = dict;
		this.bInfreqAsOOV = bInfreqAsOOV;
		this.bParse = bParse;
		this.bLabeled = bLabeled;
		this.bShowOutput = bShowOutput;
		this.bDetailed = bDetailed;
		iPosCount = m_dict.getTagCount();
//		iActCount = Action.getActionCount();
		iActCount = 0;
		miPosConfusion = new int[iPosCount][iPosCount];
		miActConfusion = new int[iActCount][iActCount];
	}

/*	public void evalAction(SRParserState so, SRParserState sg)
	{
		List<Action> sao = SRParserState.getActionSequence(so);
		List<Action> sag = SRParserState.getActionSequence(sg);
		// Console.writeLine("Gold:   " + sag.toString());
		// Console.writeLine("Output: " + sao.toString());
		for (int i=0; i<Math.min(sao.size(), sag.size()); ++i)
		{
			Action ao = sao.get(i);
			Action ag = sag.get(i);
			if (ao == null || ag == null) break;
//			++miActConfusion[Action.getActionIndex(ag)][Action.getActionIndex(ao)];
		}
	}
*/

	public boolean evalSentence(ParsedSentence osent, ParsedSentence gsent)
	{
		boolean bAllRightSent = true;
		boolean bLDepRightSent = true;
		boolean bDepRightSent = true;
		boolean bTagRightSent = true;
		boolean bSegRightSent = true;
		boolean bCompleteSent = true;

		int i = 0;
		int j = 0;

		while (i < gsent.size() || j < osent.size())
		{
			boolean bLDepRight = true;
			boolean bDepRight = true;
			boolean bTagRight = true;
			boolean bSegRight = true;

			DepChunk wg = i < gsent.size() ? gsent.get(i).toDepChunk() : null;
			DepChunk wo = j < osent.size() ? osent.get(j).toDepChunk() : null;

			boolean bGoldOOV = wg != null && (bInfreqAsOOV && m_dict.isFrequent(wg.form) || !m_dict.hasSeen(wg.form));
			boolean bOutOOV = wo != null && (bInfreqAsOOV && m_dict.isFrequent(wo.form) || !m_dict.hasSeen(wo.form));
			boolean bGoldHub = wg != null && gsent.getNumChild(i) > 2;
			boolean bOutHub = wo != null && osent.getNumChild(j) > 2;

			// evaluating segments
			if (wg != null && wo != null && wo.begin == wg.begin && wo.end == wg.end)
			{
				++iNumRightSeg;
				if (bGoldOOV)
					iNumRightSegOv++;
				else
					iNumRightSegIv++;
				if (bGoldHub)
					iNumRightSegHub++;

				if (wo.tag == null || wo.headBegin == -2)
					bCompleteSent = false;

				// evaluating tags
				if (wg.tag.equals(wo.tag))
				{
					iNumRightTag++;
					if (bGoldOOV)
						iNumRightTagOv++;
					else
						iNumRightTagIv++;
					if (bGoldHub)
						iNumRightTagHub++;
				}
				else
					bTagRight = false;

				if (wg.tag != null && wo.tag != null)
					++miPosConfusion[m_dict.getTagIndex(wg.tag)][m_dict.getTagIndex(wo.tag)];

				// evaluating dependencies with punctuation excluded
				if (bParse && !wg.tag.equals("PU"))
				{
					if (wg.headBegin == wo.headBegin && wg.headEnd == wo.headEnd)
					{
						iNumRightDep++;
						if (wg.arcLabel == wo.arcLabel)
							iNumRightLDep++;
						if (wo.headBegin == -1)
							++iNumRightRoot;
						else
							++iNumRightNrt;
					}
					else
						bDepRight = false;
				}
			}
			else
				bSegRight = false;

			// check the exact correctness of the parse to determine if the update is necessary
			if (wo == null || wg == null
					|| wo.begin != wg.begin || wo.end != wg.end || !wg.tag.equals(wo.tag)
					|| bParse && (wg.headBegin != wo.headBegin || wg.headEnd != wo.headEnd)
					|| bParse && bLabeled && wg.arcLabel != wo.arcLabel)
				bAllRightSent = false;

			// sentence-level evaluation
			bLDepRightSent = bLDepRightSent && bLDepRight;
			bDepRightSent = bDepRightSent && bDepRight;
			bTagRightSent = bTagRightSent && bTagRight;
			bSegRightSent = bSegRightSent && bSegRight;

			// display the result
			boolean b1 = wo != null && (wg == null || wo.begin <= wg.begin);
			boolean b2 = wg != null && (wo == null || wo.begin >= wg.begin);

			if (b1)
				++iNumOutSeg;
			if (b2)
				++iNumSeg;
			if (b1 && !bOutOOV)
				++iNumOutSegIv;
			if (b2 && !bGoldOOV)
				++iNumSegIv;
			if (b1 && bOutOOV)
				++iNumOutSegOv;
			if (b2 && bGoldOOV)
				++iNumSegOv;
			if (b1 && bOutHub)
				++iNumOutSegHub;
			if (b2 && bGoldHub)
				++iNumSegHub;
			if (b1 && wo.tag != null)
				++iNumOutTag;
			if (b2 && wg.tag != null)
				++iNumTag;
			if (b1 && wo.tag != null && !bOutOOV)
				++iNumOutTagIv;
			if (b2 && wg.tag != null && !bGoldOOV)
				++iNumTagIv;
			if (b1 && wo.tag != null && bOutOOV)
				++iNumOutTagOv;
			if (b2 && wg.tag != null && bGoldOOV)
				++iNumTagOv;
			if (b1 && wo.tag != null && bOutHub)
				++iNumOutTagHub;
			if (b2 && wg.tag != null && bGoldHub)
				++iNumTagHub;
			if (b1 && bParse && !wo.tag.equals("PU") && wo.headBegin != -2)
				++iNumOutDep;
			if (b2 && bParse && !wg.tag.equals("PU") && wg.headBegin != -2)
				++iNumDep;
			if (b1 && bParse && !wo.tag.equals("PU") && wo.headBegin != -2 && wo.arcLabel != null)
				++iNumOutLDep;
			if (b2 && bParse && !wg.tag.equals("PU") && wg.headBegin != -2 && wg.arcLabel != null)
				++iNumLDep;
			if (b1 && bParse && !wo.tag.equals("PU") && wo.headBegin == -1)
				++iNumOutRoot;
			if (b2 && bParse && !wg.tag.equals("PU") && wg.headBegin == -1)
				++iNumRoot;
			if (b1 && bParse && !wo.tag.equals("PU") && wo.headBegin >= 0)
				++iNumOutNrt;
			if (b2 && bParse && !wg.tag.equals("PU") && wg.headBegin >= 0)
				++iNumNrt;

			final int maxlen = 24;

			if (bShowOutput)
			{
				char c0 = bSegRight ? 'O' : 'X';
				char c1 = bSegRight ? bTagRight ? 'O' : 'X' : '-';
				char c2 = (!bParse || wg == null || wg.tag.equals("PU") || !bSegRight) ? '-' : bDepRight ? bLDepRight ? '8' : 'O' : 'X';

				String sForm = b1 ? bOutOOV ? wo.form + "*" : wo.form : "-";
				String sTag = b1 ? wo.tag : "-";
//				String sHead = (b1 && wo.headBegin != -2) ? wo.headBegin + ":" + wo.headEnd : "-";
				String sHead = (b1 && wo.headBegin != -2) ? Integer.toString(osent.get(j).head) : "-";
				String sIndex = b1 ? Integer.toString(j) : "-";
				String sForm2 = b2 ? bGoldOOV ? wg.form + "*" : wg.form : "-";
				String sTag2 = b2 ? wg.tag : "-";
//				String sHead2 = b2 ? wg.headBegin + ":" + wg.headEnd : "-";
				String sHead2 = b2 ? Integer.toString(gsent.get(i).head) : "-";
				String sIndex2 = b2 ? Integer.toString(i) : "-";

				int len1 = Statics.multiByteLength(sForm);
				int len2 = Statics.multiByteLength(sForm2);
				sForm = Statics.charMultiplyBy(' ', Math.max(0, (maxlen - len1 - 1) / 2)) + sForm + Statics.charMultiplyBy(' ', Math.max(0, (maxlen - len1) / 2));
				sForm2 = Statics.charMultiplyBy(' ', Math.max(0, (maxlen - len2 - 1) / 2)) + sForm2 + Statics.charMultiplyBy(' ', Math.max(0, (maxlen - len2) / 2));
				Console.writeLine(String.format("%c %c %c %d %2s %2s %s %4s %6s %s %4s %6s",
						c0, c1, c2, iNumSent, sIndex, sIndex2, sForm, sTag, sHead, sForm2, sTag2, sHead2));
			}

			if (wo == null)
				++i;
			else if (wg == null)
				++j;
			else if (wo.begin < wg.begin)
				++j;
			else if (wo.begin > wg.begin)
				++i;
			else {
				++i;
				++j;
			}
		}
		if (bShowOutput)
			Console.writeLine();

		++iNumSent;
		if (bCompleteSent)
			++iNumOutSent;
		if (bCompleteSent)
			++iNumOutTree;
		if (bSegRightSent && bDepRightSent)
			++iNumRightTree;
		if (bSegRightSent && bTagRightSent && bDepRightSent)
			++iNumRightSent;
		if (bShowOutput)
		{
			String s;
			if (bSegRightSent)
			{
				if (bDepRightSent)
					s = bAllRightSent ? "★★★" : "☆☆☆";
				else if (bTagRightSent)
					s = "●●●";
				else
					s = "○○○";
			}
			else
				s = "×××";
			Console.writeLine(s);
		}
		return bAllRightSent;
	}

	public double evalTotal()
	{
		String[] ssLabels = bDetailed
				? new String[] { "SEG", "Siv", "Sov", "Shb", "TAG", "Tiv", "Tov", "Thb", "UAS", " nr", " rt", "TRE", "LAS", "ALL" }
				: new String[] { "SEG", "TAG", "UAS", "LAS", "ROT", "TRE", "ALL" };
		int[][] iiNums = bDetailed
				? new int[][] {
						{ iNumRightSeg, iNumOutSeg, iNumSeg },
						{ iNumRightSegIv, iNumOutSegIv, iNumSegIv },
						{ iNumRightSegOv, iNumOutSegOv, iNumSegOv },
						{ iNumRightSegHub, iNumOutSegHub, iNumSegHub },
						{ iNumRightTag, iNumOutTag, iNumTag },
						{ iNumRightTagIv, iNumOutTagIv, iNumTagIv },
						{ iNumRightTagOv, iNumOutTagOv, iNumTagOv },
						{ iNumRightTagHub, iNumOutTagHub, iNumTagHub },
						{ iNumRightDep, iNumOutDep, iNumDep },
						{ iNumRightNrt, iNumOutNrt, iNumNrt },
						{ iNumRightRoot, iNumOutRoot, iNumRoot },
						{ iNumRightTree, iNumOutTree, iNumTree },
						{ iNumRightLDep, iNumOutLDep, iNumLDep },
						{ iNumRightSent, iNumOutSent, iNumSent } }
				: new int[][] {
						{ iNumRightSeg, iNumOutSeg, iNumSeg },
						{ iNumRightTag, iNumOutTag, iNumTag },
						{ iNumRightDep, iNumOutDep, iNumDep },
						{ iNumRightLDep, iNumOutLDep, iNumLDep },
						{ iNumRightRoot, iNumOutRoot, iNumRoot },
						{ iNumRightTree, iNumOutTree, iNumTree },
						{ iNumRightSent, iNumOutSent, iNumSent } };

		double[] score = new double[iiNums.length];

		for (int i = 0; i < iiNums.length; ++i)
		{
			double dPrec = iiNums[i][1] > 0 ? (double)iiNums[i][0] / (double)iiNums[i][1] : 0;
			double dRecl = iiNums[i][2] > 0 ? (double)iiNums[i][0] / (double)iiNums[i][2] : 0;
			double dFunc = Statics.harmonicMean(dPrec, dRecl);

			Console.write(String.format("#%x-%s: ", i + 1, ssLabels[i]));
			Console.write(String.format("F1 %4f | ", dFunc));
			Console.write(String.format("Prec %4f (%6d/%6d) | ", dPrec, iiNums[i][0], iiNums[i][1]));
			Console.write(String.format("Recl %4f (%6d/%6d) | ", dRecl, iiNums[i][0], iiNums[i][2]));
			Console.writeLine();

			score[i] = dFunc;
		}
		if (bParse)
			return score[Arrays.asList(ssLabels).indexOf("UAS")];
		else
			return score[Arrays.asList(ssLabels).indexOf("TAG")];
	}

	public void evalPosConfusion()
	{
		String[] pos = m_dict.getTagList();
		Console.write("      ");
		for (int i = 0; i < iPosCount; ++i)
			Console.write(String.format("%4s ", pos[i]));
		Console.writeLine();

		for (int i = 0; i < iPosCount; ++i)
		{
			Console.write(String.format("%4s: ", pos[i]));
			for (int j = 0; j < iPosCount; ++j)
				Console.write(String.format("%4d ", miPosConfusion[i][j]));
			Console.writeLine();
		}
		Console.writeLine();
	}

//	public void evalActConfusion()
//	{
//		String[] sActs = new String[iActCount];
//		Console.write("      ");
//		for (int i = 0; i < iActCount; ++i)
//		{
//			sActs[i] = Action.getAction(i).toString();
//			if (sActs[i].contains("-"))
//				sActs[i] = sActs[i].split("-")[1];
//			Console.write(String.format("%4s ", sActs[i]));
//		}
//		Console.write("TPOS ");
//		Console.write("TOTL ");
//		Console.writeLine();
//
//		int iTotalPos[] = new int[iActCount + 1];
//		int iTotal[] = new int[iActCount + 2];
//		for (int i = 0; i < iActCount; ++i)
//		{
//			Console.write(String.format("%4s: ", sActs[i]));
//			int _iTotalPos = 0;
//			int _iTotal = 0;
//			for (int j = 0; j < iActCount; ++j)
//			{
//				Console.write(String.format("%4d ", miActConfusion[i][j]));
//				_iTotal += miActConfusion[i][j];
//				iTotal[j] += miActConfusion[i][j];
//				iTotal[iActCount + 1] += miActConfusion[i][j];
//				if (j > 5)
//					_iTotalPos += miActConfusion[i][j];
//				if (i > 5)
//				{
//					iTotalPos[j] += miActConfusion[i][j];
//					iTotal[iActCount] += miActConfusion[i][j];
//				}
//				if (i > 5 && j > 5)
//					iTotalPos[iActCount] += miActConfusion[i][j];
//			}
//			Console.write(String.format("%4d ", _iTotalPos));
//			Console.write(String.format("%4d ", _iTotal));
//			Console.writeLine();
//		}
//		Console.write("TPOS: ");
//		int _iTotalPos = 0;
//		for (int i = 0; i < iActCount + 1; ++i)
//		{
//			if (i < iActCount)
//				_iTotalPos += iTotalPos[i];
//			Console.write(String.format("%4s ", iTotalPos[i]));
//		}
//		Console.write(String.format("%4s ", _iTotalPos));
//		Console.writeLine();
//		Console.write("TOTL: ");
//		for (int i = 0; i < iActCount + 2; ++i)
//			Console.write(String.format("%4s ", iTotal[i]));
//		Console.writeLine();
//		Console.writeLine();
//	}

}
