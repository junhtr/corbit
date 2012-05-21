package corbit.segdep;

import java.util.List;
import java.util.Map;
import java.util.Set;

import corbit.commons.ml.IntFeatVector;
import corbit.commons.transition.SDAction;
import corbit.commons.util.Pair;
import corbit.commons.word.DepChunkTree;
import corbit.commons.word.UnsegmentedSentence;
import corbit.segdep.handler.DelayedFeature;
import corbit.segdep.transition.SRParserTransition;

class SRParserDPState extends SRParserState
{
	SRParserDPState(
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
		super(sent, stack, curidx, curstep, idbgn, idend, scprf, scins, fvins, fvdelay, preds, pred0, trans, decision, lstact, gold, states);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			throw new NullPointerException();
		if (!(obj instanceof SRParserState))
			throw new ClassCastException();
		return this.atoms.equals(((SRParserState)obj).atoms);
	}

	@Override
	public int hashCode()
	{
		return atoms.hashCode();
	}
}
