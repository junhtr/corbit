package corbit.segdep.handler;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import corbit.commons.Vocab;
import corbit.commons.ml.IntFeatVector;
import corbit.commons.ml.WeightVector;
import corbit.commons.transition.SDAction;
import corbit.commons.util.Pair;
import corbit.commons.util.Statics;
import corbit.commons.word.DepChunkTree;
import corbit.segdep.SRParserState;
import corbit.segdep.handler.DelayedFeature.DelayedFeatureType;
import corbit.segdep.transition.SRParserTransition;

public class SRParserCharBasedHandler extends SRParserHandler
{
	/*
	 * See Huang and Sagae (2010) for a detailed description of how the atomic (kernel) features work.
	 */
	private class AtomicTaggingFeatures implements AtomicFeatures
	{
		private final int curstep;
		final int curidx;
		final int spanbgn;
		final int spanend;
		final String sfqp1;
		final String sfqp2;
		final String spqp1;
		final String spqp2;

		private final int hash1;
		
		public AtomicTaggingFeatures(int curstep, int curidx, int spanbgn, int spanend,
				String sfqp1, String sfqp2, String spqp1, String spqp2)
		{
			this.curstep = curstep;
			this.curidx = curidx;
			this.spanbgn = spanbgn;
			this.spanend = spanend;
			this.sfqp1 = sfqp1;
			this.sfqp2 = sfqp2;
			this.spqp1 = spqp1;
			this.spqp2 = spqp2;
			this.hash1 = hash1();
		}

		private int hash1()
		{
			int hash = 13;
			hash = hash * 31 + curstep;
			hash = hash * 31 + curidx;
			hash = hash * 31 + spanbgn;
			hash = hash * 31 + spanend;
			hash = hash * 31 + sfqp1.hashCode();
			hash = hash * 31 + sfqp2.hashCode();
			hash = hash * 31 + spqp1.hashCode();
			hash = hash * 31 + spqp2.hashCode();
			return hash;
		}

		@Override
		public int hashCode()
		{
			return hash1;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null) throw new NullPointerException();
			if (!(obj instanceof AtomicTaggingFeatures)) throw new ClassCastException();
			
			AtomicTaggingFeatures atoms = (AtomicTaggingFeatures)obj;
			if (hash1 != atoms.hash1) return false; // to improve efficiency
			if (!(	curstep == atoms.curstep &&
					curidx == atoms.curidx &&
					spanbgn == atoms.spanbgn &&
					spanend == atoms.spanend &&
					sfqp1.equals(atoms.sfqp1) &&
					sfqp2.equals(atoms.sfqp2) && 
					spqp1.equals(atoms.spqp1) &&
					spqp2.equals(atoms.spqp2)
					)) return false;
			else return true;
		}
	}
	
	/*
	 * See Huang and Sagae (2010) for a detailed description of how the atomic (kernel) features work.
	 */
	private class AtomicParsingFeatures extends AtomicTaggingFeatures
	{
		private final int st0bgn;
		private final int st1bgn;
		
		final String sfst0;
		final String sfst1;
		final String spst0;
		final String spst1;
		final String spst2;
		final String spst0rc;
		final String spst0lc;
		final String spst1rc;
		final String spst1lc;
		final String punct;
		final boolean adjoin;
		
		final TreeSet<DelayedFeature> fvdelay; // must be ordered
	
		private final int hash2;
		
		public AtomicParsingFeatures(
				int curstep, int curidx, int spanbgn, int spanend, int st0bgn, int st1bgn, int st1sbgn,
				String sfst0, String sfst1, String sfqp1, String sfqp2,
				String spst0, String spst1, String spst2, String spqp1, String spqp2,
				String spst0rc, String spst0lc, String spst1rc, String spst1lc,
				String sPunct, boolean bAdjoin, List<DelayedFeature> fvdelay)
		{
			super(curstep, curidx, spanbgn, spanend, sfqp1, sfqp2, spqp1, spqp2);
			this.st0bgn = st0bgn;
			this.st1bgn = st1bgn;
			this.sfst0 = sfst0;
			this.sfst1 = sfst1;
			this.spst0 = spst0;
			this.spst1 = spst1;
			this.spst2 = spst2;
			this.spst0rc = spst0rc;
			this.spst0lc = spst0lc;
			this.spst1rc = spst1rc;
			this.spst1lc = spst1lc;
			this.punct = sPunct;
			this.adjoin = bAdjoin;
			
			this.fvdelay = fvdelay != null ? new TreeSet<DelayedFeature>(fvdelay) : null;
			this.hash2 = hash2();
		}

		private int hash2()
		{
			int hash = super.hashCode();
			hash = hash * 31 + st0bgn;
			hash = hash * 31 + st1bgn;
			hash = hash * 31 + sfst0.hashCode();
			hash = hash * 31 + sfst1.hashCode();
			hash = hash * 31 + spst0.hashCode();
			hash = hash * 31 + spst1.hashCode();
			hash = hash * 31 + spst2.hashCode();
			hash = hash * 31 + spst0rc.hashCode();
			hash = hash * 31 + spst0lc.hashCode();
			hash = hash * 31 + spst1rc.hashCode();
			hash = hash * 31 + spst1lc.hashCode();
			hash = hash * 31 + punct.hashCode();
			hash = hash * 31 + (adjoin ? 1 : 0);
			if (fvdelay != null)
				for (DelayedFeature s: fvdelay)
					hash = hash * 31 + s.hashCode();
			return hash;
		}

		@Override
		public int hashCode()
		{
			return hash2;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null) throw new NullPointerException();
			if (!(obj instanceof AtomicTaggingFeatures)) throw new ClassCastException();
			
			AtomicParsingFeatures atoms = (AtomicParsingFeatures)obj;
			if (hash2 != atoms.hash2) return false; // to improve efficiency
			if (!super.equals(atoms)) return false;
			if (!(	st0bgn == atoms.st0bgn &&
					st1bgn == atoms.st1bgn &&
					sfst0.equals(atoms.sfst0) &&
					sfst1.equals(atoms.sfst1) &&
					spst0.equals(atoms.spst0) &&
					spst1.equals(atoms.spst1) &&
					spst2.equals(atoms.spst2) &&
					spst0rc.equals(atoms.spst0rc) &&
					spst0lc.equals(atoms.spst0lc) &&
					spst1rc.equals(atoms.spst1rc) &&
					spst1lc.equals(atoms.spst1lc) &&
					punct.equals(atoms.punct) &&
					adjoin == atoms.adjoin
					)) return false;
			Set<DelayedFeature> _fvdelay = atoms.fvdelay;
			if (fvdelay != null)
			{
				if (fvdelay == null) return false;
				else return Statics.setEquals(fvdelay, _fvdelay);
			}
			else if (_fvdelay != null) return false;
			else return true;
		}
	}

	/*
	 * end of AtomicParsingFeatures
	 */
	
	@Override
	public AtomicFeatures getAtomicFeatures(SRParserState s0)
	{
		int curidx = s0.curidx;
		
		DepChunkTree wst0 = s0.pstck[0];
		DepChunkTree wst1 = s0.pstck[1];
		DepChunkTree wst2 = s0.pstck[2];
		DepChunkTree wstqp1 = curidx > 0 ? wst0.findSubtreeEndsAt(curidx) : null;
		DepChunkTree wstqp2 = curidx > 1 ? wst0.findSubtreeEndsAt(wstqp1.begin) != null
				? wst0.findSubtreeEndsAt(wstqp1.begin)
				: wst1 != null ? wst1.findSubtreeEndsAt(wstqp1.begin) : null : null;

		assert (wstqp1 == null || !wstqp1.isRoot());
		assert (wstqp2 == null || !wstqp2.isRoot());
		
		String sfqp1 = wstqp1 != null ? wstqp1.form : OOR;
		String sfqp2 = wstqp2 != null ? wstqp2.form : OOR;
		String spqp1 = wstqp1 != null ? wstqp1.tag : OOR;
		String spqp2 = wstqp2 != null ? wstqp2.tag : OOR;
		
		if (!m_bParse)
		{
			return new AtomicTaggingFeatures(s0.curstep, curidx, s0.idbgn, s0.idend, sfqp1, sfqp2, spqp1, spqp2);
		}
		else
		{
			DepChunkTree wst0rc = wst0 != null ? getRightmostChild(wst0) : null;
			DepChunkTree wst0lc = wst0 != null ? getLeftmostChild(wst0) : null;
			DepChunkTree wst1rc = wst1 != null ? getRightmostChild(wst1) : null;
			DepChunkTree wst1lc = wst1 != null ? getLeftmostChild(wst1) : null;
	
			String sfst0 = wst0.form;
			String sfst1 = wst1 != null ? wst1.form : OOR;
			
			String spst0 = wst0.tag;
			String spst1 = wst1 != null ? wst1.tag : OOR;
			String spst2 = wst2 != null ? wst2.tag : OOR;
			String spst0rc = wst0rc != null ? wst0rc.tag : OOR;
			String spst0lc = wst0lc != null ? wst0lc.tag : OOR;
			String spst1rc = wst1rc != null ? wst1rc.tag : OOR;
			String spst1lc = wst1lc != null ? wst1lc.tag : OOR;
	
			String sPunct = (wst0 != null && wst1 != null) ? getPunctInBetween(wst1, wst0) : OOR;
			boolean bAdjoin = (wst0 != null && wst1 != null && wst0.begin == wst1.end);
	
			int st1bgn = wst1 != null ? wst1.begin : -2;
			int st1sbgn = wst1 != null ? wst1.getSpanBeginIndex() : -2;
			return new AtomicParsingFeatures(s0.curstep, curidx, s0.idbgn, s0.idend, wst0.begin, st1bgn, st1sbgn, 
					sfst0, sfst1, sfqp1, sfqp2, spst0, spst1, spst2, spqp1, spqp2,
					spst0rc, spst0lc, spst1rc, spst1lc, sPunct, bAdjoin, s0.fvdelay);
		}
	}

	/*
	 * beginning of main
	 */
	
	protected final boolean m_bParse;
	protected final boolean m_bLemmaFilter;
	protected final boolean m_bCharType;
	protected final double m_dParserWeight;
	
	public SRParserCharBasedHandler(Vocab v, boolean bParse, boolean bLemmaFilter, boolean bCharType, double dParserWeight)
	{
		super(v);
		m_bParse = bParse;
		m_bLemmaFilter = bLemmaFilter;
		m_bCharType = bCharType;
		m_dParserWeight = dParserWeight;
	}

	/**
	 * This function should be executed with a VM option that enables efficient concatenation of strings (such as --XX:AggressiveOpts) 
	 */
	@Override
	public Pair<IntFeatVector, Double> getFeatures(SRParserState s0, SDAction act, List<DelayedFeature> vd, boolean bAdd, WeightVector w, SRParserTransition.SRParserCache cache)
	{
		if (!m_bParse) return getTaggingFeatures(s0, act, vd, bAdd, w, cache);
		
		IntFeatVector v = bAdd ? new IntFeatVector() : null;
		double sc = 0.0;
		
		AtomicParsingFeatures atoms = (AtomicParsingFeatures)s0.atoms;
		final int szSent = s0.sent.length();
		
		int curidx = atoms.curidx;
		int spanbgn = atoms.spanbgn;
		int spanend = atoms.spanend;
		String sfqp1 = atoms.sfqp1;
		String sfqp2 = atoms.sfqp2;
		String spqp2 = atoms.spqp2;
		String spqp1 = atoms.spqp1;

		/*
		 *  Evaluate chunking and tagging features
		 */
		
		if (!(act.isShiftTagAction() || act == SDAction.APPEND || act == SDAction.REDUCE_LEFT || act == SDAction.REDUCE_RIGHT || act.isLabeledReduceLeft() || act.isLabeledReduceRight()))
			throw new UnsupportedOperationException(act.toString());
		
		int wordNormLength = 16; // threshold above which word length is clipped.
		
		int ln_sfqp1 = sfqp1.equals(OOR) ? 0 : sfqp1.length();
		int ln_sfqp2 = sfqp2.equals(OOR) ? 0 : sfqp2.length();
		String ln_sfqp1s = Integer.toString(Math.min(ln_sfqp1, wordNormLength));
		String ln_sfqp2s = Integer.toString(Math.min(ln_sfqp2, wordNormLength));
		String c_sfqp1_b = sfqp1.substring(0, 1);
		String c_sfqp1_e = ln_sfqp1 > 0 ? sfqp1.substring(ln_sfqp1 - 1, ln_sfqp1) : OOR;
		String c_sfqp2_e = ln_sfqp2 > 0 ? sfqp2.substring(ln_sfqp2 - 1, ln_sfqp2) : OOR;
		
		AtomicParsingFeatures atoms2 = (AtomicParsingFeatures)atoms;
		String sfst0 = atoms2.sfst0;
		String sfst1 = atoms2.sfst1;
		String spst0 = atoms2.spst0;
		String spst1 = atoms2.spst1;
		String spst2 = atoms2.spst2;
		String spst0rc = atoms2.spst0rc;
		String spst0lc = atoms2.spst0lc;
		String spst1rc = atoms2.spst1rc;
		String spst1lc = atoms2.spst1lc;
		String sPunct = atoms2.punct;
		boolean bAdjoin = atoms2.adjoin;
		
		String c0 = curidx < szSent ? s0.sent.substring(curidx, curidx + 1) : OOR;

		/*
		 * balancing features (original)
		 */
		
		{
			if (act == SDAction.APPEND || act.isShiftTagAction())
				sc += addFeature(v, "SH00-" + c_sfqp1_e + spqp1, "", 1.0, bAdd, w);
			sc += addFeature(v, "SH001-" + c_sfqp1_e, act.toString(), 1.0, bAdd, w);
			sc += addFeature(v, "SH002-" + c_sfqp1_e + spqp1, act.toString(), 1.0, bAdd, w);
		}

		/*
		 *  Word segmentation and tagging features from Zhang and Clark (2010)
		 */
		
		{
			if (act == SDAction.APPEND)
			{
				String sLabel = act.isShiftTagAction() ? SDAction.SHIFT.toString() : act.toString();
				
				double _sc = 0.0;
				
				String c1 = curidx - 1 >= 0 ? Character.toString(s0.sent.charAtIgnoreRange(curidx - 1)) : OOR;
				
				/* character bigrams within the word */
				sc += addFeature(v, "SH07-" + c0 + c1, sLabel, 1.0, bAdd, w);
				/* tag on a word containing char */
//				sc += addFeature(v, "SH25a-" + spqp1 + c0, sLabel, 1.0, bAdd, w);
				if (act == SDAction.APPEND)
					sc += addFeature(v, "SH25-" + spqp1 + c0, "", 1.0, bAdd, w); // shared with shift-tag action
				/* tag on a word starting with char and containing char */
				sc += addFeature(v, "SH26-" + spqp1 + c0 + c_sfqp1_b, sLabel, 1.0, bAdd, w);
				/* tag on a word ending with char and containing char */
//				for (String s: m_dict.getSeenTags(c_sfqp1_b))
//					sc += addFeature(v, "SH28a-" + spqp1 + c0 + SEP + s, sLabel, 1.0, bAdd, w);
				sc += addFeature(v, "SH28-" + spqp1 + c0 + SEP + m_dict.getCharType(c_sfqp1_b.charAt(0)), sLabel, 1.0, bAdd, w);
				/* tag and character bigrams with in the word */
				sc += addFeature(v, "SH31-" + spqp1 + c0 + c1, sLabel, 1.0, bAdd, w);

				sc += _sc;
			}
		}
		
		{
			/*
			 * Features with full lexical information
			 * This block is evaluated only when the word is a bare word without children.
			 */
			if (spanend - spanbgn == ln_sfqp1) // for shift or first reduce-left/right
			{
				String sLabel = act == SDAction.APPEND ? SDAction.APPEND.toString() : SDAction.SHIFT.toString();
			
				if (cache != null && !bAdd && act != SDAction.APPEND && cache.wordScore().containsKey(s0))
					sc += cache.wordScore().get(s0); // use cached information to improve speed
				else
				{
					double _sc = 0.0;
					
					/*
					 * Features from external lexicon (original)
					 */
					
					if (m_wordlists != null)
					{
						for (int i=0; i<m_wordlists.length; ++i)
						{
							if (m_wordlists[i].contains(sfqp1))
							{
								_sc += addFeature(v, "DD00-" + (i + 1) + SEP + ln_sfqp1s, sLabel, 1.0, bAdd, w);
								_sc += addFeature(v, "DD01-" + (i + 1) + SEP + ln_sfqp1s + SEP + spqp1, sLabel, 1.0, bAdd, w);
							}
							else
							{
								_sc += addFeature(v, "DN00-" + (i + 1) + SEP + ln_sfqp1s, sLabel, 1.0, bAdd, w);
								_sc += addFeature(v, "DN01-" + (i + 1) + SEP + ln_sfqp1s + SEP + spqp1, sLabel, 1.0, bAdd, w);
							}
						}
					}
					
					// (1) word unigram
					_sc += addFeature(v, "SH01-" + sfqp1, sLabel, 1.0, bAdd, w);
					// (2) word bigram
					_sc += addFeature(v, "SH02-" + sfqp2 + SEP + sfqp1, sLabel, 1.0, bAdd, w);
					// (3) single-character word
					if (ln_sfqp1 == 1)
						_sc += addFeature(v, "SH03-" + sfqp1, sLabel, 1.0, bAdd, w);
					// (4) lemma and length with starting character
					_sc += addFeature(v, "SH04-" + c_sfqp1_b + ln_sfqp1s, sLabel, 1.0, bAdd, w);
					// (5) lemma and length with ending character
					_sc += addFeature(v, "SH05-" + c_sfqp1_e + ln_sfqp1s, sLabel, 1.0, bAdd, w);
					// (6) space-separated characters
					_sc += addFeature(v, "SH06-" + c_sfqp1_e + c0, sLabel, 1.0, bAdd, w);
					// (8) the first and last character of the word
					_sc += addFeature(v, "SH08-" + c_sfqp1_b + c_sfqp1_e, sLabel, 1.0, bAdd, w);
					// (9) word and next character
					_sc += addFeature(v, "SH09-" + sfqp1 + c0, sLabel, 1.0, bAdd, w);
					// (10) word and previous character
					_sc += addFeature(v, "SH10-" + c_sfqp2_e + sfqp1, sLabel, 1.0, bAdd, w);
					// (11) the starting characters of two consecutive words
					_sc += addFeature(v, "SH11-" + c_sfqp1_b + c0, sLabel, 1.0, bAdd, w);
					// (12) the ending characters of two consecutive words
					_sc += addFeature(v, "SH12-" + c_sfqp2_e + c_sfqp1_e, sLabel, 1.0, bAdd, w);
					// (13) word length with previous word
					_sc += addFeature(v, "SH13-" + sfqp2 + ln_sfqp1s, sLabel, 1.0, bAdd, w);
					// (14) word length with next word
					_sc += addFeature(v, "SH14-" + ln_sfqp2s + sfqp1, sLabel, 1.0, bAdd, w);
					
					// (15) tag and word
					_sc += addFeature(v, "SH15-" + sfqp1 + SEP + spqp1, sLabel, 1.0, bAdd, w);
					
					if (!m_bLemmaFilter || ln_sfqp1 < 3)
					{
						_sc += addFeature(v, "SH19-" + spqp2 + SEP + sfqp1, sLabel, 1.0, bAdd, w);
						_sc += addFeature(v, "SH20-" + sfqp1 + SEP + spqp1 + c_sfqp2_e, sLabel, 1.0, bAdd, w);
						_sc += addFeature(v, "SH21-" + sfqp1 + SEP + spqp1 + c0, sLabel, 1.0, bAdd, w);
					}
					if (ln_sfqp1 == 1)
						_sc += addFeature(v, "SH22-" + c_sfqp2_e + sfqp1 + c0 + spqp1, sLabel, 1.0, bAdd, w);
					_sc += addFeature(v, "SH24-" + spqp1 + c_sfqp1_e, sLabel, 1.0, bAdd, w);
					
					for (int i = 0; i < sfqp1.length() - 1; ++i)
						_sc += addFeature(v, "SH27-" + spqp1 + sfqp1.charAt(i) + c_sfqp1_e, sLabel, 1.0, bAdd, w);

					String sCat = m_dict.getCharType(c_sfqp1_e.charAt(0));
					for (int i = 0; i < sfqp1.length() - 1; ++i)
					{
//						for (String s: m_dict.getSeenTags(c_sfqp1_e))
//							_sc += addFeature(v, "SH29a-" + spqp1 + sfqp1.charAt(i) + SEP + s, sLabel, 1.0, bAdd, w);
						_sc += addFeature(v, "SH29-" + spqp1 + sfqp1.charAt(i) + SEP + sCat, sLabel, 1.0, bAdd, w);
					}
					
					/*
					 * Evaluated partial score and put it to the cache
					 */
					
					sc += _sc;
					if (cache != null && !bAdd && act != SDAction.APPEND)
						cache.wordScore().put(s0, _sc);
				}
				
				/*
				 * Evaluated delayed features (Hatori et al. (2011))
				 */
				if (vd != null && act != SDAction.APPEND)
				{
					sc += evaluateDelayedFeatures(v, vd, curidx - ln_sfqp1, 1, sfqp1, spqp1, bAdd, w);
					if (curidx == szSent)
						sc += evaluateDelayedFeatures(v, vd, curidx - ln_sfqp1, 2, OOR, OOR, bAdd, w);
				}
			}

			/*
			 * Features for POS tagging
			 */
			
			if (act.isShiftTagAction())
			{
				String sLabel = "";
				String spqf1 = act.getTag();
				
				sc += addFeature(v, "SH16-" + spqp1 + SEP + spqf1, sLabel, 1.0, bAdd, w);
				sc += addFeature(v, "SH17-" + spqp2 + SEP + spqp1 + SEP + spqf1, sLabel, 1.0, bAdd, w);
				if (!m_bLemmaFilter || ln_sfqp1 < 3)
					sc += addFeature(v, "SH18-" + sfqp1 + SEP + spqf1, sLabel, 1.0, bAdd, w);
				sc += addFeature(v, "SH23-" + spqf1 + c0, sLabel, 1.0, bAdd, w);
//				sc += addFeature(v, "SH25a-" + spqp1 + c0, sLabel, 1.0, bAdd, w);
				sc += addFeature(v, "SH25-" + spqf1 + c0, "", 1.0, bAdd, w); // shared with append action
//				for (String s: m_dict.getSeenTags(c0))
//					sc += addFeature(v, "SH28a-" + c0 + spqf1 + SEP + s, sLabel, 1.0, bAdd, w);
//				for (String s: m_dict.getSeenTags(c0))
//					sc += addFeature(v, "SH28b-" + c0 + spqf1 + SEP + s, sLabel, 1.0, bAdd, w);
				sc += addFeature(v, "SH30-" + spqf1 + c0 + SEP + spqp1 + c_sfqp1_e, sLabel, 1.0, bAdd, w);

				/*
				 * Evaluated delayed features
				 */
				
				if (vd != null)
				{
					sc += evaluateDelayedFeatures(v, vd, curidx - ln_sfqp1, 1, null, spqp1, bAdd, w);
					sc += evaluateDelayedFeatures(v, vd, curidx - ln_sfqp1, 2, null, spqf1, bAdd, w);
					assert (curidx < s0.sent.length() || vd.size() == 0);
					if (!(curidx < s0.sent.length() || vd.size() == 0)) throw new RuntimeException();
				}
			}
			
		}
		
		/*
		 * Parsing features
		 */
		if (m_bParse)
			sc += addHS10ParserFeatures(act, vd, bAdd, w, v, curidx, szSent, sfst0, sfst1, spst0, spst1, spst2, spst0rc, spst0lc, spst1rc, spst1lc, sPunct, bAdjoin, spanend - spanbgn == ln_sfqp1);
		
		return new Pair<IntFeatVector, Double>(v, sc);
	}

	/**
	 * Add dependency parser features from Huang and Sagae (2010)
	 */
	double addHS10ParserFeatures(SDAction act, List<DelayedFeature> vd, boolean bAdd, WeightVector w, IntFeatVector v, int curidx, final int szSent,
			String sfst0, String sfst1, String spst0, String spst1, String spst2,
			String spst0rc, String spst0lc, String spst1rc, String spst1lc, String sPunct, boolean bAdjoin, boolean bWordLevel)
	{
		String sfqf1 = curidx < szSent ? null : OOR;
		String spqf1 = curidx < szSent ? null : OOR;
		String spqf2 = curidx < szSent - 1 ? null : OOR;

		double sc = 0.0;
		
		String sParseLabel = act.toString();
		
		final double weight = m_dParserWeight;
		
		/*
		 * For all actions
		 */
		{
			String sParseLabel1 = sParseLabel + "-" + (bWordLevel ? 1 : 0);

			sc += addFeature(v, "FP01-" + sfst0, sParseLabel1, weight, bAdd, w);
			sc += addFeature(v, "FP02-" + spst0, sParseLabel1, weight, bAdd, w);
			sc += addFeature(v, "FP03-" + sfst0 + SEP + spst0, sParseLabel1, weight, bAdd, w);
	
			sc += addFeature(v, "FP04-" + sfst1, sParseLabel1, weight, bAdd, w);
			sc += addFeature(v, "FP05-" + spst1, sParseLabel1, weight, bAdd, w);
			sc += addFeature(v, "FP06-" + sfst1 + SEP + spst1, sParseLabel1, weight, bAdd, w);
	
			sc += addFeature(v, "FP10-" + sfst0 + SEP + sfst1, sParseLabel1, weight, bAdd, w);
			sc += addFeature(v, "FP11-" + spst0 + SEP + spst1, sParseLabel1, weight, bAdd, w);
			
			if (act != SDAction.APPEND)
			{
				sc += addFeature(v, "FP13-" + sfst0 + SEP + spst0 + SEP + spst1, sParseLabel1, weight, bAdd, w);
				sc += addFeature(v, "FP14-" + sfst0 + SEP + spst0 + SEP + sfst1, sParseLabel1, weight, bAdd, w);
				sc += addFeature(v, "FP15-" + sfst0 + SEP + sfst1 + SEP + spst1, sParseLabel1, weight, bAdd, w);
				sc += addFeature(v, "FP16-" + spst0 + SEP + sfst1 + SEP + spst1, sParseLabel1, weight, bAdd, w);
				sc += addFeature(v, "FP17-" + sfst0 + SEP + spst0 + SEP + sfst1 + SEP + spst1, sParseLabel1, weight, bAdd, w);
			}
		}

		/*
		 * Only for parsing actions
		 */
		if (act != SDAction.APPEND)
		{
			String sParseLabel2 = sParseLabel;
			
			if (vd != null) // use delayed features evaluation
			{
				if (sfqf1 == null)
				{
					vd.add(new DelayedFeature("FP07-" + sParseLabel2, curidx, 1, 0, DelayedFeatureType.LEX, null, weight));
				}
				else
					sc += addFeature(v, "FP07-" + sParseLabel2 + SEP + sfqf1, weight, bAdd, w);
				
				if (spqf1 == null)
				{
					vd.add(new DelayedFeature("FP08-" + sParseLabel2, curidx, 1, 0, DelayedFeatureType.POS, null, weight));
					vd.add(new DelayedFeature("FP12-" + sParseLabel2 + SEP + spst0, curidx, 1, 0, DelayedFeatureType.POS, null, weight));
					vd.add(new DelayedFeature("FP19-" + sParseLabel2 + SEP + spst0 + SEP + spst1, curidx, 1, 0, DelayedFeatureType.POS, null, weight));
					vd.add(new DelayedFeature("FP21-" + sParseLabel2 + SEP + sfst0 + SEP + spst1, curidx, 1, 0, DelayedFeatureType.POS, null, weight));
				}
				else
				{
					sc += addFeature(v, "FP08-" + sParseLabel2 + SEP + spqf1, weight, bAdd, w);
					sc += addFeature(v, "FP12-" + sParseLabel2 + SEP + spst0 + SEP + spqf1, weight, bAdd, w);
					sc += addFeature(v, "FP19-" + sParseLabel2 + SEP + spst0 + SEP + spst1 + SEP + spqf1, weight, bAdd, w);
					sc += addFeature(v, "FP21-" + sParseLabel2 + SEP + sfst0 + SEP + spst1 + SEP + spqf1, weight, bAdd, w);
				}

				if (spqf1 == null || sfqf1 == null)
				{
					DelayedFeature df = new DelayedFeature("FP09-" + sParseLabel2, curidx, 1, 1, DelayedFeatureType.LEX, DelayedFeatureType.POS, weight);
					if (sfqf1 != null) df.fill(1, DelayedFeatureType.LEX, sfqf1);
					if (spqf1 != null) df.fill(1, DelayedFeatureType.POS, spqf1);
					vd.add(df);
				}
				else
					sc += addFeature(v, "FP09-" + sParseLabel2 + SEP + sfqf1 + SEP + spqf1, weight, bAdd, w);
				
				if (spqf1 == null || spqf2 == null)
				{
					DelayedFeature df1 = new DelayedFeature("FP18-" + sParseLabel2 + SEP + spst0, curidx, 1, 2, DelayedFeatureType.POS, DelayedFeatureType.POS, weight);
					DelayedFeature df2 = new DelayedFeature("FP20-" + sParseLabel2 + SEP + sfst0, curidx, 1, 2, DelayedFeatureType.POS, DelayedFeatureType.POS, weight);
					if (spqf1 != null) df1.fill(1, DelayedFeatureType.POS, spqf1);
					if (spqf2 != null) df2.fill(2, DelayedFeatureType.POS, spqf2);
					vd.add(df1);
					vd.add(df2);
				}
				else
				{
					sc += addFeature(v, "FP18-" + sParseLabel2 + SEP + spst0 + SEP + spqf1 + SEP + spqf2, weight, bAdd, w);
					sc += addFeature(v, "FP20-" + sParseLabel2 + SEP + sfst0 + SEP + spqf1 + SEP + spqf2, weight, bAdd, w);
				}
			}
			else
			{
				if (sfqf1 != null)
					sc += addFeature(v, "FP07-" + sfqf1, sParseLabel2, weight, bAdd, w);
				
				if (spqf1 != null)
				{
					sc += addFeature(v, "FP08-" + spqf1, sParseLabel2, weight, bAdd, w);
					sc += addFeature(v, "FP09-" + sfqf1 + SEP + spqf1, sParseLabel2, weight, bAdd, w);
					sc += addFeature(v, "FP12-" + spst0 + SEP + spqf1, sParseLabel2, weight, bAdd, w);
					sc += addFeature(v, "FP19-" + spst0 + SEP + spst1 + SEP + spqf1, sParseLabel2, weight, bAdd, w);
					sc += addFeature(v, "FP21-" + sfst0 + SEP + spst1 + SEP + spqf1, sParseLabel2, weight, bAdd, w);
	
					if (spqf2 != null)
					{
						sc += addFeature(v, "FP18-" + spst0 + SEP + spqf1 + SEP + spqf2, sParseLabel2, weight, bAdd, w);
						sc += addFeature(v, "FP20-" + sfst0 + SEP + spqf1 + SEP + spqf2, sParseLabel2, weight, bAdd, w);
					}
				}
			}
	
			sc += addFeature(v, "FP22-" + spst0 + SEP + spst1 + SEP + spst1lc, sParseLabel2, weight, bAdd, w);
			sc += addFeature(v, "FP23-" + spst0 + SEP + spst1 + SEP + spst1rc, sParseLabel2, weight, bAdd, w);
			sc += addFeature(v, "FP24-" + spst0 + SEP + spst0rc + SEP + spst1, sParseLabel2, weight, bAdd, w);
			sc += addFeature(v, "FP25-" + spst0 + SEP + spst1lc + SEP + spst1, sParseLabel2, weight, bAdd, w);
			sc += addFeature(v, "FP26-" + sfst0 + SEP + spst1 + SEP + spst1rc, sParseLabel2, weight, bAdd, w);
			sc += addFeature(v, "FP27-" + sfst0 + SEP + spst1 + SEP + spst0lc, sParseLabel2, weight, bAdd, w);
			sc += addFeature(v, "FP28-" + spst0 + SEP + spst1 + SEP + spst2, sParseLabel2, weight, bAdd, w);
	
			sc += addFeature(v, "FP29-", sParseLabel2, bAdjoin ? weight : 0.0, bAdd, w);
			sc += addFeature(v, "FP30-" + spst0 + SEP + spst1, sParseLabel2, bAdjoin ? weight : 0.0, bAdd, w);
			sc += addFeature(v, "FP31-" + sPunct, sParseLabel2, weight, bAdd, w);
			sc += addFeature(v, "FP32-" + spst0 + SEP + spst1 + SEP + sPunct, sParseLabel2, weight, bAdd, w);
		}
		
		return sc;
	}

	/**
	 * Joint word segmentation and POS tagging features from Zhang and Clark (2010)
	 * This function exactly follows their original implementation for comparison, 
	 * and should not be modified.
	 */
	private Pair<IntFeatVector, Double> getTaggingFeatures(SRParserState s0, SDAction act, List<DelayedFeature> vd, boolean bAdd, WeightVector w, SRParserTransition.SRParserCache cache)
	{
		IntFeatVector v = bAdd ? new IntFeatVector() : null;
		double sc = 0.0;
		
		AtomicTaggingFeatures atoms = (AtomicTaggingFeatures)s0.atoms;
		final int szSent = s0.sent.length();
		
		int curidx = atoms.curidx;
		String sfqp1 = atoms.sfqp1;
		String sfqp2 = atoms.sfqp2;
		String spqp2 = atoms.spqp2;
		String spqp1 = atoms.spqp1;

		/*
		 *  Evaluate chunking and tagging features
		 */
		
		if (!(act.isShiftTagAction() || act == SDAction.APPEND))
			throw new UnsupportedOperationException(act.toString());
		
		int wordNormLength = 16;
		
		int ln_sfqp1 = sfqp1.equals(OOR) ? 0 : sfqp1.length();
		int ln_sfqp2 = sfqp2.equals(OOR) ? 0 : sfqp2.length();
		String ln_sfqp1s = Integer.toString(Math.min(ln_sfqp1, wordNormLength));
		String ln_sfqp2s = Integer.toString(Math.min(ln_sfqp2, wordNormLength));
		String c_sfqp1_b = sfqp1.substring(0, 1);
		String c_sfqp1_e = ln_sfqp1 > 0 ? sfqp1.substring(ln_sfqp1 - 1, ln_sfqp1) : OOR;
		String c_sfqp2_e = ln_sfqp2 > 0 ? sfqp2.substring(ln_sfqp2 - 1, ln_sfqp2) : OOR;
		
		String c0 = curidx < szSent ? s0.sent.substring(curidx, curidx + 1) : OOR;
		
		if (act == SDAction.APPEND)
		{
			String sLabel = "";
			
			String c1 = s0.curidx - 1 >= 0 ? Character.toString(s0.sent.charAt(s0.curidx - 1)) : OOR;
			
			// character bigrams within the word
			sc += addFeature(v, "SH07-" + c0 + c1, sLabel, 1.0, bAdd, w);
			// tag on a word containing char
			sc += addFeature(v, "SH25-" + spqp1 + c0, "", 1.0, bAdd, w); // shared with shift-tag action
			// tag on a word starting with char and containing char
			sc += addFeature(v, "SH26-" + spqp1 + c0 + c_sfqp1_b, sLabel, 1.0, bAdd, w);
			// tag on a word ending with char and containing char
			sc += addFeature(v, "SH28-" + spqp1 + c0 + SEP + m_dict.getCharType(c_sfqp1_b.charAt(0)), sLabel, 1.0, bAdd, w);
			// tag and character bigrams with in the word
			sc += addFeature(v, "SH31-" + spqp1 + c0 + c1, sLabel, 1.0, bAdd, w);
			
			// additional features to incorporate word-class information (original)
			if (m_bCharType)
			{
				sc += addFeature(v, "SH90-" + (s0.sent.charTypeAt(curidx) == 4 ? "1" : "0"), sLabel, 1.0, bAdd, w);
				sc += addFeature(v, "SH91-" + s0.sent.charTypeAt(curidx - 1) + SEP + s0.sent.charTypeAt(curidx), sLabel, 1.0, bAdd, w);
				sc += addFeature(v, "SH92-" + s0.sent.charTypeAt(curidx - 2) + SEP + s0.sent.charTypeAt(curidx - 1) + SEP + s0.sent.charTypeAt(curidx), sLabel, 1.0, bAdd, w);
			}
		}
		else
		{
			/*
			 * Word segmentation features
			 */
			{
				String sLabel = "";
				
				if (cache != null && !bAdd && cache.wordScore().containsKey(s0))
					sc += cache.wordScore().get(s0);
				else
				{
					double _sc = 0.0;
					
					/*
					 * Features from external lexicon
					 */
					
					if (m_wordlists != null)
					{
						for (int i = 0; i < m_wordlists.length; ++i)
						{
							if (m_wordlists[i].contains(sfqp1))
							{
								_sc += addFeature(v, "DD00-" + (i + 1) + SEP + ln_sfqp1s, sLabel, 1.0, bAdd, w);
								_sc += addFeature(v, "DD01-" + (i + 1) + SEP + ln_sfqp1s + SEP + spqp1, sLabel, 1.0, bAdd, w);
							}
							else
							{
								_sc += addFeature(v, "DN00-" + (i + 1) + SEP + ln_sfqp1s, sLabel, 1.0, bAdd, w);
								_sc += addFeature(v, "DN01-" + (i + 1) + SEP + ln_sfqp1s + SEP + spqp1, sLabel, 1.0, bAdd, w);
							}
						}
					}
					
					// word unigram
					_sc += addFeature(v, "SH01-" + sfqp1, sLabel, 1.0, bAdd, w);
					// word bigram
					_sc += addFeature(v, "SH02-" + sfqp2 + SEP + sfqp1, sLabel, 1.0, bAdd, w);
					// single-character word
					if (ln_sfqp1 == 1)
						_sc += addFeature(v, "SH03-" + sfqp1, sLabel, 1.0, bAdd, w);
					// lemma and length with starting character
					_sc += addFeature(v, "SH04-" + c_sfqp1_b + ln_sfqp1s, sLabel, 1.0, bAdd, w);
					// lemma and length with ending character
					_sc += addFeature(v, "SH05-" + c_sfqp1_e + ln_sfqp1s, sLabel, 1.0, bAdd, w);
					// space-separater characters
					_sc += addFeature(v, "SH06-" + c_sfqp1_e + c0, sLabel, 1.0, bAdd, w);
					// the first and last character of the word
					_sc += addFeature(v, "SH08-" + c_sfqp1_b + c_sfqp1_e, sLabel, 1.0, bAdd, w);
					// word and next character
					_sc += addFeature(v, "SH09-" + sfqp1 + c0, sLabel, 1.0, bAdd, w);
					// word and previous character
					_sc += addFeature(v, "SH10-" + c_sfqp2_e + sfqp1, sLabel, 1.0, bAdd, w);
					// the starting characters of two consecutive words
					_sc += addFeature(v, "SH11-" + c_sfqp1_b + c0, sLabel, 1.0, bAdd, w);
					// the ending characters of two consecutive words
					_sc += addFeature(v, "SH12-" + c_sfqp2_e + c_sfqp1_e, sLabel, 1.0, bAdd, w);
					// word length with previous word
					_sc += addFeature(v, "SH13-" + sfqp2 + ln_sfqp1s, sLabel, 1.0, bAdd, w);
					// word length with next word
					_sc += addFeature(v, "SH14-" + ln_sfqp2s + sfqp1, sLabel, 1.0, bAdd, w);
					
					// tag and word
					_sc += addFeature(v, "SH15-" + sfqp1 + SEP + spqp1, sLabel, 1.0, bAdd, w);
					
					if (!m_bLemmaFilter || ln_sfqp1 < 3)
					{
						_sc += addFeature(v, "SH19-" + spqp2 + SEP + sfqp1, sLabel, 1.0, bAdd, w);
						_sc += addFeature(v, "SH20-" + sfqp1 + SEP + spqp1 + c_sfqp2_e, sLabel, 1.0, bAdd, w);
						_sc += addFeature(v, "SH21-" + sfqp1 + SEP + spqp1 + c0, sLabel, 1.0, bAdd, w);
					}
					if (ln_sfqp1 == 1)
						_sc += addFeature(v, "SH22-" + c_sfqp2_e + sfqp1 + c0 + spqp1, sLabel, 1.0, bAdd, w);
					_sc += addFeature(v, "SH24-" + spqp1 + c_sfqp1_e, sLabel, 1.0, bAdd, w);
					
					for (int i = 0; i < sfqp1.length() - 1; ++i)
						_sc += addFeature(v, "SH27-" + spqp1 + sfqp1.charAt(i) + c_sfqp1_e, sLabel, 1.0, bAdd, w);

					String sCat = m_dict.getCharType(c_sfqp1_e.charAt(0));
					for (int i = 0; i < sfqp1.length() - 1; ++i)
						_sc += addFeature(v, "SH29-" + spqp1 + sfqp1.charAt(i) + SEP + sCat, sLabel, 1.0, bAdd, w);

					if (m_bCharType)
					{
						_sc += addFeature(v, "SH90-" + (s0.sent.charTypeAt(curidx) == 4 ? "1" : "0"), sLabel, 1.0, bAdd, w);
						_sc += addFeature(v, "SH91-" + s0.sent.charTypeAt(curidx - 1) + SEP + s0.sent.charTypeAt(curidx), sLabel, 1.0, bAdd, w);
						_sc += addFeature(v, "SH92-" + s0.sent.charTypeAt(curidx - 2) + SEP + s0.sent.charTypeAt(curidx - 1) + SEP + s0.sent.charTypeAt(curidx), sLabel, 1.0, bAdd, w);
					}
					
					/* Evaluate partial score and put it to cache */
					
					sc += _sc;
					if (cache != null && !bAdd)
						cache.wordScore().put(s0, _sc);
				}
			}
			
			/* POS tagging features */
			{
				String sLabel = "";
				String spqf1 = act.getTag();
				
				sc += addFeature(v, "SH16-" + spqp1 + SEP + spqf1, sLabel, 1.0, bAdd, w);
				sc += addFeature(v, "SH17-" + spqp2 + SEP + spqp1 + SEP + spqf1, sLabel, 1.0, bAdd, w);
				if (!m_bLemmaFilter || ln_sfqp1 < 3)
					sc += addFeature(v, "SH18-" + sfqp1 + SEP + spqf1, sLabel, 1.0, bAdd, w);
				sc += addFeature(v, "SH23-" + spqf1 + c0, sLabel, 1.0, bAdd, w);
				sc += addFeature(v, "SH25-" + spqf1 + c0, "", 1.0, bAdd, w); // shared with append action
				sc += addFeature(v, "SH30-" + spqf1 + c0 + SEP + spqp1 + c_sfqp1_e, sLabel, 1.0, bAdd, w);
			}
		}

		return new Pair<IntFeatVector, Double>(v, sc);
	}

}
