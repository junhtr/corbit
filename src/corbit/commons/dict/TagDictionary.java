package corbit.commons.dict;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import corbit.commons.io.ParseReader;
import corbit.commons.util.Statics;
import corbit.commons.util.StepCounter;
import corbit.commons.word.IndexWord;
import corbit.commons.word.ParsedSentence;

public abstract class TagDictionary implements Serializable
{
	private static final long serialVersionUID = 8161161836265425026L;

	// maxWordLength used to build arrays; increase this value if an exception occured
	protected static final int maxWordLength = 128;

	protected final String[] m_tagList;
	protected final String[] m_openTagList;
	protected final String[] m_closedTagList;
	protected final String[] m_arcLabels;

	// store indices of all tags
	protected final Map<String,Integer> m_tagIndex;

	// store the list of closed tags
	protected final Set<String> m_closedTagSet;

	// store the maximum length of words for each tag
	protected final int[] m_tagWordLength;

	// store the list of open tags for each word length
	protected String[][] m_openTagsByLen;

	// store all (open and closed) tags of frequent words
	protected Map<String,String[]> m_freqTagDict;

	// store all (open and closed) tags of infrequent words
	protected Map<String,String[]> m_infreqTagDict;

	// store closed tags of infrequent words
	protected Map<String,String[]> m_closedTagDict;

	// store the type of each character
	protected Map<Character,String> m_charTypeDict;

	// tentative (not saved)
	protected Map<String,Integer> m_frequencies;

	private static final String[] emptyStrArray = new String[0];

	// constructor
	protected TagDictionary(
			final String[] openTagList,
			final String[] closedTagList,
			final String[] tagList,
			final String[] arcLabels)
	{
		m_openTagList = openTagList;
		m_closedTagList = closedTagList;
		m_tagList = tagList;
		m_arcLabels = arcLabels;

		m_closedTagSet = closedTagList != null ? new LinkedHashSet<String>() : null;
		if (m_closedTagSet != null)
			for (int i = 0; i < closedTagList.length; ++i)
				m_closedTagSet.add(closedTagList[i]);

		m_tagIndex = new LinkedHashMap<String,Integer>();
		for (int i = 0; i < tagList.length; ++i)
			m_tagIndex.put(tagList[i], i);

		m_freqTagDict = new HashMap<String,String[]>();
		m_infreqTagDict = new HashMap<String,String[]>();
		m_closedTagDict = new HashMap<String,String[]>();
		m_charTypeDict = new HashMap<Character,String>();
		m_frequencies = new HashMap<String,Integer>();
		m_tagWordLength = new int[tagList.length];
		m_openTagsByLen = new String[maxWordLength][m_tagIndex.size()];
	}

	public abstract Set<String> generateTagSet();

	public abstract String[] getArcLabels();

	public void clear()
	{
		m_freqTagDict.clear();
		m_infreqTagDict.clear();
		m_closedTagDict.clear();
		m_charTypeDict.clear();
		m_frequencies.clear();
		Arrays.fill(m_tagWordLength, 0);
		for (int i = 0; i < m_openTagsByLen.length; ++i)
			Statics.fillArray(m_openTagsByLen[i], null);
	}

	public boolean isFrequent(String sForm)
	{
		return m_freqTagDict.containsKey(sForm);
	}

	public boolean hasSeen(String sForm)
	{
		return m_freqTagDict.containsKey(sForm) || m_infreqTagDict.containsKey(sForm);
	}

	public boolean hasSeen(String sForm, String sPos)
	{
		if (m_freqTagDict.containsKey(sForm))
			return Statics.arrayContains(m_freqTagDict.get(sForm), sPos);
		else if (m_infreqTagDict.containsKey(sForm))
			return Statics.arrayContains(m_infreqTagDict.get(sForm), sPos);
		else
			return false;
	}

	public String[] getSeenTags(String sForm)
	{
		if (m_freqTagDict.containsKey(sForm))
			return m_freqTagDict.get(sForm);
		else if (m_infreqTagDict.containsKey(sForm))
			return m_infreqTagDict.get(sForm);
		else
			return emptyStrArray;
	}

	public String getCharType(char c)
	{
		if (m_charTypeDict.containsKey(c))
			return m_charTypeDict.get(c);
		else
			return "$";
	}

	public String[] getLabelCandidates()
	{
		return m_arcLabels;
	}

	public boolean validateTagForChunk(String chunk, String tag)
	{
		if (m_freqTagDict.containsKey(chunk))
		{
			for (String s: m_freqTagDict.get(chunk))
				if (s.equals(tag))
					return true;
			return false;
		}
		else if (m_closedTagSet != null && m_closedTagSet.contains(tag))
		{
			if (m_closedTagDict.containsKey(chunk))
				for (String s: m_closedTagDict.get(chunk))
					if (s.equals(tag))
						return true;
			if (chunk.length() > m_tagWordLength[m_tagIndex.get(tag)])
				return false;
			for (String s: m_openTagList)
				if (s.equals(tag))
					return true;
			return false;
		}
		else
		{
			boolean bLoaded = m_freqTagDict.size() > 0;
			return (!bLoaded || chunk.length() <= m_tagWordLength[m_tagIndex.get(tag)]);
		}
	}

	public boolean validateTagForSequence(String seq, String tag, int startLength)
	{
		for (int i = startLength; i <= Math.min(seq.length(), maxWordLength); ++i)
			for (String s: getTagCandidates(seq.substring(0, i)))
				if (tag.equals(s))
					return true;
		return false;
	}

	public String[] getTagCandidatesForSequence(String seq)
	{
		boolean bLoaded = m_freqTagDict.size() > 0;
		if (!bLoaded)
			return m_openTagList;
		else
		{
			Set<String> tags = new TreeSet<String>();
			for (int i = 1; i <= Math.min(seq.length(), maxWordLength); ++i)
				for (String s: getTagCandidates(seq.substring(0, i)))
					if (!tags.contains(s))
						tags.add(s);
			return tags.toArray(new String[0]);
		}
	}

	/**
	 * Returns the candidate tags for the given word.
	 * 
	 * 1. If the given word is in the frequent word dictionary, it returns the
	 * list of tags stored for each frequent word. 2. Else if the word has any
	 * observed closed tags, it returns the union of all open tags and the
	 * observed closed tags. 3. Otherwise, it returns the list of open tags that
	 * are valid for the input word length.
	 */
	public String[] getTagCandidates(String sForm)
	{
		if (m_freqTagDict.containsKey(sForm))
			return m_freqTagDict.get(sForm);
		else if (m_closedTagSet != null && m_closedTagDict.containsKey(sForm))
		{
			String[] ssClosed = m_closedTagDict.get(sForm);
			String[] ss = Statics.mergeArray(m_openTagsByLen[sForm.length() - 1], ssClosed);
//			String[] ss = Statics.mergeArray(m_openTagList, ssClosed);
			// m_tagDict.put(sForm, ss); // if you want to cache this list
			return ss;
		}
		else
		{
			return m_openTagsByLen[sForm.length() - 1];
//			return m_openTagList;
		}
	}

	public String[] getTagCandidates(String sForm, int iLength)
	{
		boolean bLoaded = m_freqTagDict.size() > 0;
		List<String> ls = new ArrayList<String>();
		for (String sTag: getTagCandidates(sForm))
			if (!bLoaded || iLength <= m_tagWordLength[m_tagIndex.get(sTag)])
				ls.add(sTag);
		return ls.toArray(new String[0]);
	}

	public String[] getTagList()
	{
		return m_tagList;
	}

	public int getTagIndex(String sPos)
	{
		return m_tagIndex.containsKey(sPos) ? m_tagIndex.get(sPos) : -1;
	}

	public int getTagCount()
	{
		return m_tagIndex.size();
	}

	public Set<String> getTagSet()
	{
		return m_tagIndex.keySet();
	}

	public int getFrequency(String sWord)
	{
		if (m_frequencies.containsKey(sWord))
			return m_frequencies.get(sWord);
		else
			return 0;
	}

	public static int getMaxWordLength()
	{
		return maxWordLength;
	}

	public void listOpenTagsByLength()
	{
		List<Set<String>> _openTagsByLen = new ArrayList<Set<String>>(maxWordLength);
		for (int i = 0; i < maxWordLength; ++i)
			_openTagsByLen.add(new HashSet<String>());
		for (String t: m_openTagList)
		{
			int l = m_tagWordLength[m_tagIndex.get(t)];
			if (l > maxWordLength)
				throw new RuntimeException("maxWordLength set too small. A word of length " + l + " is found.");
			for (int i = 0; i < l; ++i)
				_openTagsByLen.get(i).add(t);
		}
		m_openTagsByLen = new String[maxWordLength][m_tagIndex.size()];
		for (int i = 0; i < maxWordLength; ++i)
			m_openTagsByLen[i] = _openTagsByLen.get(i).toArray(new String[0]);
	}

	public void loadFromFile(String sFile, int iThreshold) throws IOException
	{
		clear();

		String sLine;
		BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(sFile), "UTF-8"));

		List<String> lsAllTags = new ArrayList<String>();
		List<String> lsClosedTags = new ArrayList<String>();
		List<String> lsFreqTags = new ArrayList<String>();

		while ((sLine = sr.readLine()) != null)
		{
			sLine = Statics.trimSpecial(sLine);
			String[] p = sLine.split("\t");
			String sForm = p[0];

			lsAllTags.clear();
			lsClosedTags.clear();
			lsFreqTags.clear();

			int iTotalCount = 0;
			int iMaxCount = Integer.MIN_VALUE;
			String sMaxTag = null;

			// read a word entry
			for (int i = 1; i < p.length; ++i)
			{
				String[] pp = p[i].split(":");
				String sTag = pp[0];
				int iCount = Integer.parseInt(pp[1]);
				if (iCount > 0)
				{
					iTotalCount += iCount;

					// list open and closed tags of the word
					lsAllTags.add(sTag);
					if (iCount > 2)
						lsFreqTags.add(sTag);
					if (m_closedTagSet != null && m_closedTagSet.contains(sTag))
						lsClosedTags.add(sTag);

					// update the most-frequent tag
					if (iCount > iMaxCount)
					{
						iMaxCount = iCount;
						sMaxTag = sTag;
					}

					// update longest word length information if necessary
					int iLen = sForm.length();
					if (!m_tagIndex.containsKey(sTag))
						throw new RuntimeException("Unknown POS '" + sTag + "' is found. Review the tag set setting.");
					int iIdx = m_tagIndex.get(sTag);
					if (iLen > m_tagWordLength[iIdx])
						m_tagWordLength[iIdx] = iLen;
				}
			}

			m_frequencies.put(sForm, iTotalCount);

			if (iTotalCount > 0)
			{
				String[] allTags = lsAllTags.toArray(new String[0]);
				String[] closedTags = lsClosedTags.toArray(new String[0]);

				if (sForm.length() == 1 && sMaxTag != null)
				{
					String sType = iTotalCount >= 20 ? Statics.strJoin(lsFreqTags, ":") : Statics.strJoin(lsAllTags, ":");
					m_charTypeDict.put(sForm.charAt(0), sType);
				}

				if (iTotalCount >= iThreshold)
				{
					m_freqTagDict.put(sForm, allTags);
				}
				else
				{
					m_infreqTagDict.put(sForm, allTags);
					if (lsClosedTags.size() > 0)
						m_closedTagDict.put(sForm, closedTags);
				}
			}
		}
		sr.close();

		listOpenTagsByLength();

		System.err.println(m_frequencies.size() + " dictionary entries loaded from " + sFile + " with threshold of " + iThreshold + ".");

	}

	public static void createCountDict(ParseReader pr, String sSaveFile, String[] tags) throws IOException
	{
		Map<String,Integer> tagIndex = new HashMap<String,Integer>();
		for (String s: tags)
			tagIndex.put(s, tagIndex.size());

		Map<String,Map<String,Integer>> dict = new LinkedHashMap<String,Map<String,Integer>>();
		StepCounter sc = new StepCounter();

		while (pr.hasNext())
		{
			ParsedSentence sent = pr.next();
			if (sent == null)
				continue;
			for (IndexWord t: sent)
			{
				if (!tagIndex.containsKey(t.tag))
					throw new RuntimeException("Unknown POS: " + t.tag);
				// update tag count
				if (!dict.containsKey(t.form))
					dict.put(t.form, new TreeMap<String,Integer>());
				Statics.increment(dict.get(t.form), t.tag);
			}
			sc.increment();
		}
		pr.shutdown();

		PrintWriter sw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sSaveFile), "UTF-8"));
		for (String s: dict.keySet())
		{
			Map<String,Integer> lp = dict.get(s);
			sw.print(s + "\t");
			for (Entry<String,Integer> p: lp.entrySet())
				sw.print(p.getKey() + ":" + p.getValue() + "\t");
			sw.println();
		}
		sw.close();
	}

	private static final String m_prefFreqTags = "@@F@_";
	private static final String m_prefInfreqTags = "@@I@_";
	private static final String m_prefClosedTags = "@@C@_";
	private static final String m_prefWordLength = "@@L@_";
	private static final String m_prefCharType = "@@T@_";

	public void loadFromStream(BufferedReader sr) throws NumberFormatException, IOException
	{
		clear();

		int n = Integer.parseInt(sr.readLine());
		for (int i = 0; i < n; ++i)
		{
			String sLine = Statics.trimSpecial(sr.readLine());
			String[] p = sLine.split("\t");

			// read max word length information
			if (p[0].startsWith(m_prefWordLength))
			{
				String sTag = p[0].substring(m_prefWordLength.length());
				m_tagWordLength[m_tagIndex.get(sTag)] = Integer.parseInt(p[1]);
				continue;
			}

			// read character-type dictionary
			if (p[0].startsWith(m_prefCharType))
			{
				m_charTypeDict.put(p[0].substring(m_prefCharType.length()).charAt(0), p[1]);
				continue;
			}

			// read tag dictionaries
			String[] lp = new String[p.length - 1];
			for (int j = 1; j < p.length; ++j)
			{
				String[] pp = p[j].split(":");
				lp[j - 1] = pp[0];
			}
			if (p[0].startsWith(m_prefFreqTags))
				m_freqTagDict.put(p[0].substring(m_prefFreqTags.length()), lp);
			else if (p[0].startsWith(m_prefInfreqTags))
				m_infreqTagDict.put(p[0].substring(m_prefInfreqTags.length()), lp);
			else if (p[0].startsWith(m_prefClosedTags))
				m_closedTagDict.put(p[0].substring(m_prefClosedTags.length()), lp);
		}

		listOpenTagsByLength();

		System.err.println(n + " dictionary entries loaded from the model.");
	}

	public void saveToStream(PrintWriter sw)
	{
		int numLines = 0;
		numLines += m_freqTagDict.size();
		numLines += m_infreqTagDict.size();
		numLines += m_closedTagDict.size();
		numLines += m_tagIndex.size();
		numLines += m_charTypeDict.size();

		sw.println(numLines);
		for (Entry<String,String[]> e: m_freqTagDict.entrySet())
		{
			sw.print(m_prefFreqTags + e.getKey());
			sw.print("\t");
			String[] ss = e.getValue();
			for (int i = 0; i < ss.length; ++i)
				sw.print(ss[i] + ":" + "\t");
			sw.println();
		}
		for (Entry<String,String[]> e: m_infreqTagDict.entrySet())
		{
			sw.print(m_prefInfreqTags + e.getKey());
			sw.print("\t");
			String[] ss = e.getValue();
			for (int i = 0; i < ss.length; ++i)
				sw.print(ss[i] + ":" + "\t");
			sw.println();
		}
		for (Entry<String,String[]> e: m_closedTagDict.entrySet())
		{
			sw.print(m_prefClosedTags + e.getKey());
			sw.print("\t");
			String[] ss = e.getValue();
			for (int i = 0; i < ss.length; ++i)
				sw.print(ss[i] + ":" + "\t");
			sw.println();
		}
		for (Entry<String,Integer> e: m_tagIndex.entrySet())
			sw.println(m_prefWordLength + e.getKey() + "\t" + m_tagWordLength[e.getValue()]);
		for (Entry<Character,String> e: m_charTypeDict.entrySet())
			sw.println(m_prefCharType + e.getKey() + "\t" + e.getValue());

		System.err.println(numLines + " dictionary entries saved to the model.");
	}

}
