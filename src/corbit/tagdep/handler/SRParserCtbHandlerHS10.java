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
 * Dependency parsing features from Huang and Sagae (2010)
 */
public class SRParserCtbHandlerHS10 extends SRParserHandler
{
	class AtomsHS10 extends AtomicFeatures
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

		static final int NUM_FEATURE = 24;

		AtomsHS10(
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
				TreeSet<String> fvdelay)
		{
			super(NUM_FEATURE, fvdelay);
			features = new String[NUM_FEATURE];
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
			setHash();
		}
	}

	public SRParserCtbHandlerHS10(Vocab v, SRParserParameters params)
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
		
		DepTree wst0rc = wst0 != null ? getRightmostChild(wst0) : null;
		DepTree wst0lc = wst0 != null ? getLeftmostChild(wst0) : null;
		DepTree wst1rc = wst1 != null ? getRightmostChild(wst1) : null;
		DepTree wst1lc = wst1 != null ? getLeftmostChild(wst1) : null;

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
		String spst0rc = wst0rc != null ? s0.pos[wst0rc.index] : OOR;
		String spst0lc = wst0lc != null ? s0.pos[wst0lc.index] : OOR;
		String spst1rc = wst1rc != null ? s1.pos[wst1rc.index] : OOR;
		String spst1lc = wst1lc != null ? s1.pos[wst1lc.index] : OOR;

		String sPunct = (wst0 != null && wst1 != null) ? getPunctInBetween(s0.sent, wst0.index, wst1.index) : OOR;
		boolean bAdjoin = (wst0 != null && wst1 != null && Math.abs(wst1.index - wst0.index) == 1);

		// debugging assertions
		
		assert (spst0 != null);
		assert (spst1 != null);
		assert (spst2 != null);
		assert (spqp1 != null);
		assert (spqp2 != null);
		assert (spst0rc != null);
		assert (spst0lc != null);
		assert (spst1rc != null);
		assert (spst1lc != null);

		// ad-hoc modification

		return new AtomsHS10(
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
				spst0rc,
				spst0lc,
				spst1rc,
				spst1lc,
				sPunct,
				bAdjoin,
				OOR,
				s0.fvdelay != null ? new TreeSet<String>(s0.fvdelay) : null);
	}

	@Override
	public IntFeatVector getFeatures(SRParserState s0, PDAction act, List<String> vd, boolean bAdd)
	{
		IntFeatVector v = new IntFeatVector();

		String sfst0 = s0.atoms.get(AtomsHS10.F_f_st0);
		String sfst1 = s0.atoms.get(AtomsHS10.F_f_st1);
		String sfqp1 = s0.atoms.get(AtomsHS10.F_f_qp1);
		String sfqf1 = s0.atoms.get(AtomsHS10.F_f_qf1);
		String spst0 = s0.atoms.get(AtomsHS10.F_p_st0);
		String spst1 = s0.atoms.get(AtomsHS10.F_p_st1);
		String spst2 = s0.atoms.get(AtomsHS10.F_p_st2);
		String spqp1 = s0.atoms.get(AtomsHS10.F_p_qp1);
		String spqp2 = s0.atoms.get(AtomsHS10.F_p_qp2);
		String spqf1 = s0.atoms.get(AtomsHS10.F_p_qf1);
		String spqf2 = s0.atoms.get(AtomsHS10.F_p_qf2);
		String spst0rc = s0.atoms.get(AtomsHS10.F_p_st0rc);
		String spst0lc = s0.atoms.get(AtomsHS10.F_p_st0lc);
		String spst1rc = s0.atoms.get(AtomsHS10.F_p_st1rc);
		String spst1lc = s0.atoms.get(AtomsHS10.F_p_st1lc);
		String sPunct = s0.atoms.get(AtomsHS10.F_punct);
		String sAdjoin = s0.atoms.get(AtomsHS10.F_adjoin);

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
			SRParserCtbHandlerHS10.setParseFeaturesHS10(v, vd, m_vocab, bAdd, sAct, sfst0, sfst1, sfqf1, spst0, spst1, spst2, spqf1, spqf2, spst0rc, spst0lc, spst1rc, spst1lc, sPunct, sAdjoin, curidx, szSent, m_params.m_bUseLookAhead);

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

	// parsing features described in Huang and Sagae (2010)
	
	static String setParseFeaturesHS10(
			IntFeatVector v,
			List<String> vd,
			Vocab vocab,
			boolean bAdd,
			String sAct,
			String sfst0, String sfst1, String sfqf1, String spst0,
			String spst1, String spst2, String spqf1, String spqf2,
			String spst0rc, String spst0lc, String spst1rc, String spst1lc,
			String sPunct, String sAdjoin,
			final int curidx, final int szSent,
			boolean bUseLookAhead)
	{
		addFeature(v, "FP01-" + sfst0, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP02-" + spst0, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP03-" + sfst0 + SEP + spst0, sAct, 1.0, bAdd, vocab);

		addFeature(v, "FP04-" + sfst1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP05-" + spst1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP06-" + sfst1 + SEP + spst1, sAct, 1.0, bAdd, vocab);

		addFeature(v, "FP07-" + sfqf1, sAct, 1.0, bAdd, vocab);

		addFeature(v, "FP10-" + sfst0 + SEP + sfst1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP11-" + spst0 + SEP + spst1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP13-" + sfst0 + SEP + spst0 + SEP + spst1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP14-" + sfst0 + SEP + spst0 + SEP + sfst1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP15-" + sfst0 + SEP + sfst1 + SEP + spst1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP16-" + spst0 + SEP + sfst1 + SEP + spst1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP17-" + sfst0 + SEP + spst0 + SEP + sfst1 + SEP + spst1, sAct, 1.0, bAdd, vocab);

		if (bUseLookAhead)
		{
			if (vd != null) // use delayed evaluation
			{
				boolean bAddToDelay1 = false;
				boolean bAddToDelay2 = false;
				if (spqf1 == null && curidx < szSent)
				{
					spqf1 = getPosArgString(curidx);
					bAddToDelay1 = true;
				}
				if (spqf2 == null && curidx < szSent - 1)
				{
					spqf2 = getPosArgString(curidx + 1);
					bAddToDelay2 = true;
				}

				if (bAddToDelay1)
				{
					vd.add("FP08d-" + spqf1 + SEP + sAct);
					vd.add("FP09d-" + sfqf1 + SEP + spqf1 + SEP + sAct);
					vd.add("FP12d-" + spst0 + SEP + spqf1 + SEP + sAct);
					vd.add("FP19d-" + spst0 + SEP + spst1 + SEP + spqf1 + SEP + sAct);
					vd.add("FP21d-" + sfst0 + SEP + spst1 + SEP + spqf1 + SEP + sAct);
				}
				else
				{
					addFeature(v, "FP08d-" + spqf1, sAct, 1.0, bAdd, vocab);
					addFeature(v, "FP09d-" + sfqf1 + SEP + spqf1, sAct, 1.0, bAdd, vocab);
					addFeature(v, "FP12d-" + spst0 + SEP + spqf1, sAct, 1.0, bAdd, vocab);
					addFeature(v, "FP19d-" + spst0 + SEP + spst1 + SEP + spqf1, sAct, 1.0, bAdd, vocab);
					addFeature(v, "FP21d-" + sfst0 + SEP + spst1 + SEP + spqf1, sAct, 1.0, bAdd, vocab);
				}
				if (bAddToDelay1 || bAddToDelay2)
				{
					vd.add("FP18d-" + spst0 + SEP + spqf1 + SEP + spqf2 + SEP + sAct);
					vd.add("FP20d-" + sfst0 + SEP + spqf1 + SEP + spqf2 + SEP + sAct);
				}
				else
				{
					addFeature(v, "FP18d-" + spst0 + SEP + spqf1 + SEP + spqf2, sAct, 1.0, bAdd, vocab);
					addFeature(v, "FP20d-" + sfst0 + SEP + spqf1 + SEP + spqf2, sAct, 1.0, bAdd, vocab);
				}
			}
			else
			{
				if (spqf1 != null)
				{
					addFeature(v, "FP08-" + spqf1, sAct, 1.0, bAdd, vocab);
					addFeature(v, "FP09-" + sfqf1 + SEP + spqf1, sAct, 1.0, bAdd, vocab);
					addFeature(v, "FP12-" + spst0 + SEP + spqf1, sAct, 1.0, bAdd, vocab);
					addFeature(v, "FP19-" + spst0 + SEP + spst1 + SEP + spqf1, sAct, 1.0, bAdd, vocab);
					addFeature(v, "FP21-" + sfst0 + SEP + spst1 + SEP + spqf1, sAct, 1.0, bAdd, vocab);

					if (spqf2 != null)
					{
						addFeature(v, "FP18-" + spst0 + SEP + spqf1 + SEP + spqf2, sAct, 1.0, bAdd, vocab);
						addFeature(v, "FP20-" + sfst0 + SEP + spqf1 + SEP + spqf2, sAct, 1.0, bAdd, vocab);
					}
				}
			}
		}

		addFeature(v, "FP22-" + spst0 + SEP + spst1 + SEP + spst1lc, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP23-" + spst0 + SEP + spst1 + SEP + spst1rc, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP24-" + spst0 + SEP + spst0rc + SEP + spst1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP25-" + spst0 + SEP + spst0lc + SEP + spst1, sAct, 1.0, bAdd, vocab);
//		addFeature(v, "FP25-" + spst0 + SEP + spst1lc + SEP + spst1, sAct, 1.0, bAdd, vocab); // compatible with run0818--run0905
		addFeature(v, "FP26-" + sfst0 + SEP + spst1 + SEP + spst1rc, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP27-" + sfst0 + SEP + spst1 + SEP + spst0lc, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP28-" + spst0 + SEP + spst1 + SEP + spst2, sAct, 1.0, bAdd, vocab);

		addFeature(v, "FP29-", sAct, sAdjoin.equals("true") ? 1.0 : 0.0, bAdd, vocab);
		addFeature(v, "FP30-" + spst0 + SEP + spst1, sAct, sAdjoin.equals("true") ? 1.0 : 0.0, bAdd, vocab);
		addFeature(v, "FP31-" + sPunct, sAct, 1.0, bAdd, vocab);
		addFeature(v, "FP32-" + spst0 + SEP + spst1 + SEP + sPunct, sAct, 1.0, bAdd, vocab);
		return spqf1;
	}

	static void setTagSyntacticFeatures(
			IntFeatVector v, Vocab vocab, boolean bAdd, String sAct,
			String sfst0, String sfqf1, String spst0, String spst1, String spst0lc)
	{
		final int ln_sfst0 = sfst0.length();
		final char c_sfst0_b = sfst0.charAt(0);
		final char c_sfst0_e = sfst0.charAt(ln_sfst0 - 1);
		
		addFeature(v, "SF01-" + sfst0 + SEP + sfqf1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "SF02-" + spst0 + SEP + sfqf1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "SF03-" + spst0 + SEP + spst0lc + sfqf1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "SF04-" + c_sfst0_b, sAct, 1.0, bAdd, vocab);
		addFeature(v, "SF05-" + c_sfst0_e, sAct, 1.0, bAdd, vocab);
		addFeature(v, "SF06-" + c_sfst0_e + sfqf1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "SF07-" + spst1 + SEP + spst0 + SEP + sfqf1, sAct, 1.0, bAdd, vocab);
	}


}
