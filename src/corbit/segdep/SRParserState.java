package corbit.segdep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import corbit.commons.ml.IntFeatVector;
import corbit.commons.transition.SDAction;
import corbit.commons.util.Pair;
import corbit.commons.word.DepChunk;
import corbit.commons.word.DepChunkTree;
import corbit.commons.word.ParsedSentence;
import corbit.commons.word.SentenceBuilder;
import corbit.commons.word.UnsegmentedSentence;
import corbit.segdep.handler.DelayedFeature;
import corbit.segdep.handler.SRParserHandler;
import corbit.segdep.transition.SRParserTransition;

/**
 * This class should be treated like a struct in c consisting only of primitive
 * values and some pointers.
 */
public class SRParserState implements Comparable<SRParserState>
{
	/** original sentence (i.e. input queue) */
	public final UnsegmentedSentence sent;
	/** partial stack */
	public final DepChunkTree[] pstck;
	/** current index of the input queue */
	public final int curidx;
	/** current step of the beam */
	public final int curstep;
	/** beginning index of the tree span (inclusive) */
	public final int idbgn;
	/** ending index of the tree span (exclusive) */
	public final int idend;

	/** prefix score */
	public final double scprf;
	/** inside score */
	public final double scins;

	/** inside feature vector */
	public final List<IntFeatVector> fvins;
	/** delayed feature vector */
	public final List<DelayedFeature> fvdelay;

	/** predictor states */
	public final Set<SRParserState> preds;
	/** predictor state with the highest prefix score */
	public final SRParserState pred0;
	/** transition score and vector cache from its predictor states */
	public final Map<SRParserState,Pair<List<IntFeatVector>,Double>> trans;
	/** preprocessing and pruning decisions forced during decoding */
	public final SRParserTransition.Decision[] decision;

	/** atomic (kernel) features */
	public SRParserHandler.AtomicFeatures atoms;

	/** inside action sequence */
	public final List<SDAction> lstact;

	/** whether or not the state contains the gold derivation in its graph-structured stack*/
	public boolean gold;
	/** the total number of packed derivations */
	public long nstates;

	SRParserState(
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
		this.sent = sent;
		this.pstck = stack;
		this.curidx = curidx;
		this.curstep = curstep;
		this.idbgn = idbgn;
		this.idend = idend;
		this.scprf = scprf;
		this.scins = scins;
		this.fvins = fvins;
		this.pred0 = pred0;
		this.preds = preds;
		this.fvdelay = fvdelay;
		this.lstact = lstact;
		this.trans = trans;
		this.decision = decision;
		this.gold = gold;
		this.nstates = states;
		this.atoms = null;
	}

	public DepChunkTree[] pushStack(DepChunkTree t)
	{
		DepChunkTree[] _pstck = new DepChunkTree[pstck.length];
		for (int i = 0; i < pstck.length - 1; ++i)
			_pstck[i + 1] = pstck[i];
		_pstck[0] = t;
		return _pstck;
	}

	public DepChunkTree[] cloneStack()
	{
		DepChunkTree[] _pstck = new DepChunkTree[pstck.length];
		for (int i = 0; i < pstck.length; ++i)
			_pstck[i] = (i == 0 && pstck[i] != null) ? new DepChunkTree(pstck[i]) : pstck[i];
		return _pstck;
	}

	public void calcAtomicFeatures(SRParserHandler handler)
	{
		atoms = handler.getAtomicFeatures(this);
	}

	public static boolean isBetter(SRParserState s1, SRParserState s2)
	{
		return (s1.scprf > s2.scprf || s1.scprf == s2.scprf && s1.scins > s2.scins);
	}

	public List<SDAction> getActionSequence()
	{
		return getActionSequence(this);
	}

	public static List<SDAction> getActionSequence(SRParserState s)
	{
		List<SDAction> l = new LinkedList<SDAction>();
		for (; s != null; s = s.pred0)
			l.addAll(0, s.lstact);
		return l;
	}

	public static ParsedSentence getParsedResult(SRParserState s)
	{
		List<DepChunk> chunks = new ArrayList<DepChunk>();

		List<DepChunkTree> lc = new ArrayList<DepChunkTree>();
		List<DepChunkTree> _lc = new ArrayList<DepChunkTree>();

		for (SRParserState _s = s; _s != null; _s = _s.preds.size() > 0 ? _s.pred0 : null)
		{
			lc.add(_s.pstck[0]);
			while (lc.size() > 0)
			{
				for (DepChunkTree c: lc)
				{
					if (!c.isRoot())
						chunks.add(new DepChunk(c));
					_lc.addAll(c.children);
				}
				List<DepChunkTree> __lc = lc;
				lc = _lc;
				_lc = __lc;

				_lc.clear();
			}
			lc.clear();
		}

		Collections.sort(chunks, new Comparator<DepChunk>() {
			public int compare(DepChunk o1, DepChunk o2)
			{
				return o1.begin - o2.begin;
			}
		});

		return SentenceBuilder.chunkedToParsedSentence(chunks, s.sent);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.gold ? "[T] " : "[F] ");
		for (int i = pstck.length - 1; i >= 0; --i)
			if (pstck[i] != null)
				sb.append(pstck[i].toString() + " ");
		sb.append(": ");
		sb.append(sent.substring(curidx, sent.length()));
		return sb.toString();
	}

	public String stackToString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.gold ? "[T] " : "[F] ");
		for (int i = pstck.length - 1; i >= 0; --i)
			if (pstck[i] != null)
				sb.append(pstck[i].toString() + " ");
//		sb.append(": ");
//		sb.append(atoms.fvdelay);
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj)
	{
		return (this == obj);
	}

	@Override
	public int hashCode()
	{
		return super.hashCode();
	}

	@Override
	public int compareTo(SRParserState o)
	{
		int h1 = hashCode();
		int h2 = o.hashCode();
		return h1 > h2 ? 1 : h1 == h2 ? 0 : -1;
	}
}
