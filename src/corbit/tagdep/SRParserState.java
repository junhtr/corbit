package corbit.tagdep;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import corbit.commons.ml.IntFeatVector;
import corbit.commons.transition.PDAction;
import corbit.commons.util.Pair;
import corbit.tagdep.handler.AtomicFeatures;
import corbit.tagdep.handler.SRParserHandler;
import corbit.tagdep.word.DepTree;
import corbit.tagdep.word.DepTreeSentence;

/**
 * This class should be treated as a struct consisting only of (arrays of) primitive values and some pointers.
 */
public class SRParserState implements Comparable<SRParserState>
{
	public DepTreeSentence sent; // original sentence (i.e. input queue)
	public DepTree[] pstck; // partial stack
	public int curidx; // current index of the input queue
	public int idbgn; // beginning of the tree span
	public int idend; // end of the tree span
	public double scprf; // prefix score
	public double scins; // inside score
	public double scdlt; // delta score
	public IntFeatVector fvprf; // prefix feature vector
	public List<IntFeatVector> fvins; // inside feature vector
	public IntFeatVector fvdlt; // delta feature vector
	public Set<SRParserState> preds; // predictor states

	public SRParserState pred0; // predictor state with the highest prefix score
	public Map<SRParserState, Pair<IntFeatVector, Double>> trans; // transitions from predictor states; work as a cache

	public AtomicFeatures atoms;

	public int[] heads;
	public String[] pos;

	public List<String> fvdelay;

	public double scbonus;

	public final List<PDAction> lstact; // inside action sequence

	public boolean gold; // if the state contains the gold derivation
	public long nstates; // the number of packed instances

	public SRParserState(
			DepTreeSentence sent,
			DepTree[] stack,
			int curidx,
			int idbgn,
			int idend,
			double scprf,
			double scins,
			double scdlt,
			List<IntFeatVector> fvins,
			IntFeatVector fvdlt,
			Set<SRParserState> preds,
			SRParserState pred0,
			Map<SRParserState, Pair<IntFeatVector, Double>> trans,
			int[] heads,
			String[] pos,
			List<String> fvdelay,
			List<PDAction> lstact,
			boolean gold,
			long states)
	{
		this.sent = sent;
		this.pstck = stack;
		this.curidx = curidx;
		this.idbgn = idbgn;
		this.idend = idend;
		this.scprf = scprf;
		this.scins = scins;
		this.scdlt = scdlt;
		this.fvprf = null;
		this.fvins = fvins;
		this.fvdlt = fvdlt;
		this.pred0 = pred0;
		this.preds = preds;
		this.heads = heads;
		this.pos = pos;
		this.fvdelay = fvdelay;
		this.scbonus = 0.0d;
		this.lstact = lstact;
		this.trans = trans;
		this.gold = gold;
		this.nstates = states;
		this.atoms = null;
	}

	public DepTree[] pushStack(DepTree t)
	{
		DepTree[] _pstck = new DepTree[pstck.length];
		for (int i = 0; i < pstck.length - 1; ++i)
			_pstck[i + 1] = pstck[i];
		_pstck[0] = t;
		return _pstck;
	}

	public DepTree[] cloneStack()
	{
		DepTree[] _pstck = new DepTree[pstck.length];
		for (int i = 0; i < pstck.length; ++i)
			_pstck[i] = (i == 0 && pstck[i] != null) ? new DepTree(pstck[i]) : pstck[i];
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

	public List<PDAction> getActionSequence()
	{
		return getActionSequence(this);
	}

	public static List<PDAction> getActionSequence(SRParserState s)
	{
		List<PDAction> l = new LinkedList<PDAction>();
		for (; s != null; s = s.pred0)
			l.addAll(0, s.lstact);
		return l;
	}

	public static DepTreeSentence getParsedResult(SRParserState s)
	{
		DepTreeSentence sent = new DepTreeSentence();

		for (int i = 0; i < s.sent.size(); ++i)
			sent.add(new DepTree(sent, i, s.sent.get(i).form, null, -2));

		List<DepTree> lc = new ArrayList<DepTree>();
		List<DepTree> _lc = new ArrayList<DepTree>();

		for (SRParserState _s = s; _s != null; _s = _s.preds.size() > 0 ? _s.pred0 : null)
		{
			// restore the parsed sentence

			lc.add(_s.pstck[0]);
			while (lc.size() > 0)
			{
				for (DepTree c : lc)
				{
					_lc.addAll(c.children);

					int idx = c.index;
					if (idx == -1)
						continue;

					DepTree seg = sent.get(idx);
					seg.head = _s.heads[idx];
					seg.pos = _s.pos[idx];

					// if (seg.head == -1) sent.Root = idx;
				}
				List<DepTree> __lc = lc;
				lc = _lc;
				_lc = __lc;

				_lc.clear();
			}
			lc.clear();
		}

		return sent;
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
		for (int i = curidx; i < sent.size(); ++i)
			sb.append(sent.get(i).toString() + " ");
		return sb.toString();
	}

	public String stackToString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.gold ? "[T] " : "[F] ");
		for (int i = pstck.length - 1; i >= 0; --i)
			if (pstck[i] != null)
				sb.append(pstck[i].toString() + " ");
		sb.append(": ");
		sb.append(atoms.fvdelay);
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
