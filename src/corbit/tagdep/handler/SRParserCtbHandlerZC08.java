package corbit.tagdep.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import corbit.commons.Vocab;
import corbit.commons.ml.IntFeatVector;
import corbit.commons.transition.PDAction;
import corbit.tagdep.SRParserParameters;
import corbit.tagdep.SRParserState;
import corbit.tagdep.dict.TagDictionary;

/**
 * Word segmentation and POS tagging features from Zhang and Clark (2008)
 */
public class SRParserCtbHandlerZC08 extends SRParserHandler
{
	class AtomsZC08 extends AtomicFeatures
	{
		public static final int F_curidx = 0;
		public static final int F_f_qp1 = 1;
		public static final int F_f_qf1 = 2;
		public static final int F_p_qp2 = 3;
		public static final int F_p_qp1 = 4;

		public static final int NUM_FEATURE = 5;

		AtomsZC08(
				int curidx,
				String f_qp1,
				String f_qf1,
				String p_qp2,
				String p_qp1)
		{
			super(NUM_FEATURE, null);
			features = new String[NUM_FEATURE];
			features[F_curidx] = Integer.toString(curidx);
			features[F_f_qp1] = f_qp1;
			features[F_f_qf1] = f_qf1;
			features[F_p_qp2] = p_qp2;
			features[F_p_qp1] = p_qp1;
			setHash();
		}
	}

	public SRParserCtbHandlerZC08(Vocab v, SRParserParameters params)
	{
		super(v, params);
	}

	@Override
	public AtomicFeatures getAtomicFeatures(SRParserState s0)
	{
		SRParserState s1 = s0.preds.size() > 0 ? s0.pred0 : null;

		int idx = s0.curidx;

		String sfqp1 = idx > 0 ? s0.sent.get(idx - 1).form : OOR;
		String sfqf1 = idx < s0.sent.size() ? s0.sent.get(idx).form : OOR;

		String spqp1 = idx > 0 ? s0.pos[idx - 1] : OOR;
		String spqp2 = idx > 1 ? (s0.idbgn <= idx - 2) ? s0.pos[idx - 2] : s1.pos[idx - 2] : OOR;

		// debugging assertions

		assert (spqp1 != null);
		assert (spqp2 != null);

		// ad-hoc modification

		return new AtomsZC08(
				s0.curidx,
				sfqp1,
				sfqf1,
				spqp2,
				spqp1);
	}

	@Override
	public IntFeatVector getFeatures(SRParserState s0, PDAction act, List<String> vd, boolean bAdd)
	{
		IntFeatVector v = new IntFeatVector();

		String sfqp1 = s0.atoms.get(AtomsZC08.F_f_qp1);
		String sfqf1 = s0.atoms.get(AtomsZC08.F_f_qf1);
		String spqp1 = s0.atoms.get(AtomsZC08.F_p_qp1);
		String spqp2 = s0.atoms.get(AtomsZC08.F_p_qp2);

		String sfqf2 = s0.curidx < s0.sent.size() - 1 ? s0.sent.get(s0.curidx + 1).form : OOR;
		String sAct = act.toString();

		if (m_params.m_bUseTagFeature && act.isShiftPosAction())
			SRParserCtbHandlerZC08.setTagFeaturesZC08(v, m_vocab, m_dict, bAdd, sAct, sfqp1, sfqf1, sfqf2, spqp1, spqp2);

		return v;
	}

	// tagging features described in Zhang and Clark (2008)

	static void setTagFeaturesZC08(
			IntFeatVector v, Vocab vocab, TagDictionary dict, boolean bAdd, String sAct,
			String sfqp1, String sfqf1, String sfqf2, String spqp1, String spqp2)
	{
		final int ln_sfqp1 = sfqp1.length();
		final int ln_sfqf1 = sfqf1.length();
		final int ln_sfqf2 = sfqf2.length();

		final char[] c_sfqf1 = sfqf1.toCharArray();

		final char c_sfqp1_e = sfqp1.charAt(ln_sfqp1 - 1);
		final char c_sfqf1_b = c_sfqf1[0];
		final char c_sfqf1_e = c_sfqf1[ln_sfqf1 - 1];
		final char c_sfqf2_b = sfqf2.charAt(0);

		addFeature(v, "RF00-" + sfqf1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "RF00a-", sAct, 1.0, bAdd, vocab);
		addFeature(v, "RF01-" + spqp1, sAct, 1.0, bAdd, vocab);
		addFeature(v, "RF02-" + spqp2 + SEP + spqp1, sAct, 1.0, bAdd, vocab);

		if (ln_sfqp1 < 3)
			addFeature(v, "RF03-" + sfqp1, sAct, 1.0, bAdd, vocab);

		if (ln_sfqf2 < 3)
			addFeature(v, "RF04-" + sfqf2, sAct, 1.0, bAdd, vocab);

		if (ln_sfqf1 < 3)
		{
			addFeature(v, "RF05-" + sfqf1 + SEP + c_sfqp1_e, sAct, 1.0, bAdd, vocab);
			addFeature(v, "RF06-" + sfqf1 + SEP + c_sfqf2_b, sAct, 1.0, bAdd, vocab);
		}

		if (ln_sfqf1 == 1)
			addFeature(v, "RF07-" + c_sfqp1_e + sfqf1 + c_sfqf2_b, sAct, 1.0, bAdd, vocab);

		addFeature(v, "RF08-" + c_sfqf1_b, sAct, 1.0, bAdd, vocab);
		addFeature(v, "RF09-" + c_sfqf1_e, sAct, 1.0, bAdd, vocab);

		Set<Character> sc = new HashSet<Character>();
		for (int i = 0; i < ln_sfqf1; ++i)
		{
			if (sc.contains(c_sfqf1[i])) continue;

			sc.add(c_sfqf1[i]);
			// if (i > 0 && i < sfqf1.length() - 1) // modified from Zhang (2008)
			addFeature(v, "RF10a-" + c_sfqf1[i], sAct, 1.0, bAdd, vocab);
			if (i > 0)
				addFeature(v, "RF11-" + c_sfqf1[i] + SEP + c_sfqf1_b, sAct, 1.0, bAdd, vocab);
			if (i < ln_sfqf1 - 1)
				addFeature(v, "RF12-" + c_sfqf1[i] + SEP + c_sfqf1_e, sAct, 1.0, bAdd, vocab);
			if (i < ln_sfqf1 - 1 && c_sfqf1[i] == c_sfqf1[i + 1])
				addFeature(v, "RF13-" + c_sfqf1[i], sAct, 1.0, bAdd, vocab);
		}

		if (dict != null)
		{
			String _s1 = Character.toString(c_sfqf1_b);
			String _s2 = Character.toString(c_sfqf1_e);
			if (dict.inDictionary(_s1))
				for (String s : dict.getSeenTags(_s1))
					addFeature(v, "RF14-" + s, sAct, 1.0, bAdd, vocab);
			if (dict.inDictionary(_s2))
				for (String s : dict.getSeenTags(_s2))
					addFeature(v, "RF15-" + s, sAct, 1.0, bAdd, vocab);
		}
	}

}
