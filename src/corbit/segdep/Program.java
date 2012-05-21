package corbit.segdep;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import corbit.commons.Vocab;
import corbit.commons.dict.CTB5TagDictionary;
import corbit.commons.dict.CTB7TagDictionary;
import corbit.commons.dict.TagDictionary;
import corbit.commons.io.CTBReader;
import corbit.commons.io.Console;
import corbit.commons.io.MaltReader;
import corbit.commons.io.ParseReader;
import corbit.commons.util.GlobalConf;

class Program
{
	static void initializeExperimentalConf(GlobalConf conf)
	{}

	static boolean evalExperimentalOpt(GlobalConf conf, int i, List<String> lArgs)
	{
		boolean bFound = false;
		for (String s : conf.getConfs())
		{
			if (lArgs.get(i).equals("--" + s))
			{
				lArgs.remove(i);
				bFound = true;
				conf.setConf(s, true);
				break;
			}
		}
		return bFound;
	}

	static boolean evalCommonOpt(SRParser parser, int i, List<String> lArgs) throws IOException
	{
		if (lArgs.get(i).equals("--no-parse"))
		{
			lArgs.remove(i);
			parser.m_bParse = false;
			parser.m_bAlignArcChar = false;
			parser.m_bSingleBeam = true;
		}
		else if (lArgs.get(i).equals("--tagset"))
		{
			lArgs.remove(i);
			parser.initTagDictionary(lArgs.get(i));
			lArgs.remove(i);
		}
		else if (lArgs.get(i).equals("--no-feature-cache"))
		{
			lArgs.remove(i);
			parser.m_bUseFeatureCache = false;
		}
		else if (lArgs.get(i).equals("--gold-seg"))
		{
			lArgs.remove(i);
			parser.m_bAssignGoldSeg = true;
		}
		else if (lArgs.get(i).equals("--gold-tag"))
		{
			lArgs.remove(i);
			parser.m_bAssignGoldTag = true;
			parser.m_bAssignGoldSeg = true;
		}
		else if (lArgs.get(i).equals("--no-average"))
		{
			lArgs.remove(i);
			parser.m_bAveraged = false;
		}
		else if (lArgs.get(i).equals("-b") || lArgs.get(i).equals("--beam"))
		{
			lArgs.remove(i);
			parser.m_iBeam = Integer.parseInt(lArgs.get(i));
			lArgs.remove(i);
		}
		else if (lArgs.get(i).equals("--input-format"))
		{
			lArgs.remove(i);
			parser.m_inputFileFormat = ParseReader.Format.valueOf(lArgs.get(i).toUpperCase());
			lArgs.remove(i);
		}
		else if (lArgs.get(i).equals("--show-output"))
		{
			lArgs.remove(i);
			parser.m_bShowOutput = true;
		}
		else if (lArgs.get(i).equals("--infreq-as-oov"))
		{
			lArgs.remove(i);
			parser.m_bInfreqAsOOV = true;
		}
		else if (lArgs.get(i).equals("--no-compress"))
		{
			lArgs.remove(i);
			parser.m_bCompressedModel = false;
		}
		else if (lArgs.get(i).equals("--show-stats"))
		{
			lArgs.remove(i);
			parser.m_bShowStats = true;
		}
		else if (lArgs.get(i).equals("--parallel"))
		{
			lArgs.remove(i);
			parser.m_iParallel = Integer.parseInt(lArgs.get(i));
			lArgs.remove(i);
		}
		else if (lArgs.get(i).equals("--gold-arc"))
		{
			lArgs.remove(i);
			parser.m_bGoldArc = true;
		}
		else if (lArgs.get(i).equals("--limit"))
		{
			lArgs.remove(i);
			parser.m_iLimitSent = Integer.parseInt(lArgs.get(i));
			lArgs.remove(i);
		}
		else if (lArgs.get(i).equals("--wordlist"))
		{
			lArgs.remove(i);
			String[] sFiles = lArgs.get(i).split("[:;,]");
			lArgs.remove(i);
			/* if not loaded from a model (i.e. for training) */
			if (parser.m_wordlists == null)
				parser.m_wordlists = new Vocab[sFiles.length];
			/*
			 * All word list files to use must be specified for training. 
			 * For Test and Run, you can replace some of the word lists loaded from a model, 
			 * by partially specifying the file names, such as in "--wordlist :::file4"
			 */
			for (int j = 0; j < sFiles.length; ++j)
				if (sFiles[j].length() > 0)
					parser.m_wordlists[j] = new Vocab(sFiles[j]);
		}
		else if (lArgs.get(i).equals("--preproc"))
		{
			lArgs.remove(i);
			parser.loadPreprocWords(lArgs.get(i));
			lArgs.remove(i);
		}
		else
			return false;
		return true;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		Console.open();
		List<String> lArgs = new LinkedList<String>(java.util.Arrays.asList(args));

		SRParser parser = new SRParser();
		GlobalConf conf = GlobalConf.getInstance();
		initializeExperimentalConf(conf);

		if (lArgs.size() < 2)
			usage();
		else if (lArgs.get(0).equals("Train"))
		{
			if (lArgs.size() < 5) usage();

			String sDictFile = null;
			int iDictThreshold = 0;
			boolean bResume = false;
			boolean bPrintParams = false;

			for (int i = 5; i < lArgs.size();)
			{
				if (lArgs.get(i).equals("--dict"))
				{
					lArgs.remove(i);
					sDictFile = lArgs.get(i);
					lArgs.remove(i);
					iDictThreshold = Integer.parseInt(lArgs.get(i));
					lArgs.remove(i);
				}
				else if (lArgs.get(i).equals("--print-params"))
				{
					lArgs.remove(i);
					bPrintParams = true;
				}
				else if (lArgs.get(i).equals("--load"))
				{
					lArgs.remove(i);
					parser.loadModel(lArgs.get(i));
					lArgs.remove(i);
				}
				else if (lArgs.get(i).equals("--resume"))
				{
					lArgs.remove(i);
					bResume = true;
				}
				else if (lArgs.get(i).equals("--parser-weight"))
				{
					lArgs.remove(i);
					parser.m_dParserWeight = Double.parseDouble(lArgs.get(i));
					lArgs.remove(i);
				}
				else if (lArgs.get(i).equals("--no-early"))
				{
					lArgs.remove(i);
					parser.m_bEarlyUpdate = false;
				}
				else if (lArgs.get(i).equals("--no-average"))
				{
					lArgs.remove(i);
					parser.m_bAveraged = false;
				}
				else if (lArgs.get(i).equals("--batch"))
				{
					lArgs.remove(i);
					parser.m_bLoadOnMemory = false;
				}
				else if (lArgs.get(i).equals("--no-dp"))
				{
					lArgs.remove(i);
					parser.m_bDP = false;
				}
				else if (lArgs.get(i).equals("--save-each"))
				{
					lArgs.remove(i);
					parser.m_bSaveEach = true;
				}
				else if (lArgs.get(i).equals("--no-lemma-filter"))
				{
					lArgs.remove(i);
					parser.m_bLemmaFilter = false;
				}
				else if (lArgs.get(i).equals("--char-type"))
				{
					lArgs.remove(i);
					parser.m_bCharType = true;
				}
				else if (lArgs.get(i).equals("--delay"))
				{
					lArgs.remove(i);
					parser.m_bEvalDelay = true;
				}
				else if (lArgs.get(i).equals("--use-trie"))
				{
					lArgs.remove(i);
					parser.m_bUseTrie = true;
					parser.setUseTrie();
				}
				else if (lArgs.get(i).equals("--no-rebuild-vocab"))
				{
					lArgs.remove(i);
					parser.m_bRebuildVocab = false;
				}
				else if (lArgs.get(i).equals("--no-shuffle"))
				{
					lArgs.remove(i);
					parser.m_bShuffle = false;
				}
				else if (evalCommonOpt(parser, i, lArgs))
					;
				else if (evalExperimentalOpt(conf, i, lArgs))
					;
				else if (lArgs.get(i).startsWith("-"))
				{
					System.err.println("Unknown option: " + lArgs.get(i));
					System.exit(-1);
				}
				else
					++i;
			}

			if (lArgs.size() != 5) usage();

			String sModelFile = lArgs.get(4);
			String sLastModelFile = sModelFile + ".last";
			if ((new File(sModelFile)).exists())
			{
				if (bResume)
				{
					System.err.println("Resuming: all the specified options will be neglected.");
					if ((new File(sLastModelFile)).exists())
						parser.loadModel(sLastModelFile);
					else
						parser.loadModel(sModelFile);
				}
				else
					System.err.println(sModelFile + " already exists. Will be overwritten.");
			}
			if (sDictFile == null) usage("Error: dictionary file must be specified.", true);
			if (sDictFile != null && !bResume)
				parser.loadDictFromFile(sDictFile, iDictThreshold);
			if (bPrintParams)
				parser.printProperties();
			parser.train(lArgs.get(1), lArgs.get(2), sModelFile, Integer.parseInt(lArgs.get(3)));
		}
		else if (lArgs.get(0).equals("Test"))
		{
			if (lArgs.size() < 3)
				usage();
			else
			{
				parser.loadModel(lArgs.get(1));

				String sParseFile = null;
				boolean bPrintParams = false;
				String sDictFile = null;
				int iDictThreshold = 0;
				for (int i = 3; i < lArgs.size();)
				{
					if (lArgs.get(i).equals("--dict"))
					{
						lArgs.remove(i);
						sDictFile = lArgs.get(i);
						lArgs.remove(i);
						if (i == lArgs.size()) usage("Error: threshold missing for --dict option", true);
						iDictThreshold = Integer.parseInt(lArgs.get(i));
						lArgs.remove(i);
					}
					else if (lArgs.get(i).equals("--save-pos"))
					{
						lArgs.remove(i);
						sParseFile = lArgs.get(i);
						lArgs.remove(i);
						parser.m_bSaveOnlyPos = true;
					}
					else if (lArgs.get(i).equals("--save-parse"))
					{
						lArgs.remove(i);
						sParseFile = lArgs.get(i);
						lArgs.remove(i);
						parser.m_bSaveOnlyPos = false;
					}
					else if (lArgs.get(i).equals("--print-params"))
					{
						lArgs.remove(i);
						bPrintParams = true;
					}
					else if (evalCommonOpt(parser, i, lArgs))
						;
					else if (evalExperimentalOpt(conf, i, lArgs))
						;
					else if (lArgs.get(i).startsWith("-"))
					{
						Console.writeLine("Unknown option: " + lArgs.get(i));
						System.exit(-1);
					}
					else
						++i;
				}
				if (bPrintParams)
					parser.printProperties();
				// TENTATIVE: for use of frequency map in dictionary
				if (sDictFile != null)
					parser.loadDictFromFile(sDictFile, iDictThreshold);
				parser.test(lArgs.get(2), sParseFile);
			}
		}
		else if (lArgs.get(0).equals("Run"))
		{
			if (lArgs.size() < 2)
				usage();
			else
			{
				parser.loadModel(lArgs.get(1));
				// parser.printDictFeatureWeights();
				boolean bPrintParams = false;
				String sDictFile = null;
				int iDictThreshold = 0;
				for (int i = 2; i < lArgs.size();)
				{
					if (lArgs.get(i).equals("--print-params"))
					{
						lArgs.remove(i);
						bPrintParams = true;
					}
					else if (lArgs.get(i).equals("--dict"))
					{
						lArgs.remove(i);
						sDictFile = lArgs.get(i);
						lArgs.remove(i);
						if (i == lArgs.size()) usage("Error: threshold missing for --dict option", true);
						iDictThreshold = Integer.parseInt(lArgs.get(i));
						lArgs.remove(i);
					}
					else if (evalCommonOpt(parser, i, lArgs))
						;
					else if (lArgs.get(i).startsWith("-"))
					{
						Console.writeLine("Unknown option: " + lArgs.get(i));
						System.exit(-1);
					}
					else
						++i;
				}
				if (bPrintParams) parser.printProperties();
				if (sDictFile != null) parser.loadDictFromFile(sDictFile, iDictThreshold);
				System.err.println("Ready.");
				parser.run();
			}
			System.err.println("done.");
		}
		else if (lArgs.get(0).equals("CreateDict"))
		{
			if (lArgs.size() < 4)
				usage();
			else
			{
				String sType = lArgs.get(1);
				if (sType.equals("ctb5"))
				{
					ParseReader pr = new CTBReader(lArgs.get(2), new CTB5TagDictionary(true));
					TagDictionary.createCountDict(pr, lArgs.get(3), CTB5TagDictionary.ssCtbTags);
				}
				else if (sType.equals("ctb7"))
				{
					ParseReader pr = new CTBReader(lArgs.get(2), new CTB7TagDictionary(true));
					TagDictionary.createCountDict(pr, lArgs.get(3), CTB7TagDictionary.ssCtbTags);
				}
				else
					throw new IllegalArgumentException("Unknown type: " + sType);
			}
		}
		else if (lArgs.get(0).equals("PrintWeights"))
		{
			parser.loadModel(lArgs.get(1));
			parser.printWeights();
		}
		else if (lArgs.get(0).equals("MaltToDep"))
		{
			if (lArgs.size() < 3) usage();
			MaltReader.maltToDep(lArgs.get(1), lArgs.get(2), new CTB7TagDictionary(true));
		}
		else
			usage();

		Console.close();
	}

	static void usage()
	{
		// System.err.println("Corbit, a Chinese text analyzer  -  Copyright (c) 2010-2012, Jun Hatori");
		System.err.println("Usage: ./corbit.sh <sp|spd> <Run|Train|Test> (arguments..)");
		System.err.println();
		System.err.println("Run (model-file-to-load) [options..] < (input-file) > (output-file)");
		System.err.println("  --print-params       print the list of model and program parameters");
		System.err.println();
		System.err.println("Train (train-file) (dev-file) (#iteration) (model-file-to-save) --dict (dict-file) (threshold) [options..]");
		System.err.println("  --dict (file) (int)  use the file (made using CreateDict command) as a POS tag dictionary");
		System.err.println("                       and set the threshold frequency to use the set of POS tags in the dictionary");
		System.err.println("                       This option is a required argument for sp and spd model.");
		System.err.println("  --tagset <ctb5|ctb7> specify the tag set (default: ctb5)");
		System.err.println("  --input-format <malt|ctb>");
		System.err.println("                       specify the format of input data");
		System.err.println("  --beam-size,-b (int) set the beam size");
		System.err.println("  --wordlist (file1[:file2:..])");
		System.err.println("                       specify word list files (each line corresponds to one word) to be used in dictionary features");
		System.err.println("  --preproc (file)     specify a word list (each line corresponds to one word), which will be pre-segmented");
		System.err.println("  --delay              use delayed features (default: disabled)");
		System.err.println("  --parser-weight (w)  set the weight of the parsing features (default: 0.5)");
		System.err.println("  --char-type          use character-type features (recommended for out-domain evaluation)");
		System.err.println("  --no-lemma-filter    use all lexical information (default: only words with >2 frequencies)");
		System.err.println("  --no-shuffle         disable shuffling of training instances");
		System.err.println("  --use-trie           use Patricia trie to store the feature index (default: HashMap)");
		System.err.println("  --load (file)        load a model file and continue training");
		System.err.println("  --resume             resume training if a temporary file is availale");
		System.err.println("  --save-each          always save the model file of the last iteration so it can be used to resume training");
		System.err.println("  --no-early           disable early update");
		System.err.println("  --no-parse           disable dependency parsing (i.e. segmentation and POS tagging only)");
		System.err.println("  --no-average         disable averaged perceptron (i.e. non-averaged perceptron)");
		System.err.println("  --no-dp              disable dynamic programming as described in Huang and Sagae (2010)");
		System.err.println();
		System.err.println("  (options below can also be used for Test command)");
		System.err.println("  --parallel (num)     specified the number of CPUs to use for decoding");
		System.err.println("  --print-params       print the list of model and program parameters");
		System.err.println("  --show-stats         show statistics during decoding");
		System.err.println("  --show-output        print outputs");
		System.err.println();
		System.err.println("Test (model-file-to-load) (test-file) [options..]");
		System.err.println("  --save-pos (file)    save the output POS tags to the file");
		System.err.println("  --save-parse (file)  save the output POS tags and parse trees to the file");
		System.err.println();
		// System.err.println("CreateDict <ctb5|ctb7> (target-file) (dict-file-to-save)");
	}

	static void usage(String message, boolean quit)
	{
		System.out.println(message);
		System.out.println();
		usage();
		if (quit) System.exit(-1);
	}
}
