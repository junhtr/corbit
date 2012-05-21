package corbit.segdep;

import java.io.BufferedOutputStream;
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import corbit.commons.Vocab;
import corbit.commons.dict.CTB5TagDictionary;
import corbit.commons.dict.CTB7TagDictionary;
import corbit.commons.dict.TagDictionary;
import corbit.commons.io.Console;
import corbit.commons.io.FileEnum;
import corbit.commons.io.ParseReader;
import corbit.commons.ml.AveragedWeight;
import corbit.commons.ml.WeightVector;
import corbit.commons.util.Stopwatch;

public class SRParserModel
{
	protected final Vocab m_fvocab;
	protected AveragedWeight m_weight;
	protected TagDictionary m_dict;
	protected Vocab[] m_wordlists;
	protected Set<String> m_preprocWords;

	/*
	 * beginning of main
	 */

	public SRParserModel()
	{
		m_fvocab = new Vocab();
		m_weight = new AveragedWeight();
		m_wordlists = null;

		m_preprocWords = new HashSet<String>();
		m_preprocWords.add(" "); // half-width space
		m_preprocWords.add("　"); // full-width space

		initTagDictionary(0);
	}

	public void initTagDictionary(int iTagSet)
	{
		m_iTagSet = iTagSet;
		m_dict = iTagSet == 0 ? new CTB5TagDictionary(true) : new CTB7TagDictionary(true);
	}

	public void initTagDictionary(String sTagSet)
	{
		if (sTagSet.equals("ctb5"))
			initTagDictionary(0);
		else if (sTagSet.equals("ctb7"))
			initTagDictionary(1);
		else
			throw new IllegalArgumentException(sTagSet);
	}

	public void printWeights()
	{
		for (String s: m_fvocab.getKeys())
			Console.writeLine(s + "\t" + m_weight.get(m_fvocab.get(s)));
	}

	public void setUseTrie()
	{
		m_fvocab.setUseTrie();
	}

	public void loadDictFromFile(String sFile, int iThreshold) throws IOException
	{
		m_dict.clear();
		m_dict.loadFromFile(sFile, iThreshold);
	}

	public void loadPreprocWords(String sFile) throws IOException
	{
		FileEnum fe = new FileEnum(sFile);
		int nLine = 0;
		for (String s: fe)
		{
			m_preprocWords.add(s);
			++nLine;
		}
		fe.shutdown();
		System.err.println(nLine + " words to pre-segment loaded.");
	}

	public void saveModel(String sFile) throws IOException
	{
		Stopwatch sw = new Stopwatch("Saving model...");
		sw.start();

		/*
		 * create a temporary file to write
		 */
		
		File ftmp = File.createTempFile("~segdep", ".model", new File("."));
		ftmp.deleteOnExit();

		/*
		 * save model components
		 */

		OutputStream os = m_bCompressedModel ?
				new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(ftmp))) :
				new BufferedOutputStream(new FileOutputStream(ftmp));
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
		saveProperties(pw);
		m_fvocab.save(pw);
		pw.println(m_dict != null);
		os.flush();
		if (m_dict != null)
			m_dict.saveToStream(pw);
		m_weight.save(pw);
		pw.println(m_wordlists == null ? 0 : m_wordlists.length);
		if (m_wordlists != null)
			for (int i = 0; i < m_wordlists.length; ++i)
				m_wordlists[i].save(pw);
		pw.close();

		/*
		 * rename the temporary file
		 */

		File fout = new File(sFile);
		if (fout.exists())
			fout.delete();
		if (!ftmp.renameTo(fout))
			throw new IOException("Failed to rename the temporary file: " + ftmp.getName());
		sw.lap();
	}

	public void loadModel(String sFile) throws IOException, ClassNotFoundException
	{
		System.err.println("Loading model..");
		InputStream is = m_bCompressedModel ?
				new GZIPInputStream(new FileInputStream(sFile)) :
				new FileInputStream(sFile);
		BufferedReader sr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		loadProperties(sr);
		if (m_bUseTrie)
			setUseTrie();
		m_fvocab.load(sr);
		if (Boolean.parseBoolean(sr.readLine()) == true)
			m_dict.loadFromStream(sr);
		m_weight.load(sr);
		int nWordLists = Integer.parseInt(sr.readLine());
		if (nWordLists > 0)
		{
			m_wordlists = new Vocab[nWordLists];
			for (int i = 0; i < nWordLists; ++i)
				m_wordlists[i] = new Vocab(sr);
		}
		sr.close();
	}

	public void printDictFeatureWeights()
	{
		if (m_wordlists != null)
		{
			WeightVector w = m_weight.getAveragedWeight();
			for (int i = 0; i < m_wordlists.length; ++i)
			{
				for (int j = 1; j <= 32; ++j)
				{
					for (String s: new String[] { "DD00", "DD01", "DN00", "DN01" })
					{
						String sName = s + "-" + (i + 1) + "-" + j + "-";
						if (m_fvocab.contains(sName))
							System.out.println(sName + "\t" + w.get(m_fvocab.get(sName)));
					}
				}
			}
		}
	}

	/*
	 * beginning of parameter part
	 */

	static final int m_numOpts;

	static {
		int n = 0;
		for (Field f: SRParserModel.class.getDeclaredFields())
			if (isParameterToSave(f))
				++n;
		m_numOpts = n;
	}

	private static boolean isParameterToSave(Field f)
	{
		return (!Modifier.isTransient(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())
				&& f.getName().startsWith("m_")
				&& (f.getType() == int.class || f.getType() == boolean.class || f.getType() == double.class));
	}

	/*
	 * model parameters to save
	 */

	int m_iBeam = 8;
	int m_iTagSet = 0; // 0: CTB5, 1: CTB7

	double m_dParserWeight = 0.5d;

	boolean m_bAveraged = true;
	boolean m_bParse = true;
	boolean m_bEarlyUpdate = true;
	boolean m_bAssignGoldSeg = false;
	boolean m_bAssignGoldTag = false;
	boolean m_bDP = true;
	boolean m_bEvalDelay = false;
	boolean m_bAlignArcChar = true;
	boolean m_bLemmaFilter = true;
	boolean m_bSingleBeam = false;
	boolean m_bUseTrie = false;
	boolean m_bShuffle = true;
	boolean m_bValidateTag = true;
	boolean m_bCharType = false;

	/*
	 * training status (used to resume training)
	 */

	int m_iTrainIteration = 0;
	int m_iTrainBestIteration = 0;
	double m_dTrainBestScore = Double.NEGATIVE_INFINITY;

	/*
	 * program options (not to be saved; must be declared with transient)
	 */

	transient int m_iParallel = 1;
	transient ParseReader.Format m_inputFileFormat = ParseReader.Format.MALT;
	transient boolean m_bShowOutput = false;
	transient boolean m_bLoadOnMemory = true;
	transient boolean m_bSaveEach = false;
	transient boolean m_bCheckOpts = false;
	transient boolean m_bCompressedModel = true;
	transient boolean m_bSaveOnlyPos = false;
	transient boolean m_bShowStats = false;
	transient boolean m_bRebuildVocab = false;
	transient boolean m_bInfreqAsOOV = false;
	transient boolean m_bUseFeatureCache = true;

	transient int m_iLimitSent = 0; // affect the result
	transient boolean m_bGoldArc = false; // affect the result

	private void saveProperties(PrintWriter pw)
	{
		try
		{
			for (Field f: SRParserModel.class.getDeclaredFields())
				if (isParameterToSave(f))
					pw.println(f.getName().substring(2) + " = " + f.get(this));
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		pw.println("EOP");
	}

	private void loadProperties(BufferedReader br) throws NumberFormatException, IOException
	{
		String sKey = null;
		String sVal = null;
		try
		{
			int iNumOpts = 0;
			String sLine;
			while (!(sLine = br.readLine()).equals("EOP"))
			{
				++iNumOpts;
				String[] ss = sLine.split("=");
				sKey = ss[0].trim();
				sVal = ss[1].trim();

				Field f = SRParserModel.class.getDeclaredField("m_" + sKey);
				Class<?> type = f.getType();
				if (type == int.class)
					f.setInt(this, Integer.parseInt(sVal));
				else if (type == boolean.class)
					f.setBoolean(this, Boolean.parseBoolean(sVal));
				else if (type == double.class)
					f.setDouble(this, Double.parseDouble(sVal));
				else
					throw new RuntimeException("Unknown parameter type: " + type);
			}
			if (iNumOpts != m_numOpts)
			{
				if (m_bCheckOpts)
					throw new RuntimeException("There are some missing model parameters. Probably, the model is not compatible.");
				else
					Console.writeLine("There are some missing model parameters. Probably, the model is not compatible.");
			}
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchFieldException e)
		{
			if (m_bCheckOpts)
				throw new RuntimeException("Cannot recognize model parameter: " + sKey);
			else
				Console.writeLine("Cannot recognize model parameter: " + sKey);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}

	}

	void printProperties()
	{
		try
		{
			System.err.println("[program options]");
			for (Field f: SRParserModel.class.getDeclaredFields())
				if (Modifier.isTransient(f.getModifiers()))
					System.err.println(f.getName().substring(2) + " = " + f.get(this));
			System.err.println();
			System.err.println("[model parameters]");
			for (Field f: SRParserModel.class.getDeclaredFields())
				if (isParameterToSave(f) && !f.getName().startsWith("m_iTrain") && !f.getName().startsWith("m_dTrain"))
					System.err.println(f.getName().substring(2) + " = " + f.get(this));
			System.err.println();
			System.err.println("[training status]");
			for (Field f: SRParserModel.class.getDeclaredFields())
				if (isParameterToSave(f) && (f.getName().startsWith("m_iTrain") || f.getName().startsWith("m_dTrain")))
					System.err.println(f.getName().substring(2) + " = " + f.get(this));
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
	}
}
