package corbit.tagdep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import corbit.commons.io.Console;
import corbit.commons.transition.PDAction;
import corbit.commons.util.Statics;
import corbit.tagdep.dict.TagDictionary;
import corbit.tagdep.word.DepTreeSentence;
import corbit.tagdep.word.DepWord;

public class SRParserEvaluator
{
	boolean bParse;
	boolean bDebug;

	int iNumOutSeg = 0;
	int iNumOutTag = 0;
	int iNumOutTagIv = 0;
	int iNumOutTagOv = 0;
	int iNumOutDep = 0;
	int iNumOutNrt = 0;
	int iNumOutRoot = 0;
	int iNumOutSent = 0;

	int iNumRightSeg = 0;
	int iNumRightDep = 0;
	int iNumRightTag = 0;
	int iNumRightTagIv = 0;
	int iNumRightTagOv = 0;
	int iNumRightNrt;
	int iNumRightRoot = 0;
	int iNumRightSent = 0;

	int iNumSeg = 0;
	int iNumTag = 0;
	int iNumTagIv = 0;
	int iNumTagOv = 0;
	int iNumDep = 0;
	int iNumNrt = 0;
	int iNumRoot = 0;
	int iNumSent = 0;

	final int iPosCount;
	final int iActCount;
	
	int[][] miPosConfusion;
	int[][] miActConfusion;

	final TagDictionary m_dict;
	
	public SRParserEvaluator(TagDictionary dict, boolean bParse, boolean bDebug)
	{
		this.m_dict = dict;
		this.bParse = bParse;
		this.bDebug = bDebug;
		iPosCount = m_dict.getTagCount();
		iActCount = PDAction.getActionCount();
		miPosConfusion = new int[iPosCount][iPosCount];
		miActConfusion = new int[iActCount][iActCount];
	}

	public void evalAction(SRParserState so, SRParserState sg)
	{
		List<PDAction> sao = SRParserState.getActionSequence(so);
		List<PDAction> sag = SRParserState.getActionSequence(sg);
		// Console.writeLine("Gold:   " + sag.toString());
		// Console.writeLine("Output: " + sao.toString());
		for (int i=0; i<Math.min(sao.size(), sag.size()); ++i)
		{
			PDAction ao = sao.get(i);
			PDAction ag = sag.get(i);
			if (ao == null || ag == null) break;
			++miActConfusion[PDAction.getActionIndex(ag)][PDAction.getActionIndex(ao)];
		}
	}

	public boolean evalSegmentedSentence(DepTreeSentence osent, DepTreeSentence gsent)
	{
		// store word boundaries
		
		List<Integer> obounds = new ArrayList<Integer>();
		List<Integer> gbounds = new ArrayList<Integer>();
		
		obounds.add(0);
		gbounds.add(0);
		
		for (DepWord dw: osent)
			obounds.add(obounds.get(obounds.size() - 1) + dw.form.length());
		for (DepWord dw: gsent)
			gbounds.add(gbounds.get(gbounds.size() - 1) + dw.form.length());

		if (obounds.get(obounds.size() - 1) != gbounds.get(gbounds.size() - 1))
		{
			Console.writeLine(osent.toString());
			Console.writeLine(gsent.toString());
			throw new IllegalArgumentException("Mismatched reference sentence.");
		}
		
		// begin evaluation
		
		boolean bAllRightSent = true;
		boolean bDepRightSent = true;
		boolean bCompSent = true;

		int i = 0;
		int j = 0;
		
		while (i < gsent.size() || j < osent.size())
		{
			boolean bDepRight = false;
			boolean bPosRight = false;
			boolean bSegRight = false;

			DepWord wg = i < gsent.size() ? gsent.get(i) : null;
			DepWord wo = j < osent.size() ? osent.get(j) : null;

			int gbegin = gbounds.get(i);
			int gend = wg != null ? gbounds.get(i + 1) : -1;
			int obegin = obounds.get(j);
			int oend = wo != null ? obounds.get(j + 1) : -1;
			
			if (wg != null && wo != null && gbegin == obegin && gend == oend)
			{
				bSegRight = true;
				++iNumRightSeg;
				
				if (wo.pos == null || wo.head == -2)
					bCompSent = false;
				
				// tag evaluation
				
				boolean bOOV = !m_dict.hasSeen(wg.form);
				
				if (wg.pos.equals(wo.pos))
				{
					iNumRightTag++;
					if (bOOV) ++iNumRightTagOv; else ++iNumRightTagIv;
					bPosRight = true;
				}
				
				if (wg.pos != null && wo.pos != null)
					++miPosConfusion[m_dict.getTagIndex(wg.pos)][m_dict.getTagIndex(wo.pos)];

				// dependency evaluation, where punctuation symbols are excluded
				
				if (bParse && !wg.pos.equals("PU"))
				{
					if (wg.head == -1 && wo.head == -1
						|| wg.head != -1 && wo.head != -1
						&& gbounds.get(wg.head) == obounds.get(wo.head)
						&& gbounds.get(wg.head + 1) == obounds.get(wo.head + 1))
					{
						iNumRightDep++;
						if (wo.head == -1) ++iNumRightRoot;
						else ++iNumRightNrt;
						bDepRight = true;
					}
					else
						bDepRightSent = false;
				}
			}
			
			// check the exact correctness of the parse to determine if the update is necessary
			if (wo == null || wg == null || obegin != gbegin || oend != gend || !wg.pos.equals(wo.pos)
					|| bParse && !(wo.head == -1 && wg.head == -1
							|| wo.head != -1 && wg.head != -1 && obounds.get(wo.head) == gbounds.get(wg.head) && obounds.get(wo.head + 1) == gbounds.get(wg.head + 1)))
				bAllRightSent = false;

			// display the result
			boolean b1 = wo != null && (wg == null || obegin <= gbegin);
			boolean b2 = wg != null && (wo == null || obegin >= gbegin);

			if (b1) ++iNumOutSeg;
			if (b2) ++iNumSeg;
			if (b1 && wo.pos != null) ++iNumOutTag;
			if (b2 && wg.pos != null) ++iNumTag;
			if (b1 && wo.pos != null)
				if (!m_dict.hasSeen(wo.form)) ++iNumOutTagOv; else ++iNumOutTagIv;
			if (b2 && wg.pos != null)
				if (!m_dict.hasSeen(wg.form)) ++iNumTagOv; else ++iNumTagIv;	
			if (b1 && bParse && !wo.pos.equals("PU") && wo.head != -2) ++iNumOutDep;
			if (b2 && bParse && !wg.pos.equals("PU") && wg.head != -2) ++iNumDep;
			if (b1 && bParse && !wo.pos.equals("PU") && wo.head == -1) ++iNumOutRoot;
			if (b2 && bParse && !wg.pos.equals("PU") && wg.head == -1) ++iNumRoot;
			if (b1 && bParse && !wo.pos.equals("PU") && wo.head >= 0) ++iNumOutNrt;
			if (b2 && bParse && !wg.pos.equals("PU") && wg.head >= 0) ++iNumNrt;
			
			// display the result
			if (bDebug)
			{
				char c0 = bSegRight ? 'O' : 'X';
				char c1 = bSegRight ? bPosRight ? 'O' : 'X' : '-';
				char c2 = (!bParse || wg == null || wg.pos.equals("PU") || !bSegRight) ? '-' : bDepRight ? 'O' : 'X';
				
				String sForm = b1 ? wo.form : "-";
				String sTag = b1 ? wo.pos : "-";
				String sHead = (b1 && wo.head != -2) ? Integer.toString(wo.head) : "-";
				String sIndex = b1 ? Integer.toString(j) : "-";
				String sForm2 = b2 ? wg.form : "-";
				String sTag2 = b2 ? wg.pos : "-";
				String sHead2 = b2 ? Integer.toString(wg.head) : "-";
				String sIndex2 = b2 ? Integer.toString(i) : "-";
				
				Console.writeLine(String.format("%c %c %c %d %2s %2s %12s %4s %6s %12s %4s %6s",
						c0, c1, c2, iNumSent, sIndex, sIndex2, sForm, sTag, sHead, sForm2, sTag2, sHead2));
			}
			if (wo == null) ++i;
			else if (wg == null) ++j;
			else if (obegin < gbegin) ++j;
			else if (obegin > gbegin) ++i;
			else { ++i; ++j; }
		}
		++iNumSent;
		if (bCompSent) ++iNumOutSent;
		if (bDepRightSent) ++iNumRightSent;
		if (bDebug) Console.writeLine(bAllRightSent ? "OOO" : "XXX");
		return bAllRightSent;
	}

	public boolean evalSentence(DepTreeSentence osent, DepTreeSentence gsent)
	{
		boolean bAllRightSent = true;
		boolean bDepRightSent = true;
		boolean bCompSent = true;

		if (osent.size() != gsent.size())
			throw new IllegalArgumentException("The output and gold sentence does not match.");
		
		if (!bParse)
			for (int i = 0; i < osent.size(); ++i)
				osent.get(i).head = gsent.get(i).head;
		
		for (int i = 0; i < gsent.size(); ++i)
		{
			DepWord wg = gsent.get(i);
			DepWord wo = osent.get(i);

			boolean bDepRight = false;
			boolean bPosRight = false;

			// check the exact correctness of the parse to determine if the update is necessary
			if (!wg.pos.equals(wo.pos) || bParse && wg.head != wo.head)
				bAllRightSent = false;

			if (wo.pos == null || wg.head == -2)
				bCompSent = false;
			
			// skip the word with no tag
			if (wg.pos.equals("NONE")) continue;

			// tag evaluation
			boolean bOOV = !m_dict.hasSeen(wo.form);
			
			++iNumTag;
			if (bOOV) ++iNumTagOv; else ++iNumTagIv;
			
			if (wo.pos != null) 
			{
				++iNumOutTag;
				if (bOOV) ++iNumOutTagOv; else ++iNumOutTagIv;
			}

			if (wg.pos.equals(wo.pos))
			{
				iNumRightTag++;
				if (bOOV) ++iNumRightTagOv; else ++iNumRightTagIv;
				bPosRight = true;
			}
			
			if (wg.pos != null && wo.pos != null)
				++miPosConfusion[m_dict.getTagIndex(wg.pos)][m_dict.getTagIndex(wo.pos)];

			// dependency evaluation, where punctuation symbols are excluded
			if (bParse && !wg.pos.equals("PU"))
			{
				++iNumDep;
				if (wo.head != -2)
				{
					++iNumOutDep;
					if (wo.head == -1) ++iNumOutRoot;
					else ++iNumOutNrt;			
				}
				if (wg.head == -1) ++iNumRoot;
				else ++iNumNrt;

				if (wg.head == wo.head)
				{
					iNumRightDep++;
					if (wo.head == -1) ++iNumRightRoot;
					else ++iNumRightNrt;
					bDepRight = true;
				}
				else
					bDepRightSent = false;
			}
			
			// display the result
			if (bDebug)
			{
				char c1 = bPosRight ? 'O' : 'X';
				char c2 = (!bParse || wg.pos.equals("PU")) ? '-' : bDepRight ? 'O' : 'X';
				String sHead = (wo.head == -2) ? "-" : Integer.toString(wo.head);
				Console.writeLine(String.format("%c %c %d %02d %12s %4s %2s %12s %4s %2d", c1, c2, iNumSent, i, wo.form, wo.pos, sHead, wg.form, wg.pos, wg.head));
			}
		}
		++iNumSent;
		if (bCompSent) ++iNumOutSent;
		if (bDepRightSent) ++iNumRightSent;
		if (bDebug) Console.writeLine(bAllRightSent ? "OOO" : "XXX");
		return bAllRightSent;
	}

	/**
	 * evaluate the sentence
	 * @return total score of the sentence (dependency or tagging accuracy)
	 */
	public double evalTotal()
	{
		String[] ssLabels = new String[] { "SEG", "POS", "PSI", "PSO", "DEP", "NRT", "ROT", "SNT" };
		int[][] iiNums = new int[][] {
				{ iNumRightSeg, iNumOutSeg, iNumSeg },
				{ iNumRightTag, iNumOutTag, iNumTag },
				{ iNumRightTagIv, iNumOutTagIv, iNumTagIv },
				{ iNumRightTagOv, iNumOutTagOv, iNumTagOv },
				{ iNumRightDep, iNumOutDep, iNumDep },
				{ iNumRightNrt, iNumOutNrt, iNumNrt },
				{ iNumRightRoot, iNumOutRoot, iNumRoot },
				{ iNumRightSent, iNumOutSent, iNumSent } };
		double[] score = new double[iiNums.length];
		
		for (int i = 0; i < iiNums.length; ++i)
		{
			double dPrec = iiNums[i][1] > 0 ? (double)iiNums[i][0] / (double)iiNums[i][1] : 0;
			double dRecl = iiNums[i][2] > 0 ? (double)iiNums[i][0] / (double)iiNums[i][2] : 0;
			double dFunc = Statics.harmonicMean(dPrec, dRecl);
			
			Console.write(String.format("#%d-%s: ", i + 1, ssLabels[i]));
			Console.write(String.format("F1 %4f | ", dFunc));
			Console.write(String.format("Prec %4f (%6d/%6d) | ", dPrec, iiNums[i][0], iiNums[i][1]));
			Console.write(String.format("Recl %4f (%6d/%6d) | ", dRecl, iiNums[i][0], iiNums[i][2]));
			Console.writeLine();
			
			score[i] = dFunc;
		}
		if (bParse)
			return score[Arrays.asList(ssLabels).indexOf("DEP")];
		else
			return score[Arrays.asList(ssLabels).indexOf("POS")];
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

	public void evalActConfusion()
	{
		String[] sActs = new String[iActCount];
		Console.write("      ");
		for (int i = 0; i < iActCount; ++i)
		{
			sActs[i] = PDAction.getAction(i).toString();
			if (sActs[i].contains("-"))
				sActs[i] = sActs[i].split("-")[1];
			Console.write(String.format("%4s ", sActs[i]));
		}
		Console.write("TPOS ");
		Console.write("TOTL ");
		Console.writeLine();

		int iTotalPos[] = new int[iActCount + 1];
		int iTotal[] = new int[iActCount + 2];
		for (int i = 0; i < iActCount; ++i)
		{
			Console.write(String.format("%4s: ", sActs[i]));
			int _iTotalPos = 0;
			int _iTotal = 0;
			for (int j = 0; j < iActCount; ++j)
			{
				Console.write(String.format("%4d ", miActConfusion[i][j]));
				_iTotal += miActConfusion[i][j];
				iTotal[j] += miActConfusion[i][j];
				iTotal[iActCount + 1] += miActConfusion[i][j];
				if (j > 5)
					_iTotalPos += miActConfusion[i][j];
				if (i > 5)
				{
					iTotalPos[j] += miActConfusion[i][j];
					iTotal[iActCount] += miActConfusion[i][j];
				}
				if (i > 5 && j > 5)
					iTotalPos[iActCount] += miActConfusion[i][j];
			}
			Console.write(String.format("%4d ", _iTotalPos));
			Console.write(String.format("%4d ", _iTotal));
			Console.writeLine();
		}
		Console.write("TPOS: ");
		int _iTotalPos = 0;
		for (int i = 0; i < iActCount + 1; ++i)
		{
			if (i < iActCount)
				_iTotalPos += iTotalPos[i];
			Console.write(String.format("%4s ", iTotalPos[i]));
		}
		Console.write(String.format("%4s ", _iTotalPos));
		Console.writeLine();
		Console.write("TOTL: ");
		for (int i = 0; i < iActCount + 2; ++i)
			Console.write(String.format("%4s ", iTotal[i]));
		Console.writeLine();
		Console.writeLine();
	}

}
