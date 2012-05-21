package corbit.tagdep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import corbit.commons.Vocab;
import corbit.commons.io.Console;
import corbit.commons.ml.AveragedWeight;
import corbit.commons.ml.IntFeatVector;
import corbit.commons.ml.WeightVector;
import corbit.commons.transition.PDAction;
import corbit.commons.util.Pair;
import corbit.commons.util.Statics;
import corbit.commons.util.StepCounter;
import corbit.commons.util.Stopwatch;
import corbit.tagdep.dict.CTBTagDictionary;
import corbit.tagdep.dict.TagDictionary;
import corbit.tagdep.handler.SRParserCtbHandlerHS10;
import corbit.tagdep.handler.SRParserCtbHandlerZC08;
import corbit.tagdep.handler.SRParserCtbHandlerZN11;
import corbit.tagdep.handler.SRParserHandler;
import corbit.tagdep.io.MaltReader;
import corbit.tagdep.io.CTBReader;
import corbit.tagdep.io.ParseReader;
import corbit.tagdep.io.ParseWriter;
import corbit.tagdep.transition.SRParserTransition;
import corbit.tagdep.transition.SRParserTransitionStd;
import corbit.tagdep.word.DepTree;
import corbit.tagdep.word.DepTreeSentence;

public class SRParser extends SRParserParameters
{
	public enum FeatureType
	{
		HS10, ZC11, ZC08
	}

	final Vocab m_vocab;
	AveragedWeight m_weight;
	SRParserHandler m_fhandler;
	final SRParserStateGenerator m_generator;
	TagDictionary m_dict;
	
	public SRParser()
	{
		super();
		m_vocab = new Vocab();
		m_weight = new AveragedWeight();
		m_fhandler = new SRParserCtbHandlerHS10(m_vocab, this);
		m_generator = new SRParserStateGenerator(m_fhandler, this);
		m_dict = new CTBTagDictionary(m_bUseClosedTags);
	}

	public void setUseClosedTags(boolean b)
	{
		m_dict = new CTBTagDictionary(b);
	}
	
	public void setFeatureHandler(int iType)
	{
		switch (iType)
		{
		case 0:
			m_fhandler = new SRParserCtbHandlerHS10(m_vocab, this);
			break;
		case 1:
			m_fhandler = new SRParserCtbHandlerZN11(m_vocab, this);
			break;
		case 2:
			m_fhandler = new SRParserCtbHandlerZC08(m_vocab, this);
			break;
		default:
			throw new IllegalArgumentException("Illegal feature type: " + iType);
		}
		m_iFeatureType = iType;
		m_generator.setFeatureHandler(m_fhandler);
	}
	
	public void setFeatureHandler(FeatureType ft)
	{
		int iType = -1;
		switch (ft)
		{
		case HS10:
			iType = 0;
			break;
		case ZC11:
			iType = 1;
			break;
		case ZC08:
			iType = 2;
			break;
		}
		setFeatureHandler(iType);
	}

	public void setUseTrie()
	{
		m_vocab.setUseTrie();
	}

	double iterateOnce(String sFile, String sRefFile, boolean bTrain, String sParseFile) throws IOException
	{
		ParseReader ct = m_iInputFormat == 0 ? new MaltReader(sFile) : new CTBReader(sFile);
		List<DepTreeSentence> lt = new ArrayList<DepTreeSentence>();
		for (DepTreeSentence p : ct)
			if (p != null) lt.add(p);
		ct.shutdown();
		
		List<DepTreeSentence> lr = null;
		if (sRefFile != null)
		{
			lr = new ArrayList<DepTreeSentence>();
			ParseReader cr = m_iInputFormat == 0 ? new MaltReader(sFile) : new CTBReader(sRefFile);
			for (DepTreeSentence p : cr)
				if (p != null) lr.add(p);
			cr.shutdown();
		}

		if (bTrain && m_bShuffle)
			Statics.shuffle(lt);

		return parallelIterateOnce(lt, lr, bTrain, sParseFile);
	}

	private final class SentenceParser
	{
		final boolean bTrain;
		final WeightVector w;
		final SRParserTransitionStd trans;
		final boolean bParallelMove;
		final ExecutorService execMove;
		
		SentenceParser(final boolean bTrain, final boolean bParallelMove)
		{
			this.bTrain = bTrain;
			this.bParallelMove = bParallelMove;
			
			w = (!bTrain && m_bAveraged) ? m_weight.getAveragedWeight() : m_weight;
			trans = new SRParserTransitionStd(m_generator, m_fhandler, w, m_dict, m_bParse, m_bAssignPosFollowsShift, m_bAssignGoldPos, m_bShiftWithPos);
			execMove = bParallelMove ? Executors.newFixedThreadPool(bParallelMove ? m_iParallel : 1) : null;
		}
		
		void shutdown()
		{
			if (execMove != null) execMove.shutdown();
		}

		IntFeatVector getPrefixFeatures(SRParserState s)
		{
			return trans.getPrefixFeatures(s);
		}
		
		ParseResult parseSentence(DepTreeSentence gsent)
		{
			// initialization
			DepTreeSentence sent = createSentenceToProcess(gsent);

			SRParserState sg = m_generator.create(sent);
			SRParserState sr = m_generator.create(sent);

			DPParserChart cchart = new DPParserChart(m_bDP, m_generator, m_dMargin);
			DPParserChart nchart = new DPParserChart(m_bDP, m_generator, m_dMargin);

			cchart.updateEntry(sr);

			boolean bStopped = false;
			long lNumNonDPStates = 0;

			// main loop
			while (true)
			{
				// proceed a shift-reduce step of gold derivation
				if (bTrain || m_bShowStats)
					sg = trans.moveNextGold(sg, gsent, false).second;

				// proceed a shift-reduce step
				nchart.clear();
				DepTreeSentence _gsent = (bTrain || m_bShowStats) ? gsent : null;
				boolean bResult = bParallelMove
						? parallelProceedOneStep(cchart, nchart, trans, w, _gsent, false, execMove)
						: proceedOneStep(cchart, nchart, trans, w, _gsent, false);
				if (!bResult) break;
				lNumNonDPStates += nchart.numEvaluatedNonDPState(); // possibly overflow

				// pruning for beam search [Collins 2004]
				if (m_iBeam > 0) nchart.prune(m_iBeam);

				// early update [Collins 2004]
				if (bTrain && m_bEarlyUpdate && isEarlyStop(nchart))
				{
					bStopped = true;
					break;
				}

				// swap the charts and prepare for the next step
				DPParserChart _chart = cchart;
				cchart = nchart;
				nchart = _chart;
			}

			SRParserState so = nchart.getBestEntry();
			int iMergedState = cchart.numMergedState() + nchart.numMergedState();
			int iTotalState = cchart.numTotalState() + nchart.numTotalState();

			return new ParseResult(so, sg, bStopped, iMergedState, iTotalState, lNumNonDPStates);
		}
	}
	
	double parallelIterateOnce(
			final List<DepTreeSentence> gsents, final List<DepTreeSentence> rsents,
			final boolean bTrain, String sParseFile) throws IOException
	{
		ParseWriter pw = sParseFile != null ? new ParseWriter(sParseFile) : null;
		SRParserEvaluator eval = new SRParserEvaluator(m_dict, m_bParse, m_bDebug);

		int iMergedState = 0;
		int iTotalState = 0;
		int iNumStopped = 0;
		long lNumNonDPStates = 0;
		double dTotalScore = 0.0;
		StepCounter cnt = new StepCounter(m_bDebug ? 0 : 100);

		final boolean bParallelMove = bTrain && !m_bAggressiveParallel && m_iParallel > 1;
		final ExecutorService execSent = Executors.newFixedThreadPool(!bParallelMove ? m_iParallel : 1);
		final int iParallel = bTrain ? m_bAggressiveParallel ? m_iParallel : 1 : gsents.size();

		final SentenceParser parser = new SentenceParser(bTrain, bParallelMove);
		List<Future<ParseResult>> lOuts = new ArrayList<Future<ParseResult>>();
		
		try
		{
			for (int iPhase = 0; iPhase < gsents.size() / iParallel + 1; ++iPhase)
			{
				/*
				 * queue a group of parallel threads
				 */

				lOuts.clear();

				for (int iSent = iPhase * iParallel; iSent < (iPhase + 1) * iParallel && iSent < gsents.size(); ++iSent)
				{
					final DepTreeSentence gsent = gsents.get(iSent);
					
					lOuts.add(execSent.submit(new Callable<ParseResult>()
					{
						public ParseResult call()
						{
							return parser.parseSentence(gsent);
						}
					}));
				}

				/*
				 * sequentially process results of a thread group
				 */

				IntFeatVector vdTotal = bTrain ? new IntFeatVector() : null;

				for (int i = 0; i < lOuts.size(); ++i)
				{
					// retrieve the results
					ParseResult result = lOuts.get(i).get();

					SRParserState so = result.parsedState;
					SRParserState sg = result.goldState;
					DepTreeSentence osent = SRParserState.getParsedResult(so);
					DepTreeSentence gsent = gsents.get(iPhase * iParallel + i);
					DepTreeSentence rsent = rsents != null ? rsents.get(iPhase * iParallel + i) : null;
					
					iMergedState += result.numMergedState;
					iTotalState += result.numTotalState;
					lNumNonDPStates += result.numNonDPState;
					if (result.stopped) ++iNumStopped;

					// evaluate the output and show stats
					if (pw != null)
						pw.writeParse(osent, gsent, false, !m_bParse || m_bSaveOnlyPos);
					if (m_bShowStats)
						eval.evalAction(so, sg);
					boolean bResult = (rsent == null) ? eval.evalSentence(osent, gsent) : eval.evalSegmentedSentence(osent, rsent);

					if (bTrain && !bResult)
					{
						IntFeatVector vg = parser.getPrefixFeatures(sg);
						IntFeatVector vo = parser.getPrefixFeatures(so);
						IntFeatVector vd = IntFeatVector.subtract(vg, vo);
						vdTotal.append(vd);
					}

					dTotalScore += so.scprf;
					cnt.increment();
				}

				// perceptron update
				if (bTrain)
				{
					if (vdTotal.size() > 0)
						m_weight.append(vdTotal);
					for (int i = 0; i < lOuts.size(); ++i)
						m_weight.nextStep();
				}
			}
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		catch (ExecutionException e)
		{
			throw new RuntimeException(e);
		}
//		finally
//		{
			execSent.shutdown();
			parser.shutdown();
//		}

		if (pw != null) pw.dispose();

		// evaluation
		{
			double dUpdateP = (double)iNumStopped / (double)cnt.count() * 100.0;

			Console.writeLine();
			Console.writeLine(lNumNonDPStates + " non-DP states evaluated.");
			if (m_bDP)
			{
				double dMergeP = (double)iMergedState / (double)iTotalState * 100.0;
				Console.writeLine(String.format("%d/%d states (%f%%) have been merged.", iMergedState, iTotalState, dMergeP));
			}
			if (bTrain && m_bEarlyUpdate)
				Console.writeLine(String.format("%d/%d sentences (%f%%) have been early updated.", iNumStopped, cnt.count(), dUpdateP));
			if (!bTrain)
			{
				double dAvgScore = dTotalScore / (double)cnt.count();
				Console.writeLine("Average output score: " + dAvgScore);
			}

			if (m_bShowStats)
			{
				eval.evalPosConfusion();
				eval.evalActConfusion();
			}
		}

		return eval.evalTotal();
	}

	public void test(String sTestFile, String sRefFile, String sParseFile) throws IOException
	{
		Stopwatch sw = new Stopwatch("Evaluation");
		double dScore = iterateOnce(sTestFile, sRefFile, false, sParseFile);
		Console.writeLine("F1 dScore: " + dScore);
		sw.lap();
	}

	public void run() throws IOException
	{
		SentenceParser sp = new SentenceParser(false, m_iParallel > 1);
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF8"));

		String buf;
		int n = 0;
		
		main: while ((buf = br.readLine()) != null)
		{
			buf = Statics.trimSpecial(buf);
			if (buf.length() == 0)
			{
				Console.writeLine();
				continue main;
			}
			String[] words = buf.split(" +");
			
			DepTreeSentence sent = new DepTreeSentence();
			
			for (int i = 0; i < words.length; ++i)
			{
				String[] word = words[i].split("/");
				if (m_bAssignGoldPos && word.length < 2)
				{
					System.err.println("POS tags are required when --assign-gold option is used.");
					continue main;
				}
				sent.add(new DepTree(sent, i, word[0], m_bAssignGoldPos && word.length > 1 ? word[1] : null, -2));
			}
			ParseResult pr = sp.parseSentence(sent);
			DepTreeSentence osent = SRParserState.getParsedResult(pr.parsedState);
			for (DepTree dw : osent)
				Console.writeLine(String.format("%d\t%s\t%s\t%d", dw.index, dw.form, dw.pos, dw.head));
			Console.writeLine();
			if (++n % 100 == 0) System.err.println(n + " sentences processed.");
		}
		System.err.println("done.");

		sp.shutdown();
	}
	
	public void train(String sTrainFile, String sDevFile, String sSaveFile, int iMaxPerceptIt) throws IOException
	{
		// main loop
		Stopwatch sw;
		Stopwatch swTotal = new Stopwatch("Training");

		m_iIteration = m_iIteration == 0 ? 1 : m_iIteration + 1;

		if (m_iIteration > 1)
			Console.writeLine("resumed from iteration #" + m_iIteration);

		AveragedWeight bestWeight = null;

		for (; m_iIteration <= iMaxPerceptIt; ++m_iIteration)
		{
			// training
			sw = new Stopwatch("Perceptron training: loop " + m_iIteration);
			iterateOnce(sTrainFile, null, true, null);
			sw.lap();
			if (m_bRebuildVocab)
			{
				sw = new Stopwatch("Rebuilding vocabulary and weights...");
				m_vocab.rebuild(m_weight);
				sw.lap();
			}
			gc();

			// development
			sw = new Stopwatch("Evaluating: loop " + m_iIteration);
			double dScore = iterateOnce(sDevFile, null, false, null);
			sw.lap();
			gc();

			// update the best parameters
			if (dScore > m_dBestScore)
			{
				m_dBestScore = dScore;
				m_iBestIteration = m_iIteration;
				bestWeight = new AveragedWeight(m_weight);
				saveModel(sSaveFile);
				Console.writeLine(String.format("New score %f achieved at %d-th iteration.", m_dBestScore, m_iBestIteration));
			}
			if (m_bSaveEach)
				saveModel(sSaveFile + ".last");

		}
		m_iIteration = m_iBestIteration;
		m_weight = bestWeight;

		Console.writeLine(String.format("Max score %f achieved at %d-th iteration.", m_dBestScore, m_iBestIteration));
		swTotal.lap();
	}

	static Runtime runtime = Runtime.getRuntime();

	void gc()
	{
		// Stopwatch sw = new Stopwatch("Running GC...");
		// sw.start();
		long lTotal = runtime.totalMemory() / 1024 / 1024;
		long lFree = runtime.freeMemory() / 1024 / 1024;
		Console.write((String.format("[Before GC: %,3d / %,3d MB] ", lTotal - lFree, lTotal)));
		runtime.runFinalization();
		runtime.gc();
		Thread.currentThread();
		Thread.yield();
		lTotal = runtime.totalMemory() / 1024 / 1024;
		lFree = runtime.freeMemory() / 1024 / 1024;
		Console.writeLine((String.format("[After GC:  %,3d / %,3d MB]", lTotal - lFree, lTotal)));
		// sw.lap();
	}

	public static boolean parallelProceedOneStep(
			DPParserChart cchart,
			DPParserChart nchart,
			final SRParserTransition trans,
			WeightVector w,
			final DepTreeSentence gsent,
			final boolean bAdd,
			ExecutorService exec)
	{
		boolean bAnyUpdated = false;
		List<Future<Pair<Boolean, List<SRParserState>>>> outs = new ArrayList<Future<Pair<Boolean, List<SRParserState>>>>();

		for (final SRParserState s : cchart.keySet())
			outs.add(exec.submit(new Callable<Pair<Boolean, List<SRParserState>>>()
			{
				public Pair<Boolean, List<SRParserState>> call()
				{
					List<SRParserState> lOut = new ArrayList<SRParserState>();
					boolean bAnyUpdated = false;
					for (Pair<PDAction, SRParserState> p : trans.moveNext(s, gsent, bAdd))
					{
						lOut.add(p.second);
						if (p.first != PDAction.END_STATE)
							bAnyUpdated = true;
					}
					return new Pair<Boolean, List<SRParserState>>(bAnyUpdated, lOut);
				}
			}));

		try
		{
			for (int i = 0; i < outs.size(); ++i)
			{
				Pair<Boolean, List<SRParserState>> p = outs.get(i).get();
				bAnyUpdated = bAnyUpdated || p.first;
				for (SRParserState s : p.second)
					nchart.updateEntry(s);
			}
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		catch (ExecutionException e)
		{
			throw new RuntimeException(e);
		}

		if (nchart.size() == 0)
			throw new RuntimeException("Unexpected error: no next state found.");

		return bAnyUpdated;
	}

	public static boolean proceedOneStep(
			DPParserChart cchart,
			DPParserChart nchart,
			SRParserTransition trans,
			WeightVector w,
			DepTreeSentence gsent,
			boolean bAdd)
	{
		boolean bAnyUpdated = false;
		boolean bEnd = false;

		for (SRParserState s : cchart.keySet())
		{
			for (Pair<PDAction, SRParserState> p : trans.moveNext(s, gsent, bAdd))
			{
				PDAction sk = p.first;
				SRParserState pv = p.second;

				if (sk == PDAction.END_STATE)
					bEnd = true;
				else
					bAnyUpdated = true;

				assert (!bEnd || sk == PDAction.END_STATE);

				nchart.updateEntry(pv);
			}
		}
		if (nchart.size() == 0)
			throw new RuntimeException("Unexpected error: no next state found.");
		return bAnyUpdated;
	}

	static boolean isEarlyStop(DPParserChart chart)
	{
		boolean bFound = false;
		for (SRParserState s : chart.keySet())
		{
			if (s.gold)
			{
				bFound = true;
				break;
			}
		}
		return !bFound;
	}

	DepTreeSentence createSentenceToProcess(DepTreeSentence gsent)
	{
		DepTreeSentence sent = new DepTreeSentence();
		for (DepTree dw : gsent)
			sent.add(new DepTree(sent, dw.index, dw.form, m_bUseGoldPos ? dw.pos : null, -2));
		// sent.add(new DepTree(sent, dw.index, dw.form, null, -2));
		return sent;
	}

	public void loadDictFromFile(String sFile, int iThreshold) throws IOException
	{
		m_dict.clear();
		m_dict.loadFromFile(sFile, iThreshold);
		m_fhandler.setTagDictionary(m_dict);
	}

	public void saveModel(String sFile) throws IOException
	{
		Stopwatch sw = new Stopwatch("Saving model...");
		sw.start();
		File ftmp = File.createTempFile("~segdep", ".model", new File("."));
		ftmp.deleteOnExit();
		OutputStream os = m_bCompressedModel ?
				new GZIPOutputStream(new FileOutputStream(ftmp)) :
				new FileOutputStream(ftmp);
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
		saveProperties(pw);
		m_vocab.save(pw);
		pw.println(m_dict != null);
		if (m_dict != null) m_dict.saveToStream(pw);
		m_weight.save(pw);
		pw.close();
		File fout = new File(sFile);
		if (fout.exists()) fout.delete();
		if (!ftmp.renameTo(fout))
			throw new IOException("Renaming of a temporary file failed.");
		sw.lap();
	}

	public void loadModel(String sFile) throws IOException
	{
		System.err.println("Loading model..");
		InputStream is = m_bCompressedModel ?
				new GZIPInputStream(new FileInputStream(sFile)) :
				new FileInputStream(sFile);
		BufferedReader sr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		loadProperties(sr);
		if (m_bUseTrie) setUseTrie();
		m_vocab.load(sr);
		setFeatureHandler(m_iFeatureType);
		if (Boolean.parseBoolean(sr.readLine()) == true)
		{
			m_dict = new CTBTagDictionary(m_bUseClosedTags);
			m_dict.clear();
			m_dict.loadFromStream(sr);
			m_fhandler.setTagDictionary(m_dict);
		}
		m_weight.load(sr);
		sr.close();
	}

}
