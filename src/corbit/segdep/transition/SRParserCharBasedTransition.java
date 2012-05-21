package corbit.segdep.transition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import corbit.commons.dict.TagDictionary;
import corbit.commons.io.Console;
import corbit.commons.ml.IntFeatVector;
import corbit.commons.ml.WeightVector;
import corbit.commons.transition.SDAction;
import corbit.commons.util.GlobalConf;
import corbit.commons.util.Pair;
import corbit.commons.word.ArcLabel;
import corbit.commons.word.DepChunkTree;
import corbit.commons.word.IndexWord;
import corbit.commons.word.ParsedSentence;
import corbit.commons.word.UnsegmentedSentence;
import corbit.segdep.SRParserState;
import corbit.segdep.SRParserStateGenerator;
import corbit.segdep.SRParserStats;
import corbit.segdep.handler.DelayedFeature;
import corbit.segdep.handler.SRParserHandler;

public class SRParserCharBasedTransition extends SRParserTransition
{
	final boolean m_bAlignArcChar;

	public SRParserCharBasedTransition(
			SRParserStateGenerator sg,
			SRParserHandler fh,
			WeightVector w,
			TagDictionary d,
			SRParserStats stats,
			SRParserTransitionParameter params,
			boolean bAlignArcChar)
	{
		super(sg, fh, w, d, stats, params);
		m_bAlignArcChar = bAlignArcChar;
	}

	public boolean isEnd(SRParserState s)
	{
		return s.curidx == s.sent.length() && (!m_bParse || s.pstck[1] == null);
	}

	@Override
	protected List<SRParserState> moveNext(SRParserState s, SDAction act, boolean isGoldAct, boolean bAdd, SRParserCache cache)
	{
		List<SRParserState> l = new ArrayList<SRParserState>();
		if (act.isShiftTagAction())
			l.add(shiftWithPos(s, act.getTag(), isGoldAct, bAdd, cache));
		else if (act == SDAction.APPEND)
			l.add(append(s, isGoldAct, bAdd, cache));
		else if (act == SDAction.REDUCE_LEFT)
			l.addAll(reduce(s, false, null, isGoldAct, bAdd, cache));
		else if (act == SDAction.REDUCE_RIGHT)
			l.addAll(reduce(s, true, null, isGoldAct, bAdd, cache));
		else if (act == SDAction.END_STATE)
			l.add(s);
		else
			throw new UnsupportedOperationException(act.toString());
		return l;
	}

	protected SDAction getGoldAction(SRParserState s, ParsedSentence gsent, boolean bCheckGoldState)
	{
		if (bCheckGoldState && s.gold == false)
			return SDAction.NOT_AVAILABLE;
		else if (isEnd(s))
			return SDAction.END_STATE;
		
		DepChunkTree ws0 = s.pstck[0];
		DepChunkTree ws1 = s.pstck[1];

		if (m_bParse && ws1 != null)
		{
			int id0 = gsent.getWordIndex(ws0.begin, ws0.end);
			if (id0 >= 0)
			{
				IndexWord gw = gsent.get(id0);
				int hid0 = gw.head;
				int hid1 = -2;
				if (!ws1.isRoot())
				{
					IndexWord w = gsent.getWord(ws1.begin, ws1.end);
					if (w != null) hid1 = w.head;
				}
				if ((!ws1.isRoot() || s.curidx == s.sent.length())
						&& gsent.getBeginIndex(hid0) == ws1.begin && gsent.getEndIndex(hid0) == ws1.end
						&& gsent.getNumChild(id0) == ws0.children.size())
					return SDAction.REDUCE_RIGHT;
				else if (!ws1.isRoot() && gsent.getBeginIndex(hid1) == ws0.begin && gsent.getEndIndex(hid1) == ws0.end)
					return SDAction.REDUCE_LEFT;
			}
			if (s.gold && s.curidx >= s.sent.length())
			{
				Console.write(gsent.toString() + " : ");
				Console.writeLine(ws0.begin + "--" + ws0.end);
				throw new AssertionError("Gold action not found, probably because of malformed input sentence.");
			}
		}

		/* chunk */
		
		IndexWord dw = s.curidx < s.sent.length() ? gsent.findWord(s.curidx) : null;
		if (dw != null)
			return SDAction.getShiftTagAction(dw.tag);
		else
			return SDAction.APPEND;
	}

	GlobalConf conf = GlobalConf.getInstance();
	
	
	/**
	 * get the list of next actions of the given state s
	 * goldAct is only used to calculate statistics for debugging if specified
	 */
	@Override
	List<SDAction> getNextActions(SRParserState s, SDAction goldAct, IndexWord nextWord)
	{
		List<SDAction> l = new ArrayList<SDAction>();
		if (isEnd(s))
		{
			l.add(SDAction.END_STATE);
			return l;
		}

		DepChunkTree ws0 = s.pstck[0];
		DepChunkTree ws1 = s.pstck[1];
		
		UnsegmentedSentence sent = s.sent;
		int sent_len = sent.length();
		int ws0c_size = ws0.children.size();

//		boolean bGoldArc = conf.getConf("gold-arc");
//		if (bGoldArc && goldAct == Action.NOT_AVAILABLE)
//			goldAct = (s.curidx < s.sent.length()) ? !(ws1 != null && ws1.isRoot()) ? Action.APPEND : Action.getShiftTagAction("NN") : Action.REDUCE_RIGHT;
//		assert (!bGoldArc || goldAct != null && goldAct != Action.NOT_AVAILABLE);

		/* to avoid redundant validation of chunk and tag pair */
		
		boolean bValidated = false;
		boolean bValid = false;
		
		/* the condition for the shift/chunk actions to be applied */
		
		if (s.curidx < sent_len)
		{
			Decision ds = s.decision != null && s.curidx > 0 ? s.decision[s.curidx - 1] : Decision.NA;

			if (ds != Decision.SEGMENT && !ws0.isRoot() && ws0c_size == 0)
				if (!m_bValidateTag || m_dict.validateTagForSequence(sent.substring(s.idbgn, sent_len), ws0.tag, s.idend - s.idbgn + 1))
					l.add(SDAction.APPEND);
			if (ds != Decision.IN_WORD && (!m_bGoldArc || goldAct.isShiftTagAction() || goldAct == SDAction.APPEND || goldAct == SDAction.NOT_AVAILABLE))
			{
				if (ws0c_size > 0 || ws0.isRoot() ||
						(!m_bValidateTag || (bValidated = true) && (bValid = m_dict.validateTagForChunk(ws0.form, ws0.tag))))
				{
					String seq = sent.substring(s.curidx, sent_len);
					for (String spqf1: m_dict.getTagCandidatesForSequence(seq))
						l.add(SDAction.getShiftTagAction(spqf1));
				}
			}
//			if (l.size() == 0) Console.writeLine(s.toString());
			assert (ds != Decision.NA || l.size() > 0);
		}

		if (m_bParse && ws1 != null)
		{
			/* If the word has a child, its tag has been already validated. */
			if (ws0c_size > 0 || s.idend < s.curidx ||
					(!m_bValidateTag || (!bValidated || bValid) && (bValidated || m_dict.validateTagForChunk(ws0.form, ws0.tag)) ))
			{
				if (!ws1.isRoot() && (!m_bGoldArc || goldAct == SDAction.REDUCE_LEFT || goldAct == SDAction.APPEND || goldAct == SDAction.NOT_AVAILABLE))
					l.add(SDAction.REDUCE_LEFT);
				if ((!ws1.isRoot() || s.curidx == sent_len) && (!m_bGoldArc || goldAct == SDAction.REDUCE_RIGHT || goldAct == SDAction.APPEND || goldAct == SDAction.NOT_AVAILABLE))
					l.add(SDAction.REDUCE_RIGHT);
			}
		}

//		if (l.size() == 0) Console.writeLine(s.toString());
		assert (!l.contains(null));
		assert (l.size() > 0);
		
		return l;
	}

	private List<SRParserState> reduce(SRParserState s, boolean bRight, String sLabel, boolean bGoldAct, boolean bAdd, SRParserCache cache)
	{
		List<SRParserState> l = new ArrayList<SRParserState>();
		if (!s.pstck[0].isRoot() && s.pstck[0].tag == null) return l;

		SDAction act = sLabel != null ?
				SDAction.getLabeledReduceAction(bRight, sLabel) :
				bRight ? SDAction.REDUCE_RIGHT : SDAction.REDUCE_LEFT;
		List<DelayedFeature> _fvdelay = s.fvdelay != null ? new LinkedList<DelayedFeature>(s.fvdelay) : null;
		Pair<IntFeatVector, Double> vsc = m_fhandler.getFeatures(s, act, _fvdelay, bAdd, m_weight, cache);
		double sr = vsc.second;

		assert (vsc.first == null || m_weight.score(vsc.first) == vsc.second);
		
		for (SRParserState p : s.preds)
		{
			if (p.pstck[0].isRoot() && (!bRight || s.curidx < s.sent.length())) continue;

			Pair<List<IntFeatVector>, Double> t = s.trans.get(p);

			double scdlt = t.second + sr;

			List<IntFeatVector> _fvins = null;
			if (bAdd)
			{
				// This linked-list structure might seem odd, but quite efficient in practice,
				// avoiding frequent addition of two vectors.
				_fvins = new LinkedList<IntFeatVector>(s.fvins);
				_fvins.addAll(p.fvins);
				_fvins.addAll(t.first);
				_fvins.add(vsc.first);
			}
			double _scprf = p.scprf + s.scins + scdlt;
			double _scins = p.scins + s.scins + scdlt;

			DepChunkTree[] _pstck = p.cloneStack();
			DepChunkTree h = new DepChunkTree(bRight ? p.pstck[0] : s.pstck[0]);
			DepChunkTree c = new DepChunkTree(bRight ? s.pstck[0] : p.pstck[0]);
			_pstck[0] = h;

			h.children.add(c);
			c.headBegin = h.begin;
			c.headEnd = h.end;
			c.arcLabel = sLabel != null ? ArcLabel.getLabel(sLabel) : null;

			List<SDAction> _lstact = new LinkedList<SDAction>(p.lstact);
			_lstact.addAll(s.lstact);
			_lstact.add(act);

			assert (c.form.length() == c.end - c.begin);
			int _curstep = m_bAlignArcChar ? s.curstep + 1 : s.curstep;
			l.add(m_generator.generate(s.sent, _pstck, s.curidx, _curstep, Math.max(p.idbgn, 0), s.idend,
					_scprf, _scins, _fvins, _fvdelay, p.preds, p.pred0, p.trans, p.decision,
					_lstact, s.gold && p.gold && bGoldAct, s.nstates));
		}
		return l;
	}

	@SuppressWarnings("serial")
	private SRParserState append(final SRParserState s, boolean bGoldAct, boolean bAdd, SRParserCache cache)
	{
		assert (!isEnd(s));

		List<DelayedFeature> _fvdelay = s.fvdelay != null ? new LinkedList<DelayedFeature>(s.fvdelay) : null;
		final Pair<IntFeatVector, Double> vsc = m_fhandler.getFeatures(s, SDAction.APPEND, _fvdelay, bAdd, m_weight, cache);
		
		assert (vsc.first == null || m_weight.score(vsc.first) == vsc.second);
		
		double scdlt = vsc.second;
		double _scprf = s.scprf + scdlt;
		double _scins = s.scins + scdlt;

		DepChunkTree[] _pstck = s.cloneStack();
		DepChunkTree dt = _pstck[0];
		_pstck[0] = new DepChunkTree(dt.sent, dt.begin, dt.end + 1, dt.tag, dt.headBegin, dt.headEnd, dt.arcLabel);
		
		List<IntFeatVector> _fvins = bAdd ? new LinkedList<IntFeatVector>(s.fvins) {{ add(vsc.first); }}: null;
		List<SDAction> _lstact = new LinkedList<SDAction>(s.lstact) {{ add(SDAction.APPEND); }};
		
		int _curstep = m_bAlignArcChar ? s.curstep + 2 : s.curstep + 1;
		
		return m_generator.generate(s.sent, _pstck, s.curidx + 1, _curstep, dt.begin, dt.end + 1,
				_scprf, _scins, _fvins, s.fvdelay, s.preds, s.pred0,
				s.trans, s.decision, _lstact, s.gold && bGoldAct, s.nstates);
	}
	
	@SuppressWarnings("serial")
	private SRParserState shiftWithPos(final SRParserState s, String sPos, boolean bGoldAct, boolean bAdd, SRParserCache cache)
	{
		assert (!isEnd(s));
		
		final SDAction act = SDAction.getShiftTagAction(sPos);
		List<DelayedFeature> _fvdelay = s.fvdelay != null ? new LinkedList<DelayedFeature>(s.fvdelay) : null;
		final Pair<IntFeatVector, Double> vsc = m_fhandler.getFeatures(s, act, _fvdelay, bAdd, m_weight, cache);
		final List<IntFeatVector> _vc = bAdd ? new LinkedList<IntFeatVector>() {{ add(vsc.first); }} : null;
		
		assert (vsc.first == null || m_weight.score(vsc.first) == vsc.second);
		
		final double scdlt = vsc.second;
		double _scprf = s.scprf + scdlt;
		double _scins = 0.0d;
		
		DepChunkTree[] _pstck = s.pushStack(new DepChunkTree(s.sent, s.curidx, s.curidx + 1, sPos, -2, -2, null));
		Set<SRParserState> _preds = new HashSet<SRParserState>() {{ add(s); }};
		Map<SRParserState, Pair<List<IntFeatVector>, Double>> _trans =
			new LinkedHashMap<SRParserState, Pair<List<IntFeatVector>, Double>>()
			{{ put(s, new Pair<List<IntFeatVector>, Double>(_vc, scdlt)); }};
		List<IntFeatVector> _fvins = bAdd ? new LinkedList<IntFeatVector>() {{ add (vsc.first); }}: null;
		List<SDAction> _lstact = new LinkedList<SDAction>() {{ add(act); }};

		int _curstep = s.curstep + 1;
		
		return m_generator.generate(s.sent, _pstck, s.curidx + 1, _curstep, s.curidx, s.curidx + 1,
				_scprf, _scins, _fvins, _fvdelay, _preds, s,
				_trans, s.decision, _lstact, s.gold && bGoldAct, s.nstates);
	}

}
