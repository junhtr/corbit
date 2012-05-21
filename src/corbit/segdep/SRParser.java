package corbit.segdep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import corbit.commons.SRParserEvaluator;
import corbit.commons.dict.TagDictionary;
import corbit.commons.io.CTBReader;
import corbit.commons.io.Console;
import corbit.commons.io.MaltReader;
import corbit.commons.io.ParseReader;
import corbit.commons.io.ParseWriter;
import corbit.commons.ml.AveragedWeight;
import corbit.commons.ml.IntFeatVector;
import corbit.commons.ml.WeightVector;
import corbit.commons.transition.SDAction;
import corbit.commons.util.Pair;
import corbit.commons.util.Statics;
import corbit.commons.util.StepCounter;
import corbit.commons.util.Stopwatch;
import corbit.commons.word.IndexWord;
import corbit.commons.word.ParsedSentence;
import corbit.commons.word.UnsegmentedSentence;
import corbit.segdep.handler.SRParserCharBasedHandler;
import corbit.segdep.handler.SRParserHandler;
import corbit.segdep.transition.SRParserCharBasedTransition;
import corbit.segdep.transition.SRParserTransition;
import corbit.segdep.transition.SRParserTransition.SRParserTransitionParameter;

public class SRParser extends SRParserModel
{
	private class ParseResult
	{
		final SRParserState parsedState;
		final SRParserState goldState;
		final SRParserState refState;
		final boolean bGoldSurvive;

		ParseResult(SRParserState so, SRParserState sg, SRParserState sr, boolean b)
		{
			parsedState = so;
			goldState = sg;
			refState = sr;
			bGoldSurvive = b;
		}
	}

	private final class SentenceParser
	{
		private final SRParserTransition trans;
		private final SRParserHandler handler;
		private final SRParserStateGenerator generator;
		private final WeightVector weight;

		private final SRParserStats stats;
		private final ExecutorService execMove;
		private final boolean bParallelMove;
		private final boolean bTrain;

		private SentenceParser(boolean bTrain)
		{
			this.bTrain = bTrain;

			stats = new SRParserStats();
			handler = new SRParserCharBasedHandler(m_fvocab, m_bParse, m_bLemmaFilter, m_bCharType, m_dParserWeight);
			handler.setTagDictionary(m_dict);
			if (m_wordlists != null) handler.setWordList(m_wordlists);
			generator = new SRParserStateGenerator(handler, m_bDP, m_bEvalDelay);
			weight = (!bTrain && m_bAveraged) ? m_weight.getAveragedWeight() : m_weight;
			SRParserTransitionParameter params = new SRParserTransitionParameter(
					m_bAssignGoldSeg, m_bAssignGoldTag, m_bGoldArc, m_bParse, m_bUseFeatureCache, m_bValidateTag);
			trans = new SRParserCharBasedTransition(generator, handler, weight, m_dict, stats, params, m_bAlignArcChar);

			bParallelMove = bTrain && m_iParallel > 1;
			execMove = bParallelMove ? Executors.newCachedThreadPool() : null;
		}

		private SRParserStats stats()
		{
			return stats;
		}

		private IntFeatVector getPrefixFeatures(SRParserState s)
		{
			return trans.getPrefixFeatures(s);
		}

		private void shutdown()
		{
			if (execMove != null) execMove.shutdown();
		}

		private SRParserTransition.Decision[] preprocess(UnsegmentedSentence sent)
		{
			if (m_preprocWords == null || sent.length() < 2) return null;

			final int maxWordLength = TagDictionary.getMaxWordLength();
			final int length = sent.length();
			SRParserTransition.Decision[] decision = new SRParserTransition.Decision[length + 1];

			// TODO: Tentative inefficient implementation

			for (int i = 0; i < length; ++i)
			{
				for (int l = 1; l <= Math.min(maxWordLength, length - i); ++l)
				{
					if (m_preprocWords.contains(sent.substring(i, i + l)))
					{
						if (decision[i] != SRParserTransition.Decision.IN_WORD)
							decision[i] = SRParserTransition.Decision.SEGMENT;
						for (int k = i + 1; k < i + l; ++k)
							decision[k] = SRParserTransition.Decision.IN_WORD;
						if (decision[i + l] != SRParserTransition.Decision.IN_WORD)
							decision[i + l] = SRParserTransition.Decision.SEGMENT;
					}
				}
			}

			return Arrays.copyOfRange(decision, 1, length);
		}

		public ParseResult parseSentence(UnsegmentedSentence sent, ParsedSentence gsent)
		{
			SRParserTransition.Decision[] decision = preprocess(sent);

			/*
			 * Initialization
			 */

			SRParserState sg = generator.create(sent, decision);
			SRParserState sr = generator.create(sent, decision);

			int iLastStep = m_bAlignArcChar ? 2 * sent.length() : sent.length();
			int iNumChart = iLastStep + 1;

			DPParserChart[] charts1 = new DPParserChart[iNumChart];
			DPParserChart[] charts2 = new DPParserChart[iNumChart];

			for (int i = 0; i < iNumChart; ++i)
			{
				charts1[i] = new DPParserChart(m_bDP, generator, 0.0d);
				charts2[i] = new DPParserChart(m_bDP, generator, 0.0d);
			}

			charts1[0].updateEntry(sr);

			boolean bStopped = false;
			int curstep = 0;
			SRParserState bestHP = null;
			SRParserState sref = null;

			/*
			 * Main loop
			 */
			
			while (true)
			{
				if ((m_bSingleBeam || curstep == sg.curstep) && gsent != null)
				{
					/*
					 * Move forward the gold state 
					 */
					Pair<SDAction, SRParserState> p = trans.moveNextGold(sg, gsent, false);
					sg = p.second;
				}

				/*
				 * Pruning
				 */
				if (m_iBeam > 0)
					charts1[curstep].prune(m_iBeam);

				/*
				 * Proceeds a shift-reduce step
				 */
				ParsedSentence _gsent = (bTrain || m_bShowStats || m_bAssignGoldSeg || m_bAssignGoldTag || m_bGoldArc)
					? gsent : null;
				boolean bResult = bParallelMove ?
						parallelProceedOneStep(charts1, charts2, curstep, trans, weight, _gsent, false, m_bSingleBeam, execMove) :
						proceedOneStep(charts1, charts2, curstep, trans, weight, _gsent, false, m_bSingleBeam);

				assert (m_bSingleBeam || curstep < iLastStep || !bResult);
				
				if (!bResult && curstep == iLastStep) break;

				if (charts2[curstep].size() == 0 && (gsent == null || sg.curstep > curstep)) ++curstep;

				/*
				 * Pruning
				 */
				if (m_iBeam > 0)
				{
					if (!m_bSingleBeam)
						for (int i = 0; i < charts2.length; ++i)
							charts2[i].prune(m_iBeam);
					else
					{
						Pair<SRParserState, SRParserState> p = DPParserChart.horizontalPrune(charts2, curstep, sg.curstep, m_iBeam);
						bestHP = p.first;
						sref = p.second;
					}
				}

				/*
				 * Early update (Collins and Roark (2004))
				 */
				if (bTrain && m_bEarlyUpdate && (m_bSingleBeam || curstep == sg.curstep))
				{
					boolean bFound = false;
					if (!m_bSingleBeam)
						bFound = charts2[curstep].containsGoldState();
					else
					{
						for (int i = 0; i < charts2.length; ++i)
						{
							if (charts2[i].containsGoldState())
							{
								bFound = true;
								break;
							}
						}
					}
					if (!bFound)
					{
						bStopped = true;
						break;
					}
				}

				/*
				 * Swap the chart lists
				 */
				Statics.swap(charts1, charts2);
				for (int i = 0; i < charts2.length; ++i)
					charts2[i].clear();
			}

			DPParserChart nchart = charts2[sg.curstep];
			assert (m_bSingleBeam || nchart.size() > 0);
			SRParserState so = bTrain
					? (m_bSingleBeam ? bestHP : nchart.getBestEntry())
					: charts2[iLastStep].getBestEntry();

			/*
			 * Update statistics
			 */
			stats.numSentencesProcessed++;
			stats.numStatesMerged += nchart.numMergedState();
			stats.numStatesEvaluated += nchart.numTotalState();
			if (bStopped) stats.numSentencesStopped++;

			assert (so != null);
			boolean bGoldSurvive = m_bShowStats && !bTrain && charts2[iLastStep].containsGoldState();
			return new ParseResult(so, sg, sref, bGoldSurvive);
		}
	}

	/*
	 * Beginning of main
	 */
	
	public SRParser()
	{
		super();
	}

	private double parallelIterateOnce(
			final List<ParsedSentence> gsents, final boolean bTrain, String sParseFile) throws IOException
	{
		/*
		 *  Initialize components
		 */
		
		ParseWriter pw = sParseFile != null ? new ParseWriter(sParseFile) : null;
		SRParserEvaluator eval = new SRParserEvaluator(m_dict, m_bParse, false, m_bShowOutput, m_bInfreqAsOOV, m_bShowStats);
		StepCounter cnt = new StepCounter(m_bShowOutput ? 0 : 100);

		/*
		 *  For parallelism
		 */
		
		final boolean bParallelSent = !bTrain && m_iParallel > 1;
		final ExecutorService execSent = bParallelSent ? Executors.newFixedThreadPool(m_iParallel) : null;
		List<Future<ParseResult>> lOuts = bParallelSent ? new ArrayList<Future<ParseResult>>() : null;
		final int iParallelSent = bParallelSent ? !bTrain ? gsents.size() : m_iParallel : 1;

		final SentenceParser handler = new SentenceParser(bTrain);
		SRParserStats stats = handler.stats();
		
		for (int iPhase = 0; iPhase < Math.ceil((float)gsents.size() / (float)iParallelSent); ++iPhase)
		{
			/*
			 * Queue a group of parallel threads
			 */

			if (lOuts != null) lOuts.clear();

			boolean bLastLoop = false;
			ParseResult result = null;
			
			for (int iSent = iPhase * iParallelSent; iSent < (iPhase + 1) * iParallelSent && iSent < gsents.size(); ++iSent)
			{
				assert (bParallelSent || result == null);

				final ParsedSentence gsent = gsents.get(iSent);
				final UnsegmentedSentence sent = createSentenceToProcess(gsent);
				
				if (bParallelSent)
					lOuts.add(execSent.submit(new Callable<ParseResult>() {
						public ParseResult call() { return handler.parseSentence(sent, gsent); }
					}));
				else
					result = handler.parseSentence(sent, gsent);
				
				if (m_iLimitSent > 0 && iSent >= m_iLimitSent)
				{
					bLastLoop = true;
					break;
				}
			}

			/*
			 * Synchronize a thread group to process results 
			 */

			IntFeatVector vdTotal = bTrain ? new IntFeatVector() : null;
			final int iTasks = bParallelSent ? lOuts.size() : 1;
			
			for (int i = 0; i < iTasks; ++i)
			{
				int iSent = iPhase * iParallelSent + i;
				
				if (bParallelSent)
					try { result = lOuts.get(i).get(); }
				catch (InterruptedException e)
					{ e.printStackTrace(); System.err.println("error found at line " + iSent + " . skipped."); continue; }
				catch (ExecutionException e)
					{ e.printStackTrace(); System.err.println("error found at line " + iSent + " . skipped."); continue; }
		
				SRParserState so = result.parsedState;
				SRParserState sg = result.goldState;
				SRParserState sr = result.refState;
				
				if (so == null)
					{ System.err.println("no states remaining at line " + iSent + " . skipped."); continue; }
				
				ParsedSentence osent = SRParserState.getParsedResult(so);
				ParsedSentence gsent = gsents.get(iSent);
	
				/*
				 * Evaluate the parsed output and show statistics
				 */
				if (pw != null) pw.writeParse(osent, gsent, false, !m_bParse || m_bSaveOnlyPos);
//				eval.evalAction(so, sg);
				boolean bResult = eval.evalSentence(osent, gsent);
	
				if (bTrain && !bResult)
				{
					IntFeatVector vg = handler.getPrefixFeatures(sg);
					IntFeatVector vo = handler.getPrefixFeatures(sr == null ? so : sr);
					IntFeatVector vd = IntFeatVector.subtract(vg, vo);
					vdTotal.append(vd);
				}
				
				/*
				 * Update statistics
				 */
				if (!bTrain && m_bShowStats)
				{
					boolean bHigher = sg.scprf >= so.scprf;
					boolean bInBeam = result.bGoldSurvive;
					if (bHigher && !bInBeam) stats.numGoldOutOfBeamHigher++;
					if (!bHigher && !bInBeam) stats.numGoldOutOfBeamLower++;
					if (bHigher && bInBeam) stats.numGoldInBeamHigher++;
					if (!bHigher && bInBeam) stats.numGoldInBeamLower++;
				}
				
				stats.totalScore += so.scprf;
				
				cnt.increment();
			}

			/*
			 * Perceptron update
			 */
			if (bTrain)
			{
				if (vdTotal.size() > 0)
					m_weight.append(vdTotal);
				for (int i = 0; i < iTasks; ++i)
					m_weight.nextStep();
			}
			
			if (bLastLoop) break;
		}

		cnt.dispose();
		if (execSent != null) execSent.shutdown();
		handler.shutdown();
		if (pw != null) pw.dispose();

		if (m_bShowStats)
		{
			stats.print();
			
			// eval.evalPosConfusion();
			
			Console.writeLine("#GoldOutOfBeamHigher: " + stats.numGoldOutOfBeamHigher);
			Console.writeLine("#GoldOutOfBeamLower:  " + stats.numGoldOutOfBeamLower);
			Console.writeLine("#GoldInBeamHigher:    " + stats.numGoldInBeamHigher);
			Console.writeLine("#GoldInBeamLower:     " + stats.numGoldInBeamLower);
		}

		return eval.evalTotal();
	}

	public boolean parallelProceedOneStep(
			DPParserChart[] charts1,
			DPParserChart[] charts2,
			int curstep,
			final SRParserTransition trans,
			WeightVector w,
			final ParsedSentence gsent,
			final boolean bAdd,
			final boolean bSingleBeam,
			ExecutorService exec)
	{
		boolean bAnyUpdated = false;
		List<Future<List<Pair<SDAction, SRParserState>>>> outs = new ArrayList<Future<List<Pair<SDAction, SRParserState>>>>();

		if (!bSingleBeam)
			for (int i = curstep + 1; i < charts1.length; ++i)
				Statics.swap(charts1, charts2, i);

		int begin = bSingleBeam ? 0 : curstep;
		int end = bSingleBeam ? charts1.length : curstep + 1;

		for (int i = begin; i < end; ++i)
		{
			for (final SRParserState s : charts1[i].keySet())
			{
				outs.add(exec.submit(new Callable<List<Pair<SDAction, SRParserState>>>()
				{
					public List<Pair<SDAction, SRParserState>> call()
					{
						List<Pair<SDAction, SRParserState>> l = new ArrayList<Pair<SDAction, SRParserState>>();
						for (Pair<SDAction, SRParserState> p : trans.moveNext(s, gsent, bAdd))
							l.add(p);
						return l;
					}
				}));
			}
		}

		try
		{
			for (int i = 0; i < outs.size(); ++i)
			{
				List<Pair<SDAction, SRParserState>> l = outs.get(i).get();
				for (int j = 0; j < l.size(); ++j)
				{
					Pair<SDAction, SRParserState> p = l.get(j);
					SDAction sk = p.first;
					SRParserState pv = p.second;
					bAnyUpdated = bAnyUpdated || sk != SDAction.END_STATE;
					charts2[pv.curstep].updateEntry(pv);
				}
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		catch (ExecutionException e)
		{
			e.printStackTrace();
		}

		return bAnyUpdated;
	}

	public boolean proceedOneStep(
			DPParserChart[] charts1,
			DPParserChart[] charts2,
			int curstep,
			SRParserTransition trans,
			WeightVector w,
			ParsedSentence gsent,
			boolean bAdd,
			boolean bSingleBeam)
	{
		if (!bSingleBeam)
			for (int i = curstep + 1; i < charts1.length; ++i)
				Statics.swap(charts1, charts2, i);

		boolean bAnyUpdated = false;

		int begin = bSingleBeam ? 0 : curstep;
		int end = bSingleBeam ? charts1.length : curstep + 1;

		for (int i = begin; i < end; ++i)
		{
			for (SRParserState s : charts1[i].keySet())
			{
				for (Pair<SDAction, SRParserState> p : trans.moveNext(s, gsent, bAdd))
				{
					SDAction sk = p.first;
					SRParserState pv = p.second;
					bAnyUpdated = bAnyUpdated || sk != SDAction.END_STATE;
					charts2[pv.curstep].updateEntry(pv);
				}
			}
		}

		return bAnyUpdated;
	}

	public void test(String sTestFile, String sParseFile) throws IOException
	{
		List<ParsedSentence> lt = loadCorpus(sTestFile);
		Stopwatch sw = new Stopwatch("Evaluating");
		double dScore = parallelIterateOnce(lt, false, sParseFile);
		Console.writeLine("F1 dScore: " + dScore);
		sw.lap();
	}
	
	public void run() throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF8"));
		final SentenceParser sp = new SentenceParser(false);
		
		final ExecutorService execSent = Executors.newFixedThreadPool(m_iParallel);
		List<Future<ParseResult>> lOuts = new ArrayList<Future<ParseResult>>();

		String buf = "";
		
		while (buf != null)
		{
			lOuts.clear();
			int nSent = 0;
			
			do {
				buf = br.readLine();
				if (buf == null) break;
				
				final String _buf = buf;
				lOuts.add(execSent.submit(new Callable<ParseResult>() {
					public ParseResult call() {
						String buf = Normalizer.normalize(_buf, Normalizer.Form.NFKC);
						UnsegmentedSentence sent = new UnsegmentedSentence(buf.toCharArray());
						return sp.parseSentence(sent, null);
					}
				}));
			} while (br.ready() && nSent++ < 10000);
			
			for (int i=0; i<lOuts.size(); ++i)
			{
				try
				{
					ParseResult pr = lOuts.get(i).get();
					ParsedSentence osent = SRParserState.getParsedResult(pr.parsedState);
					if (m_bParse)
					{
						for (IndexWord w : osent)
							Console.writeLine(String.format("%d\t%s\t%s\t%d", w.index, w.form, w.tag, w.head));
						Console.writeLine();
					}
					else
					{
						for (IndexWord iw: osent)
							Console.write(iw.form + "/" + iw.tag + " ");
						Console.writeLine();
					}
				}
				catch (InterruptedException e) { e.printStackTrace(); }
				catch (ExecutionException e) { e.printStackTrace(); }
			}
		}
		
		if (execSent != null) execSent.shutdown();
		sp.shutdown();
	}

	public void train(String sTrainFile, String sDevFile, String sSaveFile, int iMaxPerceptIt) throws IOException
	{
		Stopwatch swTotal = new Stopwatch("Training");

		List<ParsedSentence> lt = loadCorpus(sTrainFile);
		List<ParsedSentence> ld = loadCorpus(sDevFile);
		
		if (m_bShuffle) Statics.shuffle(lt);

		if (++m_iTrainIteration > 1)
			Console.writeLine("resumed from iteration #" + m_iTrainIteration);

		AveragedWeight bestWeight = null;
		Stopwatch sw;

		for (; m_iTrainIteration <= iMaxPerceptIt; ++m_iTrainIteration)
		{
			/*
			 * Training
			 */
			sw = new Stopwatch("Perceptron training: loop " + m_iTrainIteration);
			parallelIterateOnce(lt, true, null);
			sw.lap();
			if (m_bRebuildVocab) m_fvocab.rebuild(m_weight);
			gc();

			/*
			 * Development (to determine the best iteration)
			 */
			sw = new Stopwatch("Evaluating: loop " + m_iTrainIteration);
			double dScore = parallelIterateOnce(ld, false, null);
			sw.lap();
			gc();

			/*
			 * Update the best parameters
			 */
			if (dScore > m_dTrainBestScore)
			{
				m_dTrainBestScore = dScore;
				m_iTrainBestIteration = m_iTrainIteration;
				bestWeight = new AveragedWeight(m_weight);
				saveModel(sSaveFile);
				Console.writeLine(String.format("New score %f achieved at %d-th iteration.", m_dTrainBestScore, m_iTrainBestIteration));
			}
			if (m_bSaveEach)
				saveModel(sSaveFile + ".last");

		}
		m_iTrainIteration = m_iTrainBestIteration;
		m_weight = bestWeight;

		Console.writeLine(String.format("Max score %f achieved at %d-th iteration.", m_dTrainBestScore, m_iTrainBestIteration));
		swTotal.lap();
	}

	private List<ParsedSentence> loadCorpus(String sFile)
	{
		ParseReader ct;
		if (m_inputFileFormat == ParseReader.Format.CTB)
			ct = new CTBReader(sFile, m_dict);
		else if (m_inputFileFormat == ParseReader.Format.MALT)
			ct = new MaltReader(sFile, m_dict);
		else
			throw new IllegalArgumentException("Unsupported format: " + m_inputFileFormat.name());
		
		List<ParsedSentence> lt = new ArrayList<ParsedSentence>();
		for (ParsedSentence p: ct)
			if (p != null) lt.add(p);
		ct.shutdown();
		return lt;
	}
	
	static Runtime runtime = Runtime.getRuntime();

	private void gc()
	{
		long lTotal = runtime.totalMemory() / 1024 / 1024;
		long lFree = runtime.freeMemory() / 1024 / 1024;
		if (m_bShowStats)
			Console.write((String.format("[Before GC] %,3d / %,3d MB ; ", lTotal - lFree, lTotal)));
		runtime.runFinalization();
		runtime.gc();
		Thread.currentThread();
		Thread.yield();
		lTotal = runtime.totalMemory() / 1024 / 1024;
		lFree = runtime.freeMemory() / 1024 / 1024;
		if (m_bShowStats)
			Console.writeLine((String.format("[After GC] %,3d / %,3d MB", lTotal - lFree, lTotal)));
	}

	private UnsegmentedSentence createSentenceToProcess(ParsedSentence gsent)
	{
		StringBuilder sb = new StringBuilder();
		for (IndexWord sw : gsent) sb.append(sw.form);
		UnsegmentedSentence sent = new UnsegmentedSentence(sb.toString().toCharArray());
		return sent;
	}

}
