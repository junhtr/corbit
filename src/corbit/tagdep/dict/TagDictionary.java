package corbit.tagdep.dict;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import corbit.commons.util.Statics;
import corbit.commons.util.StepCounter;
import corbit.tagdep.io.ParseReader;
import corbit.tagdep.word.DepTree;
import corbit.tagdep.word.DepTreeSentence;

public abstract class TagDictionary
{
	private static final long serialVersionUID = 8161161836265425026L;

	protected final Map<String, Integer> m_tagIndex;
	protected final String[] m_tagList;
	protected final String[] m_openTagList;
	protected final Set<String> m_closedTagSet;

	// store all (open and closed) tags of words, the frequency of which is above the threshold
	protected Map<String, String[]> m_tagDict;

	// store all tags of words that have been seen in the data
	protected Map<String, String[]> m_allTagDict;

	// store closed tags of words that are not in m_tagDict
	protected Map<String, String[]> m_closedTagDict;

	protected TagDictionary(
			final String[] openTagList,
			final String[] closedTagList,
			final String[] tagList)
	{
		m_openTagList = openTagList;
		m_tagList = tagList;
		m_closedTagSet = closedTagList != null ?
				Collections.unmodifiableSet(new LinkedHashSet<String>()
				{
					private static final long serialVersionUID = 1L;
					{
						for (int i = 0; i < closedTagList.length; ++i)
							add(closedTagList[i]);
					}
				}) : null;
		m_tagIndex = Collections.unmodifiableMap(new LinkedHashMap<String, Integer>()
				{
					private static final long serialVersionUID = 1L;
					{
						for (int i = 0; i < tagList.length; ++i)
							put(tagList[i], i);
					}
				});

		m_tagDict = new HashMap<String, String[]>();
		m_allTagDict = new HashMap<String, String[]>();
		m_closedTagDict = new HashMap<String, String[]>();
	}

	public Set<String> getVocabList()
	{
		return m_tagDict.keySet();
	}

	public void clear()
	{
		m_tagDict.clear();
		m_allTagDict.clear();
		m_closedTagDict.clear();
	}

	public boolean inDictionary(String sForm)
	{
		return m_tagDict.containsKey(sForm);
	}

	public boolean hasSeen(String sForm)
	{
		return m_allTagDict.containsKey(sForm);
	}

	public boolean hasSeen(String sForm, String sPos)
	{
		if (!m_allTagDict.containsKey(sForm))
			return false;
		String[] ss = m_allTagDict.get(sForm);
		boolean found = false;
		for (int i = 0; i < ss.length; ++i)
		{
			if (ss[i].equals(sPos))
			{
				found = true;
				break;
			}
		}
		return found;
	}

	public String[] getSeenTags(String sForm)
	{
		if (m_allTagDict.containsKey(sForm))
			return m_allTagDict.get(sForm);
		else
			return new String[0];
	}

	public String[] getTagCandidates(String sForm)
	{
		if (m_tagDict.containsKey(sForm))
			return m_tagDict.get(sForm);
		else if (m_closedTagSet != null && m_closedTagDict.containsKey(sForm))
		{
			String[] ssClosed = m_closedTagDict.get(sForm);
			String[] ss = Statics.mergeArray(m_openTagList, ssClosed);
			// m_tagDict.put(sForm, ss);
			return ss;
		}
		else
			return m_openTagList;
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
		return m_tagList.length;
	}

	public Set<String> getTagSet()
	{
		return m_tagIndex.keySet();
	}

	public void loadFromFile(String sFile, int iThreshold) throws IOException
	{
		clear();

		String sLine;
		BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(sFile), "UTF-8"));
		List<String> lsOpen = new ArrayList<String>();
		List<String> lsClosed = new ArrayList<String>();
		while ((sLine = sr.readLine()) != null)
		{
			sLine = Statics.trimSpecial(sLine);
			String[] p = sLine.split("\t");
			lsOpen.clear();
			lsClosed.clear();
			int iTotalCount = 0;
			for (int i = 1; i < p.length; ++i)
			{
				String[] pp = p[i].split(":");
				String sTag = pp[0];
				int iCount = Integer.parseInt(pp[1]);
				if (iCount > 0)
				{
					iTotalCount += iCount;
					lsOpen.add(sTag);
					if (m_closedTagSet != null && m_closedTagSet.contains(sTag) && iCount > 0)
						lsClosed.add(sTag);
				}
			}
			String[] openTags = lsOpen.toArray(new String[0]);
			String[] closedTags = lsClosed.toArray(new String[0]);
			if (lsOpen.size() > 0)
				m_allTagDict.put(p[0], openTags);
			if (lsOpen.size() > 0 && iTotalCount >= iThreshold)
				m_tagDict.put(p[0], openTags);
			else if (lsClosed.size() > 0)
				m_closedTagDict.put(p[0], closedTags);
		}
		sr.close();
	}

	public void createCountDict(ParseReader pr, String sSaveFile) throws IOException
	{
		Map<String, Map<String, Integer>> dict = new LinkedHashMap<String, Map<String, Integer>>();
		StepCounter sc = new StepCounter();

		try
		{
			while (pr.hasNext())
			{
				DepTreeSentence sent = pr.next();
				if (sent == null) continue;
				for (DepTree t : sent)
				{
					if (!m_tagIndex.containsKey(t.pos))
						throw new RuntimeException("Unknown POS: " + t.pos);
					if (!dict.containsKey(t.form))
						dict.put(t.form, new TreeMap<String, Integer>());
					Statics.<String> increment(dict.get(t.form), t.pos);
				}
				sc.increment();
			}
		}
		finally
		{
			pr.shutdown();
		}

		PrintWriter sw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sSaveFile), "UTF-8"));
		for (String s : dict.keySet())
		{
			Map<String, Integer> lp = dict.get(s);
			sw.print(s + "\t");
			for (Entry<String, Integer> p : lp.entrySet())
				sw.print(p.getKey() + ":" + p.getValue() + "\t");
			sw.println();
		}
		sw.close();
	}

	public void loadFromStream(BufferedReader sr) throws NumberFormatException, IOException
	{
		clear();

		int n = Integer.parseInt(sr.readLine());
		for (int i = 0; i < n; ++i)
		{
			String sLine = Statics.trimSpecial(sr.readLine());
			String[] p = sLine.split("\t");
			String[] lp = new String[p.length - 1];
			for (int j = 1; j < p.length; ++j)
			{
				String[] pp = p[j].split(":");
				lp[j - 1] = pp[0];
			}
			if (p[0].startsWith("@@C@_"))
				m_closedTagDict.put(p[0].substring(5), lp);
			else if (p[0].startsWith("@@A@_"))
				m_allTagDict.put(p[0].substring(5), lp);
			else
				m_tagDict.put(p[0], lp);
		}
	}

	public void saveToStream(PrintWriter sw)
	{
		sw.println(m_tagDict.size() + m_closedTagDict.size() + m_allTagDict.size());
		for (Entry<String, String[]> e : m_tagDict.entrySet())
		{
			sw.print(e.getKey());
			sw.print("\t");
			String[] ss = e.getValue();
			for (int i = 0; i < ss.length; ++i)
				sw.print(ss[i] + ":" + "\t");
			sw.println();
		}
		for (Entry<String, String[]> e : m_closedTagDict.entrySet())
		{
			sw.print("@@C@_" + e.getKey());
			sw.print("\t");
			String[] ss = e.getValue();
			for (int i = 0; i < ss.length; ++i)
				sw.print(ss[i] + ":" + "\t");
			sw.println();
		}
		for (Entry<String, String[]> e : m_allTagDict.entrySet())
		{
			sw.print("@@A@_" + e.getKey());
			sw.print("\t");
			String[] ss = e.getValue();
			for (int i = 0; i < ss.length; ++i)
				sw.print(ss[i] + ":" + "\t");
			sw.println();
		}
	}

}
