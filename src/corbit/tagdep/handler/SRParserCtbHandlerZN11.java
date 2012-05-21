package corbit.tagdep.handler;

import java.util.List;
import java.util.TreeSet;

import corbit.commons.Vocab;
import corbit.commons.ml.IntFeatVector;
import corbit.commons.transition.PDAction;
import corbit.tagdep.SRParserParameters;
import corbit.tagdep.SRParserState;
import corbit.tagdep.word.DepTree;

/**
 * Parsing features from Zhang and Nivre (2011)
 */
public class SRParserCtbHandlerZN11 extends SRParserHandler
{
	class AtomsZN11 extends AtomicFeatures
	{
		static final int F_curidx = 0;
		static final int F_span_bgn = 1;
		static final int F_span_end = 2;
		static final int F_idx_st0 = 3;
		static final int F_head_st0 = 4;
		static final int F_idx_st1 = 5;
		static final int F_f_st0 = 6;
		static final int F_f_st1 = 7;
		static final int F_f_qp1 = 8;
		static final int F_f_qf1 = 9;
		static final int F_p_st0 = 10;
		static final int F_p_st1 = 11;
		static final int F_p_st2 = 12;
		static final int F_p_qp2 = 13;
		static final int F_p_qp1 = 14;
		static final int F_p_qf1 = 15;
		static final int F_p_qf2 = 16;
		static final int F_p_st0rc = 17;
		static final int F_p_st0lc = 18;
		static final int F_p_st1rc = 19;
		static final int F_p_st1lc = 20;
		static final int F_punct = 21;
		static final int F_adjoin = 22;
		static final int F_npos = 23;

		static final int F_dist = 24;
		static final int F_val0l = 25;
		static final int F_val1l = 26;
		static final int F_val1r = 27;
		static final int F_f_st0lc = 28;
		static final int F_f_st1lc = 29;
		static final int F_f_st1rc = 30;
		static final int F_f_st0lc2 = 31;
		static final int F_f_st1lc2 = 32;
		static final int F_f_st1rc2 = 33;
		static final int F_p_st0lc2 = 34;
		static final int F_p_st1lc2 = 35;
		static final int F_p_st1rc2 = 36;
		
		static final int NUM_FEATURE = 37;

		AtomsZN11(
				int curidx,
				int span_bgn,
				int span_end,
				int idx_st0,
				int head_st0,
				int idx_st1,
				String f_st0,
				String f_st1,
				String f_qp1,
				String f_qf1,
				String p_st0,
				String p_st1,
				String p_st2,
				String p_qp2,
				String p_qp1,
				String p_qf1,
				String p_qf2,
				String p_st0rc,
				String p_st0lc,
				String p_st1rc,
				String p_st1lc,
				String punct,
				boolean adjoin,
				String npos,
				int dist,
				int val0l,
				int val1l,
				int val1r,
				String f_st0lc,
				String f_st1lc,
				String f_st1rc,
				String f_st0lc2,
				String f_st1lc2,
				String f_st1rc2,
				String p_st0lc2,
				String p_st1lc2,
				String p_st1rc2,
				TreeSet<String> fvdelay)
		{
			super(NUM_FEATURE, fvdelay);
			features[F_curidx] = Integer.toString(curidx);
			features[F_span_bgn] = Integer.toString(span_bgn);
			features[F_span_end] = Integer.toString(span_end);
			features[F_idx_st0] = Integer.toString(idx_st0);
			features[F_head_st0] = Integer.toString(head_st0);
			features[F_idx_st1] = Integer.toString(idx_st1);
			features[F_f_st0] = f_st0;
			features[F_f_st1] = f_st1;
			features[F_f_qp1] = f_qp1;
			features[F_f_qf1] = f_qf1;
			features[F_p_st0] = p_st0;
			features[F_p_st1] = p_st1;
			features[F_p_st2] = p_st2;
			features[F_p_qp2] = p_qp2;
			features[F_p_qp1] = p_qp1;
			features[F_p_qf1] = p_qf1;
			features[F_p_qf2] = p_qf2;
			features[F_p_st0rc] = p_st0rc;
			features[F_p_st0lc] = p_st0lc;
			features[F_p_st1rc] = p_st1rc;
			features[F_p_st1lc] = p_st1lc;
			features[F_punct] = punct;
			features[F_adjoin] = Boolean.toString(adjoin);
			features[F_npos] = npos;
			features[F_dist] = Integer.toString(dist);
			features[F_val0l] = Integer.toString(val0l);
			features[F_val1l] = Integer.toString(val1l);
			features[F_val1r] = Integer.toString(val1r);
			features[F_f_st0lc] = f_st0lc;
			features[F_f_st1lc] = f_st1lc;
			features[F_f_st1rc] = f_st1rc;
			features[F_f_st0lc2] = f_st0lc2;
			features[F_f_st1lc2] = f_st1lc2;
			features[F_f_st1rc2] = f_st1rc2;
			features[F_p_st0lc2] = p_st0lc2;
			features[F_p_st1lc2] = p_st1lc2;
			features[F_p_st1rc2] = p_st1rc2;
			setHash();
		}

	}

	public SRParserCtbHandlerZN11(Vocab v, SRParserParameters params)
	{
		super(v, params);
	}

	@Override
	public AtomicFeatures getAtomicFeatures(SRParserState s0)
	{
		SRParserState s1 = s0.preds.size() > 0 ? s0.pred0 : null;

		DepTree wst0 = s0.pstck[0];
		DepTree wst1 = s0.pstck[1];
		DepTree wst2 = s0.pstck[2];
		
		DepTree[] wst0rc = wst0 != null ? getTwoRightmostChild(wst0) : null;
		DepTree[] wst0lc = wst0 != null ? getTwoLeftmostChild(wst0) : null;
		DepTree[] wst1rc = wst1 != null ? getTwoRightmostChild(wst1) : null;
		DepTree[] wst1lc = wst1 != null ? getTwoLeftmostChild(wst1) : null;
		
		DepTree wst0rc1 = wst0rc != null ? wst0rc[0] : null;
//		DepTree wst0rc2 = wst0rc != null ? wst0rc[1] : null;
		DepTree wst0lc1 = wst0lc != null ? wst0lc[0] : null;
		DepTree wst0lc2 = wst0lc != null ? wst0lc[1] : null;
		DepTree wst1rc1 = wst1rc != null ? wst1rc[0] : null;
		DepTree wst1rc2 = wst1rc != null ? wst1rc[1] : null;
		DepTree wst1lc1 = wst1lc != null ? wst1lc[0] : null;
		DepTree wst1lc2 = wst1lc != null ? wst1lc[1] : null;

		int idx = s0.curidx;

		String sfst0 = wst0.form;
		String sfst1 = wst1 != null ? wst1.form : OOR;
		String sfqp1 = idx > 0 ? s0.sent.get(idx - 1).form : OOR;
		String sfqf1 = idx < s0.sent.size() ? s0.sent.get(idx).form : OOR;

		String spst0 = wst0.pos;
		String spst1 = wst1 != null ? wst1.pos : OOR;
		String spst2 = wst2 != null ? wst2.pos : OOR;
		String spqp1 = idx > 0 ? s0.pos[idx - 1] : OOR;
		String spqp2 = idx > 1 ? (s0.idbgn <= idx - 2) ? s0.pos[idx - 2] : s1.pos[idx - 2] : OOR;
		String spqf1 = idx < s0.sent.size() ? s0.sent.get(idx).pos : OOR;
		String spqf2 = idx < s0.sent.size() - 1 ? s0.sent.get(idx + 1).pos : OOR;
		String spst0rc1 = wst0rc1 != null ? s0.pos[wst0rc1.index] : OOR;
		String spst0lc1 = wst0lc1 != null ? s0.pos[wst0lc1.index] : OOR;
		String spst1rc1 = wst1rc1 != null ? s1.pos[wst1rc1.index] : OOR;
		String spst1lc1 = wst1lc1 != null ? s1.pos[wst1lc1.index] : OOR;
		
		String sfst0lc1 = wst0lc1 != null ? wst0lc1.form : OOR;
		String sfst1lc1 = wst1lc1 != null ? wst1lc1.form : OOR;
		String sfst1rc1 = wst1rc1 != null ? wst1rc1.form : OOR;
		String sfst0lc2 = wst0lc2 != null ? wst0lc2.form : OOR;
		String sfst1lc2 = wst1lc2 != null ? wst1lc2.form : OOR;
		String sfst1rc2 = wst1rc2 != null ? wst1rc2.form : OOR;
		String spst0lc2 = wst0lc2 != null ? s0.pos[wst0lc2.index] : OOR;
		String spst1lc2 = wst1lc2 != null ? s1.pos[wst1lc2.index] : OOR;
		String spst1rc2 = wst1rc2 != null ? s1.pos[wst1rc2.index] : OOR;

		int dist = (wst0 != null && wst1 != null) ? getDistance(wst0, wst1) : -2;
		int[] val0 = wst0 != null ? getValencies(wst0) : null;
		int[] val1 = wst1 != null ? getValencies(wst1) : null;
		int val0l = val0 != null ? val0[0] : -1;
		int val1l = val1 != null ? val1[0] : -1;
		int val1r = val1 != null ? val1[1] : -1;
		
		String sPunct = (wst0 != null && wst1 != null) ? getPunctInBetween(s0.sent, wst0.index, wst1.index) : OOR;
		boolean bAdjoin = (wst0 != null && wst1 != null && Math.abs(wst1.index - wst0.index) == 1);

		// debugging assertions
		
		assert (spst0 != null);
		assert (spst1 != null);
		assert (spst2 != null);
		assert (spqp1 != null);
		assert (spqp2 != null);
		assert (spst0rc1 != null);
		assert (spst0lc1 != null);
		assert (spst1rc1 != null);
		assert (spst1lc1 != null);
		assert (spst0lc2 != null);
		assert (spst1rc2 != null);
		assert (spst1lc2 != null);

		// ad-hoc modification

		return new AtomsZN11(
				s0.curidx,
				s0.idbgn,
				s0.idend,
				wst0.index,
				wst0.index >= 0 ? s0.heads[wst0.index] : -2,
				wst1 != null ? wst1.index : -2,
				sfst0,
				sfst1,
				sfqp1,
				sfqf1,
				spst0,
				spst1,
				spst2,
				spqp2,
				spqp1,
				spqf1,
				spqf2,
				spst0rc1,
				spst0lc1,
				spst1rc1,
				spst1lc1,
				sPunct,
				bAdjoin,
				OOR,
				dist,
				val0l,
				val1l,
				val1r,
				sfst0lc1,
				sfst1lc1,
				sfst1rc1,
				sfst0lc2,
				sfst1lc2,
				sfst1rc2,
				spst0lc2,
				spst1lc2,
				spst1rc2,
				s0.fvdelay != null ? new TreeSet<String>(s0.fvdelay) : null);
	}

	@Override
	public IntFeatVector getFeatures(SRParserState s0, PDAction act, List<String> vd, boolean bAdd)
	{
		IntFeatVector v = new IntFeatVector();

		String sfst0 = s0.atoms.get(AtomsZN11.F_f_st0);
		String sfst1 = s0.atoms.get(AtomsZN11.F_f_st1);
		String sfqp1 = s0.atoms.get(AtomsZN11.F_f_qp1);
		String sfqf1 = s0.atoms.get(AtomsZN11.F_f_qf1);
		String spst0 = s0.atoms.get(AtomsZN11.F_p_st0);
		String spst1 = s0.atoms.get(AtomsZN11.F_p_st1);
		String spst2 = s0.atoms.get(AtomsZN11.F_p_st2);
		String spqp1 = s0.atoms.get(AtomsZN11.F_p_qp1);
		String spqp2 = s0.atoms.get(AtomsZN11.F_p_qp2);
		String spqf1 = s0.atoms.get(AtomsZN11.F_p_qf1);
		String spqf2 = s0.atoms.get(AtomsZN11.F_p_qf2);
		String spst0rc = s0.atoms.get(AtomsZN11.F_p_st0rc);
		String spst0lc = s0.atoms.get(AtomsZN11.F_p_st0lc);
		String spst1rc = s0.atoms.get(AtomsZN11.F_p_st1rc);
		String spst1lc = s0.atoms.get(AtomsZN11.F_p_st1lc);
		String sPunct = s0.atoms.get(AtomsZN11.F_punct);
		String sAdjoin = s0.atoms.get(AtomsZN11.F_adjoin);

		String sDist = s0.atoms.get(AtomsZN11.F_dist);
		String sVal0l = s0.atoms.get(AtomsZN11.F_val0l);
		String sVal1l = s0.atoms.get(AtomsZN11.F_val1l);
		String sVal1r = s0.atoms.get(AtomsZN11.F_val1r);
		String sfst0lc = s0.atoms.get(AtomsZN11.F_f_st0lc);
		String sfst1lc = s0.atoms.get(AtomsZN11.F_f_st1lc);
		String sfst1rc = s0.atoms.get(AtomsZN11.F_f_st1rc);
		String sfst0lc2 = s0.atoms.get(AtomsZN11.F_f_st0lc2);
		String sfst1lc2 = s0.atoms.get(AtomsZN11.F_f_st1lc2);
		String sfst1rc2 = s0.atoms.get(AtomsZN11.F_f_st1rc2);
		String spst0lc2 = s0.atoms.get(AtomsZN11.F_p_st0lc2);
		String spst1lc2 = s0.atoms.get(AtomsZN11.F_p_st1lc2);
		String spst1rc2 = s0.atoms.get(AtomsZN11.F_p_st1rc2);

		int curidx = s0.curidx;
		final int szSent = s0.sent.size();

		String sfqf2 = s0.curidx < s0.sent.size() - 1 ? s0.sent.get(s0.curidx + 1).form : OOR;
		String sAct = act.toString();

		if (act.isShiftPosAction()) spqf1 = act.getPos();
		else if (act.isPosAction()) throw new UnsupportedOperationException();
		
		/*
		 *  parsing features
		 */
		
		if (act == PDAction.REDUCE_LEFT || act == PDAction.REDUCE_RIGHT || act == PDAction.SHIFT || act.isShiftPosAction())
		{
			SRParserCtbHandlerHS10.setParseFeaturesHS10(v, vd, m_vocab, bAdd, sAct, sfst0, sfst1, sfqf1, spst0, spst1, spst2, spqf1, spqf2, spst0rc, spst0lc, spst1rc, spst1lc, sPunct, sAdjoin, curidx, szSent, m_params.m_bUseLookAhead);
			SRParserCtbHandlerZN11.setParseFeaturesZN11(v, m_vocab, sAct, bAdd, sfst0, sfst1, spst0, spst1, spst0lc, spst1rc, spst1lc, sDist, sVal0l, sVal1l, sVal1r, sfst0lc, sfst1lc, sfst1rc, sfst0lc2, sfst1lc2, sfst1rc2,
					spst0lc2, spst1lc2, spst1rc2);
		}

		/*
		 *  evaluate delayed features
		 */
		
		if (vd != null && (act.isPosAction() || act.isShiftPosAction()))
		{
			evaluateDelayedFeatures(v, vd, curidx + 1, spqf1, bAdd);
			assert (curidx + 1 < szSent || vd.size() == 0);
		}
		
		/*
		 *  evaluate tagging features
		 */
		
		if (m_params.m_bUseTagFeature && act.isShiftPosAction())
		{
			SRParserCtbHandlerZC08.setTagFeaturesZC08(v, m_vocab, m_dict, bAdd, sAct, sfqp1, sfqf1, sfqf2, spqp1, spqp2);
			if (m_params.m_bUseSyntax)
				SRParserCtbHandlerHS10.setTagSyntacticFeatures(v, m_vocab, bAdd, sAct, sfst0, sfqf1, spst0, spst1, spst0lc);
		}

		return v;
	}

	/* features described in Zhang and Clark (2011) */
	
	static void setParseFeaturesZN11(
			IntFeatVector v,
			Vocab vocab,
			String sAct,
			boolean bAdd,
			String sfst0, String sfst1, String spst0, String spst1,
			String spst0lc, String spst1rc, String spst1lc, String sDist,
			String sVal0l, String sVal1l, String sVal1r,
			String sfst0lc, String sfst1lc, String sfst1rc, String sfst0lc2,
			String sfst1lc2, String sfst1rc2, String spst0lc2, String spst1lc2,
			String spst1rc2)
	{
		// distance
		addFeature(v, "FP20-" + sfst1 + SEP + sDist, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP21-" + spst1 + SEP + sDist, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP22-" + sfst0 + SEP + sDist, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP23-" + spst0 + SEP + sDist, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP24-" + sfst1 + SEP + sfst0 + SEP + sDist, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP25-" + spst1 + SEP + spst0 + SEP + sDist, sAct, 1.0, bAdd, vocab);
		
		// valency
		addFeature(v, "FP30-" + sfst1 + SEP + sVal1r, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP31-" + spst1 + SEP + sVal1r, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP32-" + sfst1 + SEP + sVal1l, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP33-" + spst1 + SEP + sVal1l, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP34-" + sfst0 + SEP + sVal0l, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP35-" + spst0 + SEP + sVal0l, sAct, 1.0, bAdd, vocab);
		
		// unigrams
		addFeature(v, "FP40-" + sfst1lc, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP41-" + spst1lc, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP42-" + sfst1rc, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP43-" + spst1rc, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP44-" + sfst0lc, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP45-" + spst0lc, sAct, 1.0, bAdd, vocab);
		
		// third-order
		addFeature(v, "FP50-" + sfst1rc2, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP51-" + spst1rc2, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP52-" + sfst1lc2, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP53-" + spst1lc2, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP54-" + sfst0lc2, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP55-" + spst0lc2, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP56-" + spst1 + SEP + spst1lc + SEP + spst1lc2, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP57-" + spst1 + SEP + spst1rc + SEP + spst1rc2, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP58-" + spst0 + SEP + spst0lc + SEP + spst0lc2, sAct, 1.0, bAdd, vocab);
	}

}
