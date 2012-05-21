package corbit.tagdep.transition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import corbit.commons.io.Console;
import corbit.commons.ml.IntFeatVector;
import corbit.commons.ml.WeightVector;
import corbit.commons.transition.PDAction;
import corbit.commons.util.Pair;
import corbit.tagdep.SRParserState;
import corbit.tagdep.SRParserStateGenerator;
import corbit.tagdep.dict.TagDictionary;
import corbit.tagdep.handler.SRParserHandler;
import corbit.tagdep.word.DepTree;
import corbit.tagdep.word.DepTreeSentence;

public class SRParserTransitionStd extends SRParserTransition
{
	public boolean m_bAssignPosFollowsShift;
	boolean m_bAssignGoldPos;
	boolean m_bIndepSPActions;
	int m_iLookaheadDepth = 0;
	
	public SRParserTransitionStd(SRParserStateGenerator sg, SRParserHandler fh, WeightVector w, TagDictionary d,
			boolean bParse, boolean bReduceFollowsShift, boolean bAssignGoldPos, boolean bIndepSPActions)
	{
		super(sg, fh, w, d, bParse);
		this.m_bAssignPosFollowsShift = bReduceFollowsShift;
		this.m_bAssignGoldPos = bAssignGoldPos;
		this.m_bIndepSPActions = bIndepSPActions;
	}

	@Override
	public Pair<PDAction, SRParserState> moveNextGold(SRParserState s, DepTreeSentence gsent, boolean bAdd)
	{
		PDAction act = getGoldAction(s, gsent);
		List<SRParserState> ls = moveNext(s, act, true, bAdd);
		SRParserState sGold = null;

		for (SRParserState sNext : ls)
		{
			assert (!sNext.gold || sGold == null);
			if (sNext.gold) sGold = sNext;
		}

		// might not be true if pruning is disabled, because states are sorted during pruning process
		assert (sGold != null && sGold == ls.get(0));

		return new Pair<PDAction, SRParserState>(act, sGold);
	}

	@Override
	public List<Pair<PDAction, SRParserState>> moveNext(SRParserState s, DepTreeSentence gsent, boolean bAdd)
	{
		List<Pair<PDAction, SRParserState>> l = new ArrayList<Pair<PDAction, SRParserState>>();
		PDAction goldAct = gsent != null ? getGoldAction(s, gsent) : PDAction.NOT_AVAILABLE;

		for (PDAction act : getNextActions(s, m_bAssignGoldPos ? gsent : null))
			for (SRParserState sNext : moveNext(s, act, act.shallowEquals(goldAct), bAdd))
				// TODO: shallow unnecessary
				l.add(new Pair<PDAction, SRParserState>(act, sNext));
		return l;
	}

	PDAction getGoldAction(SRParserState s, DepTreeSentence gsent)
	{
		if (s.gold == false) return PDAction.NOT_AVAILABLE;
//		if (!s.gold) throw new IllegalArgumentException("Cannot get the gold action for non-gold state.");
		
		if (isEnd(s))
			return PDAction.END_STATE;

		DepTree ws0 = s.pstck[0];
		DepTree ws1 = s.pstck[1];

		if (!m_bAssignPosFollowsShift
				&& ws0.index != -1
				&& s.pos[ws0.index] == null) // if m_bUseGoldPos == true, this pos is already assigned and shift action is omitted.
		{
			return PDAction.getPosAction(gsent.get(ws0.index).pos);
		}
		else
		{
			if (m_bParse && ws1 != null)
			{
				if ((ws1.index != -1 || s.curidx == s.sent.size())
						&& gsent.get(ws0.index).head == ws1.index
						&& gsent.get(ws0.index).children.size() == ws0.children.size())
					return PDAction.REDUCE_RIGHT;
				else if (ws1.index != -1 && ws0.index == gsent.get(ws1.index).head)
					return PDAction.REDUCE_LEFT;
			}

			if (s.curidx == s.sent.size())
			{
				Console.writeLine("No gold action found: " + s);
				return PDAction.NOT_AVAILABLE;
			}

			if (m_bAssignPosFollowsShift)
				return PDAction.getShiftPosAction(gsent.get(s.curidx).pos);
			else
				return PDAction.SHIFT;
		}
	}

	List<PDAction> getNextActions(SRParserState s, DepTreeSentence gsent)
	{
		assert (m_bAssignGoldPos || gsent == null);

		List<PDAction> l = new ArrayList<PDAction>();
		if (isEnd(s))
		{
			l.add(PDAction.END_STATE);
			return l;
		}

		DepTree ws0 = s.pstck[0];
		DepTree ws1 = s.pstck[1];

		if (!m_bAssignPosFollowsShift
				&& ws0.index != -1
				&& s.pos[ws0.index] == null) // if m_bUseGoldPos == true, this pos is already assigned and shift action is omitted.
		{
			if (m_bAssignGoldPos)
				l.add(PDAction.getPosAction(gsent.get(ws0.index).pos));
			else
				for (String sPos : m_dict.getTagCandidates(ws0.form))
					l.add(PDAction.getPosAction(sPos));
		}
		else
		{
			if (s.curidx < s.sent.size())
			{
				if (m_bAssignPosFollowsShift)
				{
					if (m_bAssignGoldPos)
						l.add(PDAction.getShiftPosAction(gsent.get(s.curidx).pos));
					else
					{
						for (String spqf1 : m_dict.getTagCandidates(s.sent.get(s.curidx).form))
							l.add(PDAction.getShiftPosAction(spqf1));
					}
				}
				else
					l.add(PDAction.SHIFT);
			}

			if (m_bParse && ws1 != null)
			{
				l.add(PDAction.REDUCE_LEFT);
				l.add(PDAction.REDUCE_RIGHT);
			}
		}
		assert (!l.contains(null));
		return l;
	}

	public void clear()
	{
//		m_trans.clear();
	}

	List<SRParserState> moveNext(SRParserState s, PDAction act, boolean isGoldAct, boolean bAdd)
	{
		List<SRParserState> l;
		if (act == PDAction.REDUCE_RIGHT)
			l = reduceRight(s, isGoldAct, bAdd);
		else if (act == PDAction.REDUCE_LEFT)
			l = reduceLeft(s, isGoldAct, bAdd);
		else
		{
			l = new ArrayList<SRParserState>();
			if (act == PDAction.END_STATE || act == PDAction.PENDING)
				l.add(s);
			else if (act == PDAction.SHIFT)
				l.add(shift(s, isGoldAct, bAdd));
			else if (act.isPosAction())
				l.add(assignPos(s, act.getPos(), isGoldAct, bAdd));
			else if (act.isShiftPosAction())
			{
				String sPos = act.getPos();
				SRParserState ss = m_bIndepSPActions ?
						shiftWithPos(s, sPos, isGoldAct, bAdd) :
						assignPos(shift(s, isGoldAct, bAdd), sPos, isGoldAct, bAdd);
				l.add(ss);
			}
		}
		return l;
	}

	SRParserState shiftWithPos(SRParserState s, String sPos, boolean bGoldAct, boolean bAdd)
	{
		assert (!(s.curidx == s.sent.size() || s.pstck[0].index != -1 && s.pos[s.pstck[0].index] == null));

		List<String> _fvdelay = s.fvdelay != null ? new LinkedList<String>(s.fvdelay) : null;
		IntFeatVector vs = m_fhandler.getFeatures(s, PDAction.getShiftPosAction(sPos), _fvdelay, bAdd);
		double scdlt = m_weight.score(vs);
		if (!bAdd) vs = null;
		List<IntFeatVector> _fvins = bAdd ? new LinkedList<IntFeatVector>() : null;
		double _scprf = s.scprf + scdlt;
		double _scins = 0.0d;

		DepTree[] _pstck = s.pushStack(new DepTree(s.sent.get(s.curidx)));
		Set<SRParserState> _preds = new HashSet<SRParserState>();
		_preds.add(s);
		Map<SRParserState, Pair<IntFeatVector, Double>> _trans = new LinkedHashMap<SRParserState, Pair<IntFeatVector, Double>>();
		_trans.put(s, new Pair<IntFeatVector, Double>(vs, scdlt));
		int[] _heads = new int[s.sent.size()];
		Arrays.fill(_heads, -2);
		String[] _pos = new String[s.sent.size()];
		_pos[_pstck[0].index] = sPos;
		_pstck[0].pos = sPos;

		List<PDAction> _lstact = new LinkedList<PDAction>();
		_lstact.add(PDAction.getShiftPosAction(sPos));

		return m_generator.generate(s.sent, _pstck, s.curidx + 1, s.idend + 1, s.idend + 1,
				_scprf, _scins, scdlt, _fvins, vs, _preds, s, _trans, _heads, _pos, _fvdelay,
				_lstact, s.gold && bGoldAct, s.nstates);
	}

	SRParserState shift(SRParserState s, boolean bGoldAct, boolean bAdd)
	{
		assert (!(s.curidx == s.sent.size() || s.pstck[0].index != -1 && s.pos[s.pstck[0].index] == null));

		List<String> _fvdelay = s.fvdelay != null ? new LinkedList<String>(s.fvdelay) : null;
		IntFeatVector vs = m_fhandler.getFeatures(s, PDAction.SHIFT, _fvdelay, bAdd);
		double scdlt = m_weight.score(vs);
		if (!bAdd) vs = null;
		List<IntFeatVector> _fvins = bAdd ? new LinkedList<IntFeatVector>() : null;
		double _scprf = s.scprf + scdlt;
		double _scins = 0.0;

		DepTree[] _pstck = s.pushStack(new DepTree(s.sent.get(s.curidx)));
		Set<SRParserState> _preds = new HashSet<SRParserState>();
		_preds.add(s);
		Map<SRParserState, Pair<IntFeatVector, Double>> _trans = new LinkedHashMap<SRParserState, Pair<IntFeatVector, Double>>();
		_trans.put(s, new Pair<IntFeatVector, Double>(vs, scdlt));
		int[] _heads = new int[s.sent.size()];
		Arrays.fill(_heads, -2);
		String[] _pos = new String[s.sent.size()];

		// remove the POS information if already defined (possibly with --gold-pos option)
		// if (_pstck[0].pos != null) _pstck[0].pos = null;
		if (_pstck[0].pos != null)
			_pos[s.curidx] = _pstck[0].pos;

		List<PDAction> _lstact = new LinkedList<PDAction>();
		_lstact.add(PDAction.SHIFT);

		return m_generator.generate(s.sent, _pstck, s.curidx + 1, s.idend + 1, s.idend + 1,
				_scprf, _scins, scdlt, _fvins, vs, _preds, s, _trans, _heads, _pos, _fvdelay,
				_lstact, s.gold && bGoldAct, s.nstates);
	}

	SRParserState assignPos(SRParserState s, String sPos, boolean bGoldAct, boolean bAdd)
	{
		assert (s.pstck[0].index == -1 || s.pos[s.pstck[0].index] == null);

		// update feature vector and scores

		List<String> _fvdelay = s.fvdelay != null ? new LinkedList<String>(s.fvdelay) : null;
		IntFeatVector vr = m_fhandler.getFeatures(s, PDAction.getPosAction(sPos), _fvdelay, bAdd);
		double scdlt = m_weight.score(vr);
		if (!bAdd) vr = null;
		List<IntFeatVector> _fvins = bAdd ? new LinkedList<IntFeatVector>(s.fvins) : null;
		if (bAdd) _fvins.add(vr);
		double _scprf = s.scprf + scdlt;
		double _scins = s.scins + scdlt;

		// update pos
		String[] _pos = Arrays.copyOf(s.pos, s.pos.length);
		_pos[s.pstck[0].index] = sPos;

		DepTree[] _pstck = s.cloneStack();
		_pstck[0].pos = sPos;

		List<PDAction> _lstact = new LinkedList<PDAction>(s.lstact);
		_lstact.add(PDAction.getPosAction(sPos));

		return m_generator.generate(s.sent, _pstck, s.curidx, s.idbgn, s.idend,
				_scprf, _scins, scdlt, _fvins, vr, s.preds, s.pred0, s.trans, s.heads, _pos, _fvdelay,
				_lstact, s.gold && bGoldAct, s.nstates);
	}

	List<SRParserState> reduceRight(SRParserState s, boolean bGoldAct, boolean bAdd)
	{
		List<SRParserState> l = new ArrayList<SRParserState>();
		if (s.pstck[0].index != -1 && s.pos[s.pstck[0].index] == null)
			return l;

		List<String> _fvdelay = s.fvdelay != null ? new LinkedList<String>(s.fvdelay) : null;
		IntFeatVector vr = m_fhandler.getFeatures(s, PDAction.REDUCE_RIGHT, _fvdelay, bAdd);
		double sr = m_weight.score(vr);
		if (!bAdd) vr = null;

		for (SRParserState p : s.preds)
		{
			if (p.pstck[0].index == -1 && s.curidx < s.sent.size())
				continue;

			assert (s.trans.containsKey(p));
			Pair<IntFeatVector, Double> t = s.trans.get(p);

			double scdlt = t.second + sr;

			List<IntFeatVector> _fvins = null;
			if (bAdd)
			{
				_fvins = new LinkedList<IntFeatVector>(s.fvins);
				_fvins.addAll(p.fvins);
				_fvins.add(t.first);
				_fvins.add(vr);
			}
			double _scprf = p.scprf + s.scins + scdlt;
			double _scins = p.scins + s.scins + scdlt;

			DepTree[] _pstck = p.cloneStack();
			DepTree h = new DepTree(p.pstck[0]);
			DepTree c = new DepTree(s.pstck[0]);
			_pstck[0] = h;

			h.children.add(c);
			c.head = h.index;

			int[] _heads = Arrays.copyOf(s.heads, s.heads.length);
			for (int i = 0; i < p.heads.length; ++i)
				if (p.heads[i] != -2)
					_heads[i] = p.heads[i];
			_heads[c.index] = h.index;

			String[] _pos = Arrays.copyOf(s.pos, s.pos.length);
			for (int i = 0; i < p.pos.length; ++i)
				if (p.pos[i] != null)
					_pos[i] = p.pos[i];

			List<PDAction> _lstact = new LinkedList<PDAction>(p.lstact);
			_lstact.addAll(s.lstact);
			_lstact.add(PDAction.REDUCE_RIGHT);

			l.add(m_generator.generate(s.sent, _pstck, s.curidx, Math.max(p.idbgn, 0), s.idend,
					_scprf, _scins, scdlt, _fvins, vr, p.preds, p.pred0, p.trans, _heads, _pos, _fvdelay,
					_lstact, s.gold && p.gold && bGoldAct, s.nstates));
		}
		return l;
	}

	List<SRParserState> reduceLeft(SRParserState s, boolean bGoldAct, boolean bAdd)
	{
		List<SRParserState> l = new ArrayList<SRParserState>();
		if (s.pstck[0].index != -1 && s.pos[s.pstck[0].index] == null)
			return l;

		List<String> _fvdelay = s.fvdelay != null ? new LinkedList<String>(s.fvdelay) : null;
		IntFeatVector vr = m_fhandler.getFeatures(s, PDAction.REDUCE_LEFT, _fvdelay, bAdd);
		double sr = m_weight.score(vr);
		if (!bAdd) vr = null;

		for (SRParserState p : s.preds)
		{
			if (p.pstck[0].index == -1) continue;

			Pair<IntFeatVector, Double> t = s.trans.get(p);

			double scdlt = t.second + sr;
			List<IntFeatVector> _fvins = null;
			if (bAdd)
			{
				_fvins = new LinkedList<IntFeatVector>(s.fvins);
				_fvins.addAll(p.fvins);
				_fvins.add(t.first);
				_fvins.add(vr);
			}
			double _scprf = p.scprf + s.scins + scdlt;
			double _scins = p.scins + s.scins + scdlt;

			DepTree[] _pstck = p.cloneStack();
			DepTree h = new DepTree(s.pstck[0]);
			DepTree c = new DepTree(p.pstck[0]);
			_pstck[0] = h;

			h.children.add(c);
			c.head = h.index;

			int[] _heads = Arrays.copyOf(s.heads, s.heads.length);
			for (int i = 0; i < p.heads.length; ++i)
				if (p.heads[i] != -2)
					_heads[i] = p.heads[i];
			_heads[c.index] = h.index;

			String[] _pos = Arrays.copyOf(s.pos, s.pos.length);
			for (int i = 0; i < p.pos.length; ++i)
				if (p.pos[i] != null)
					_pos[i] = p.pos[i];

			List<PDAction> _lstact = new LinkedList<PDAction>(p.lstact);
			_lstact.addAll(s.lstact);
			_lstact.add(PDAction.REDUCE_LEFT);

			l.add(m_generator.generate(s.sent, _pstck, s.curidx, Math.max(p.idbgn, 0), s.idend,
					_scprf, _scins, scdlt, _fvins, vr, p.preds, p.pred0, p.trans, _heads, _pos, _fvdelay,
					_lstact, s.gold && p.gold && bGoldAct, s.nstates));
		}
		return l;
	}

	@Override
	public IntFeatVector getPrefixFeatures(SRParserState s)
	{
		// if (s.fvprf != null) return s.fvprf; // cache
		//
		// IntFeatVector fvprf1 = new IntFeatVector();
		// SRParserState s1 = s;
		// for (SRParserState s2 = s1; s2 != null; s2 = s2.preds.size() > 0 ? s2.pred0 : null)
		// {
		// for (IntFeatVector v : s2.fvins)
		// fvprf1.append(v);
		// if (s2 != s1)
		// fvprf1.append(s1.trans.get(s2).key);
		// s1 = s2;
		// }

		IntFeatVector fvprf2 = new IntFeatVector();
		List<PDAction> lAct = s.getActionSequence();
		SRParserState s3 = m_generator.create(s.sent);
		for (PDAction act : lAct)
			s3 = moveNext(s3, act, false, true).get(0);
		SRParserState sss1 = s3;
		for (SRParserState sss2 = sss1; sss2 != null; sss2 = sss2.pred0)
		{
			for (IntFeatVector v : sss2.fvins)
				fvprf2.append(v);
			if (sss2 != sss1)
				fvprf2.append(sss1.trans.get(sss2).first);
			sss1 = sss2;
		}

		// assert (fvprf1.equals(fvprf2));

		return fvprf2;
		// return fvprf1;
	}

	public boolean isEnd(SRParserState s)
	{
		return s.curidx == s.sent.size()
				&& (m_bParse && s.pstck[1] == null || !m_bParse && s.pos[s.pstck[0].index] != null);
	}

	public static boolean isJustShifted(SRParserState s)
	{
		return s.fvins.size() == 0;
	}

}
