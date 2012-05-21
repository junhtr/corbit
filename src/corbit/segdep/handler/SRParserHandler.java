package corbit.segdep.handler;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import corbit.commons.Vocab;
import corbit.commons.dict.TagDictionary;
import corbit.commons.ml.IntFeatVector;
import corbit.commons.ml.WeightVector;
import corbit.commons.transition.SDAction;
import corbit.commons.util.Pair;
import corbit.commons.util.Statics;
import corbit.commons.word.DepChunk;
import corbit.commons.word.DepChunkTree;
import corbit.segdep.SRParserState;
import corbit.segdep.handler.DelayedFeature.DelayedFeatureType;
import corbit.segdep.transition.SRParserTransition;

public abstract class SRParserHandler
{
	protected final Vocab m_fvocab;
	protected Set<String> m_flist;
	protected TagDictionary m_dict;
	protected Vocab[] m_wordlists;

	protected static final String SEP = "-";
	protected static final String OOR = "$";
	protected static final String NA = "NA";

	public Vocab getVocabulary()
	{
		return m_fvocab;
	}

	public void setFeatureList(Set<String> v)
	{
		m_flist = v;
	}

	public void setTagDictionary(TagDictionary t)
	{
		m_dict = t;
	}

	public void setWordList(Vocab[] v)
	{
		m_wordlists = v;
	}

	public SRParserHandler(Vocab vocab)
	{
		m_fvocab = vocab;
		m_flist = null;
		m_dict = null;
		m_wordlists = null;
	}

	/*
	 *  abstract section
	 */

	public interface AtomicFeatures
	{
		public int hashCode();

		public boolean equals(Object o);

	}

	public abstract AtomicFeatures getAtomicFeatures(SRParserState s);

	public abstract Pair<IntFeatVector,Double> getFeatures(SRParserState s, SDAction act, List<DelayedFeature> vd, boolean bAdd, WeightVector w, SRParserTransition.SRParserCache cache);

	/*
	 *  utility functions
	 */

	protected static final String getPosArgString(int idx)
	{
		return "@@@POS[" + idx + "]";
	}

	protected static final String getPosArgPrefix()
	{
		return "@@@POS[";
	}

	protected static final String getFormArgString(int idx)
	{
		return "@@@FORM[" + idx + "]";
	}

	protected static final String getFormArgPrefix()
	{
		return "@@@FORM[";
	}

	protected double evaluateDelayedFeatures(IntFeatVector vn, /*Linked*/List<DelayedFeature> vd,
			int idxbgn, int woffset, String sForm, String sPos, boolean bAdd, WeightVector w)
	{
		double score = 0.0;
		List<DelayedFeature> _vd = new LinkedList<DelayedFeature>();

		// loop must be in the reverse order to avoid problems in removing actions
		for (int i = vd.size() - 1; i >= 0; --i)
		{
			DelayedFeature df = vd.get(i);

			boolean bFillForm = sForm != null && df.hasArgument(idxbgn, woffset, DelayedFeatureType.LEX);
			boolean bFillPos = sPos != null && df.hasArgument(idxbgn, woffset, DelayedFeatureType.POS);

			if (bFillForm || bFillPos)
			{
				vd.remove(i);
				if (bFillForm)
					df = df.fill(woffset, DelayedFeatureType.LEX, sForm);
				if (bFillPos)
					df = df.fill(woffset, DelayedFeatureType.POS, sPos);
			}
			else
				continue;

			// move the feature to the normal feature vector if its arguments are filled in;
			// or otherwise put it back to the delayed feature vector
			if (df.filled())
				score += addFeature(vn, df.compile(), df.value, bAdd, w);
			else
				_vd.add(df);
		}
		vd.addAll(_vd);

		return score;
	}

	protected double evaluateDelayedFeatures(IntFeatVector vn, /*Linked*/List<String> vd, int curidx, String sForm, String sPos, boolean bAdd, WeightVector w)
	{
		final String sPosArg = getPosArgString(curidx);
		final String sPosPref = getPosArgPrefix();
		final String sFormArg = getFormArgString(curidx);
		final String sFormPref = getFormArgPrefix();

		double score = 0.0;

		// loop must be in the reverse order to avoid problems in removing actions
		for (int i = vd.size() - 1; i >= 0; --i)
		{
			String sTemplate = vd.get(i);

			// fill in corresponding arguments with the the given form and tag
			if (sForm != null && sTemplate.indexOf(sPosArg) != -1 ||
					sPos != null && sTemplate.indexOf(sFormArg) != -1)
				vd.remove(i);
			else
				continue;

			if (sPos != null)
				while (sTemplate.indexOf(sPosArg) != -1)
					sTemplate = Statics.strReplace(sTemplate, sPosArg, sPos);

			if (sForm != null)
				while (sTemplate.indexOf(sFormArg) != -1)
					sTemplate = Statics.strReplace(sTemplate, sFormArg, sForm);

			// move the feature to the normal feature vector if its arguments are filled in;
			// or otherwise put it back to the delayed feature vector
			if (sTemplate.indexOf(sPosPref) == -1 && sTemplate.indexOf(sFormPref) == -1)
				score += addFeature(vn, sTemplate, 1.0d, bAdd, w);
			else
				vd.add(sTemplate);
		}

		return score;
	}

	protected double addFeature(IntFeatVector v, String sFeature, double dValue, boolean bAdd, WeightVector w)
	{
		if (bAdd)
		{
			int idx = m_fvocab.getIndex(sFeature);
			if (v.containsKey(idx))
				v.put(idx, v.get(idx) + dValue);
			else
				v.put(idx, dValue);
			return w.getWithCheck(idx) * dValue;
		}
		else
		{
			Integer idx = m_fvocab.getBoxed(sFeature);
			if (idx != null)
				return w.get(idx) * dValue;
			else
				return 0.0;
		}
	}

	protected double addFeature(IntFeatVector v, String sFeature, String sLabel, double dValue, boolean bAdd, WeightVector w)
	{
		return addFeature(v, sFeature + SEP + sLabel, dValue, bAdd, w);
	}

	protected static int[] getValencies(DepChunkTree w)
	{
		int[] vals = new int[2];
		int iCenter = w.begin;
		for (int i = 0; i < w.children.size(); ++i)
		{
			DepChunkTree wc = w.children.get(i);
			if (wc.begin < iCenter)
				++vals[0];
			else
				++vals[1];
		}
		return vals;
	}

	protected static int getDistance(DepChunk w1, DepChunk w2)
	{
		if (w1.begin == -1 || w2.begin == -1)
			return -1;
		else
		{
			int dist = Math.abs(w2.begin - w1.begin);

			if (dist > 30)
				dist = 30;
			else if (dist > 25)
				dist = 25;
			else if (dist > 20)
				dist = 20;
			else if (dist > 15)
				dist = 15;
			else if (dist > 12)
				dist = 12;

			return dist;
		}
	}

	protected static DepChunkTree getLeftmostChild(DepChunkTree w)
	{
		int cidx = Integer.MAX_VALUE;

		for (int i = 0; i < w.children.size(); ++i)
		{
			int idx = w.children.get(i).begin;
			if (idx < cidx && idx < w.begin)
				cidx = i;
		}

		return cidx == Integer.MAX_VALUE ? null : w.children.get(cidx);
	}

	protected static DepChunkTree getRightmostChild(DepChunkTree w)
	{
		int cidx = -1;

		for (int i = 0; i < w.children.size(); ++i)
		{
			int idx = w.children.get(i).begin;
			if (idx > cidx && idx > w.begin)
				cidx = i;
		}

		return cidx == -1 ? null : w.children.get(cidx);
	}

	private static String findPunctFromDescendants(DepChunkTree t)
	{
		if ("PU".equals(t.tag))
			return t.form;
		else
		{
			for (DepChunkTree _t: t.children)
			{
				String s = findPunctFromDescendants(_t);
				if (s != null)
					return s;
			}
		}
		return null;
	}

	protected static String getPunctInBetween(DepChunkTree tl, DepChunkTree tr)
	{
		int left = tl.end;
		int right = tr.begin;

		for (int i = 0; i < tl.children.size(); ++i)
		{
			DepChunkTree t = tl.children.get(i);
			if (t.begin >= left)
			{
				String s = findPunctFromDescendants(t);
				if (s != null)
					return s;
			}
		}
		for (int i = 0; i < tr.children.size(); ++i)
		{
			DepChunkTree t = tr.children.get(i);
			if (t.end <= right)
			{
				String s = findPunctFromDescendants(t);
				if (s != null)
					return s;
			}
		}
		return NA;
	}

}
