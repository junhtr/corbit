package corbit.segdep;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import corbit.commons.ml.IntFeatVector;
import corbit.commons.transition.SDAction;
import corbit.commons.util.Pair;
import corbit.commons.word.DepChunkTree;
import corbit.commons.word.UnsegmentedSentence;
import corbit.segdep.handler.DelayedFeature;
import corbit.segdep.handler.SRParserHandler;
import corbit.segdep.transition.SRParserTransition;

public class SRParserStateGenerator
{
	private static final int m_szStack = 3;

	private final SRParserHandler m_fhandler;

	private final boolean m_bDP;
	private final boolean m_bEvalDelay;

	public SRParserStateGenerator(SRParserHandler fhandler, boolean bDP, boolean bEvalDelay)
	{
		m_fhandler = fhandler;
		m_bDP = bDP;
		m_bEvalDelay = bEvalDelay;
	}

	public SRParserState create(UnsegmentedSentence sent, SRParserTransition.Decision[] decision)
	{
		SRParserState s = m_bDP ?
				new SRParserDPState(
						sent,
						new DepChunkTree[m_szStack],
						0, 0, -1, -1, 0.0d, 0.0d,
						new LinkedList<IntFeatVector>(),
						m_bEvalDelay ? new LinkedList<DelayedFeature>() : null, new TreeSet<SRParserState>(),
						null,
						new LinkedHashMap<SRParserState,Pair<List<IntFeatVector>,Double>>(),
						decision,
						new LinkedList<SDAction>(),
						true,
						1) :
				new SRParserState(
						sent,
						new DepChunkTree[m_szStack],
						0, 0, -1, -1, 0.0d, 0.0d,
						new LinkedList<IntFeatVector>(),
						m_bEvalDelay ? new LinkedList<DelayedFeature>() : null, new TreeSet<SRParserState>(),
						null,
						new LinkedHashMap<SRParserState,Pair<List<IntFeatVector>,Double>>(),
						decision,
						new LinkedList<SDAction>(),
						true, 1);

		s.pstck[0] = new DepChunkTree(s.sent, -1, -1, DepChunkTree.rootTag, -2, -2, null);
//		for (int i = 0; i < sent.numChunks(); ++i)
//		{
//			// s.pos[i] = sent.get(i).pos;
//			s.heads[i] = sent.getChunk(i).head;
//		}
		s.calcAtomicFeatures(m_fhandler);
		return s;
	}

	SRParserState copy(SRParserState s)
	{
		SRParserState sNew = m_bDP ?
				new SRParserDPState(
						s.sent,
						s.cloneStack(),
						s.curidx, s.curstep, s.idbgn, s.idend, s.scprf, s.scins,
						s.fvins != null ? new LinkedList<IntFeatVector>(s.fvins) : null,
						m_bEvalDelay ? new LinkedList<DelayedFeature>(s.fvdelay) : null, new TreeSet<SRParserState>(s.preds),
						s.pred0,
						new LinkedHashMap<SRParserState,Pair<List<IntFeatVector>,Double>>(s.trans),
						s.decision,
						new LinkedList<SDAction>(s.lstact), s.gold,
						s.nstates) :
				new SRParserState(
						s.sent,
						s.cloneStack(),
						s.curidx, s.curstep, s.idbgn, s.idend, s.scprf, s.scins,
						s.fvins != null ? new LinkedList<IntFeatVector>(s.fvins) : null,
						m_bEvalDelay ? new LinkedList<DelayedFeature>(s.fvdelay) : null, new TreeSet<SRParserState>(s.preds),
						s.pred0,
						new LinkedHashMap<SRParserState,Pair<List<IntFeatVector>,Double>>(s.trans),
						s.decision,
						new LinkedList<SDAction>(s.lstact),
						s.gold,
						s.nstates);

		sNew.atoms = s.atoms;
		return sNew;
	}

	public SRParserState generate(
			UnsegmentedSentence sent,
			DepChunkTree[] stack,
			int curidx,
			int curstep,
			int idbgn,
			int idend,
			double scprf,
			double scins,
			List<IntFeatVector> fvins,
			List<DelayedFeature> fvdelay,
			Set<SRParserState> preds,
			SRParserState pred0,
			Map<SRParserState,Pair<List<IntFeatVector>,Double>> trans,
			SRParserTransition.Decision[] decision,
			List<SDAction> lstact,
			boolean gold,
			long states)
	{
		SRParserState s = m_bDP ?
				new SRParserDPState(
						sent, stack,
						curidx, curstep, idbgn, idend, scprf, scins,
						fvins, fvdelay, preds, pred0, trans, decision, lstact, gold, states) :
				new SRParserState(
						sent, stack,
						curidx, curstep, idbgn, idend, scprf, scins,
						fvins, fvdelay, preds, pred0, trans, decision, lstact, gold, states);
		s.calcAtomicFeatures(m_fhandler);
		return s;
	}

	public SRParserState merge(SRParserState ps1, SRParserState ps2)
	{
		assert (ps1.atoms.equals(ps2.atoms));

		if (SRParserState.isBetter(ps2, ps1))
		{
			SRParserState _ps = ps1;
			ps1 = ps2;
			ps2 = _ps;
		}

		SRParserState ps = copy(ps1);

		for (SRParserState _ps: ps2.preds)
			if (!ps.preds.contains(_ps))
				ps.preds.add(_ps);

		for (Entry<SRParserState,Pair<List<IntFeatVector>,Double>> p: ps2.trans.entrySet())
		{
			SRParserState sk = p.getKey();
			if (!ps.trans.containsKey(sk))
				ps.trans.put(sk, p.getValue());
		}

		ps.gold = ps1.gold || ps2.gold && ps1.pstck[0].equals(ps2.pstck[0]);

		ps.nstates = ps1.nstates + ps2.nstates;

		return ps;
	}

}
