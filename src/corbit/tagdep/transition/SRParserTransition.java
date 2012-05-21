package corbit.tagdep.transition;

import java.util.List;

import corbit.commons.ml.IntFeatVector;
import corbit.commons.ml.WeightVector;
import corbit.commons.transition.PDAction;
import corbit.commons.util.Pair;
import corbit.tagdep.SRParserState;
import corbit.tagdep.SRParserStateGenerator;
import corbit.tagdep.dict.TagDictionary;
import corbit.tagdep.handler.SRParserHandler;
import corbit.tagdep.word.DepTreeSentence;

public abstract class SRParserTransition
{
	protected boolean m_bParse;
	protected boolean m_bDP;
	protected SRParserStateGenerator m_generator;
	protected SRParserHandler m_fhandler;
	protected WeightVector m_weight;
	protected TagDictionary m_dict;

	protected SRParserTransition(SRParserStateGenerator sg, SRParserHandler fh, WeightVector w, TagDictionary d, boolean bParse)
	{
		m_generator = sg;
		m_fhandler = fh;
		m_weight = w;
		m_dict = d;
		m_bParse = bParse;
	}

	public void setParse(boolean b)
	{
		m_bParse = b;
	}

	public abstract List<Pair<PDAction, SRParserState>> moveNext(SRParserState s, DepTreeSentence gsent, boolean bAdd);

	public abstract Pair<PDAction, SRParserState> moveNextGold(SRParserState s, DepTreeSentence gsent, boolean bAdd);

	public abstract IntFeatVector getPrefixFeatures(SRParserState s);

	public abstract void clear();

	class Transition extends Pair<PDAction, SRParserState>
	{
		public Transition(PDAction act, SRParserState s)
		{
			super(act, s);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null || !(obj instanceof Pair)) return false;
			@SuppressWarnings("rawtypes")
			Pair p = (Pair)obj;
			if (!(p.first instanceof PDAction) || !(p.second instanceof SRParserState)) return false;
			PDAction act = (PDAction)p.first;
			SRParserState s = (SRParserState)p.second;
			return this.first.equals(act) && this.second == s;
		}

		@Override
		public int hashCode()
		{
			return this.first.hashCode() * 17 + this.second.hashCode() + 7;
		}
	}

}
