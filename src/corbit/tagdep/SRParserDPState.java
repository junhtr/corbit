package corbit.tagdep;

import java.util.List;
import java.util.Map;
import java.util.Set;

import corbit.commons.ml.IntFeatVector;
import corbit.commons.transition.PDAction;
import corbit.commons.util.Pair;
import corbit.tagdep.word.DepTree;
import corbit.tagdep.word.DepTreeSentence;

public class SRParserDPState extends SRParserState
{
	public SRParserDPState(DepTreeSentence sent, DepTree[] stack, int curidx, int idbgn, int idend, double scprf, double scins, double scdlt, List<IntFeatVector> fvins, IntFeatVector fvdlt,
			Set<SRParserState> preds, SRParserState pred0, Map<SRParserState, Pair<IntFeatVector, Double>> trans, int[] heads, String[] pos, List<String> fvdelay, List<PDAction> lstact, boolean gold,
			long states)
	{
		super(sent, stack, curidx, idbgn, idend, scprf, scins, scdlt, fvins, fvdlt, preds, pred0, trans, heads, pos, fvdelay, lstact, gold, states);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof SRParserState)) return false;
		return this.atoms.equals(((SRParserState)obj).atoms);
	}

	@Override
	public int hashCode()
	{
		return atoms.hashCode();
	}
}
