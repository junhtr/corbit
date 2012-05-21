package corbit.segdep.transition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import corbit.commons.dict.TagDictionary;
import corbit.commons.ml.IntFeatVector;
import corbit.commons.ml.WeightVector;
import corbit.commons.transition.SDAction;
import corbit.commons.util.Pair;
import corbit.commons.word.IndexWord;
import corbit.commons.word.ParsedSentence;
import corbit.segdep.SRParserState;
import corbit.segdep.SRParserStateGenerator;
import corbit.segdep.SRParserStats;
import corbit.segdep.handler.SRParserHandler;

public abstract class SRParserTransition
{
	public static class SRParserTransitionParameter
	{
		final boolean bGoldSeg;
		final boolean bGoldTag;
		final boolean bGoldArc;
		final boolean bParse;
		final boolean bCache;
		final boolean bValidateTag;

		public SRParserTransitionParameter(boolean bGoldSeg, boolean bGoldTag, boolean bGoldArc, boolean bParse, boolean bCache, boolean bValidateTag)
		{
			this.bGoldSeg = bGoldSeg;
			this.bGoldTag = bGoldTag;
			this.bGoldArc = bGoldArc;
			this.bParse = bParse;
			this.bCache = bCache;
			this.bValidateTag = bValidateTag;
		}
	}

	protected final SRParserStateGenerator m_generator;
	protected final SRParserHandler m_fhandler;
	protected final WeightVector m_weight;
	protected final TagDictionary m_dict;

	protected final boolean m_bGoldSeg;
	protected final boolean m_bGoldTag;
	protected final boolean m_bGoldArc;
	protected final boolean m_bParse;
	protected final boolean m_bUseCache;
	protected final boolean m_bValidateTag;
	protected final SRParserStats m_stats;

	public static enum Decision {
		NA, SEGMENT, IN_WORD
	}

	protected static final int numMaxWordLength = 32;

	protected SRParserTransition(
			SRParserStateGenerator sg,
			SRParserHandler fh,
			WeightVector w,
			TagDictionary d,
			SRParserStats stats,
			SRParserTransitionParameter params)
	{
		m_generator = sg;
		m_fhandler = fh;
		m_weight = w;
		m_dict = d;

		m_stats = stats;
		m_bGoldSeg = params.bGoldSeg;
		m_bGoldTag = params.bGoldTag;
		m_bGoldArc = params.bGoldArc;
		m_bParse = params.bParse;
		m_bUseCache = params.bCache;
		m_bValidateTag = params.bValidateTag;

		assert (!m_bGoldTag || m_bGoldSeg);
	}

	abstract List<SDAction> getNextActions(SRParserState s, SDAction goldAct, IndexWord nextWord);

	protected abstract SDAction getGoldAction(SRParserState s, ParsedSentence gsent, boolean bGoldStateCHeck);

	abstract List<SRParserState> moveNext(SRParserState s, SDAction act, boolean isGoldAct, boolean bAdd, SRParserCache cache);

	public abstract boolean isEnd(SRParserState s);

	public class SRParserCache
	{
		private Map<SRParserState,Double> wordScoreCache = new HashMap<SRParserState,Double>();

//		private Map<SRParserState, Double> shiftCharCache = new HashMap<SRParserState, Double>();

		public Map<SRParserState,Double> wordScore()
		{
			return wordScoreCache;
		}

//		public Map<SRParserState, Double> charScore()
//		{
//			return shiftCharCache;
//		}
	}

	public List<Pair<SDAction,SRParserState>> moveNext(SRParserState s, ParsedSentence gsent, boolean bAdd)
	{
		List<Pair<SDAction,SRParserState>> l = new ArrayList<Pair<SDAction,SRParserState>>();
		SDAction goldAct = gsent != null ? getGoldAction(s, gsent, !m_bGoldArc) : SDAction.NOT_AVAILABLE;
		boolean bGoldFound = false;
		boolean bGoldSegFound = false;

		IndexWord nextWord = ((m_bGoldSeg || m_bGoldTag) && s.idend < s.sent.length()) ? gsent.findWord(s.idend) : null;
		SRParserCache cache = m_bUseCache ? new SRParserCache() : null;

		for (SDAction act: getNextActions(s, goldAct, nextWord))
		{
//			if ((m_bGoldSeg || m_bGoldTag) && act.isChunkTagAction() && act.getLength() != nextWord.form.length()) continue;
//			if (m_bGoldTag && act.isChunkTagAction() && !act.getPos().equals(nextWord.tag)) continue;
			bGoldFound = bGoldFound || act == goldAct;
			assert (goldAct != null);
			bGoldSegFound = bGoldSegFound || act.getLength() == goldAct.getLength();
			for (SRParserState sNext: moveNext(s, act, act == goldAct, bAdd, cache))
				l.add(new Pair<SDAction,SRParserState>(act, sNext));
		}

		if (s.gold)
			m_stats.numGoldMoves++;
		if (s.gold && bGoldFound)
			m_stats.numGoldMovesCovered++;
		if (s.gold && bGoldSegFound)
			m_stats.numGoldMovesSegCovered++;
//		if (s.gold && bGoldSegFound && !bGoldFound)
//			Console.writeLine(goldAct.toString() + s.sent.substringIgnoreRange(s.curidx, s.curidx + goldAct.m_length) + " was pruned.");

		return l;
	}

	public Pair<SDAction,SRParserState> moveNextGold(SRParserState s, ParsedSentence gsent, boolean bAdd)
	{
		assert (s.gold);
		SDAction act = getGoldAction(s, gsent, true);
		List<SRParserState> ls = moveNext(s, act, true, bAdd, null);
		SRParserState sGold = null;

		for (SRParserState sNext: ls)
		{
			assert (!sNext.gold || sGold == null);
			if (sNext.gold)
				sGold = sNext;
		}

		/* might not be true if pruning is disabled, since states are sorted during pruning */
		assert (sGold != null && sGold == ls.get(0));

		return new Pair<SDAction,SRParserState>(act, sGold);
	}

	public IntFeatVector getPrefixFeatures(SRParserState s)
	{
		IntFeatVector fvprf = new IntFeatVector();
		List<SDAction> lAct = s.getActionSequence();
		SRParserState s3 = m_generator.create(s.sent, s.decision);
		for (SDAction act: lAct)
			s3 = moveNext(s3, act, false, true, null).get(0);
		SRParserState sss1 = s3;
		for (SRParserState sss2 = sss1; sss2 != null; sss2 = sss2.pred0)
		{
			for (IntFeatVector v: sss2.fvins)
				fvprf.append(v);
			if (sss2 != sss1)
				for (IntFeatVector v: sss1.trans.get(sss2).first)
					fvprf.append(v);
			sss1 = sss2;
		}

		return fvprf;
	}

}
