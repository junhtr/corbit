package corbit.commons;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;

import corbit.commons.io.Console;
import corbit.commons.ml.AveragedWeight;
import corbit.commons.ml.WeightVector;

public class Vocab
{
	Map<String,Integer> m_index;
	List<String> m_rindex;
	int m_size = 0;

	public Vocab()
	{
		m_index = new HashMap<String,Integer>();
		m_rindex = new ArrayList<String>();
	}

	public Vocab(String sFile)
	{
		this();
		try
		{
			load(sFile);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public Vocab(BufferedReader br)
	{
		this();
		try
		{
			load(br);
		} catch (NumberFormatException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public Set<String> getKeys()
	{
		return m_index.keySet();
	}

	/**
	 * rebuild the entire vocabulary with unused items removed should be
	 * effective for reducing memory use
	 * 
	 * @param w
	 */
	@SuppressWarnings("deprecation") public void rebuild(AveragedWeight w)
	{
		Map<String,Integer> oldIndex = m_index;
		m_index = (m_index instanceof HashMap)
				? new HashMap<String,Integer>()
				: new PatriciaTrie<String,Integer>(StringKeyAnalyzer.INSTANCE);
		m_rindex.clear();
		WeightVector ww = new WeightVector(w);
		WeightVector wa = new WeightVector(w.getAverageWeight());
		WeightVector wwNew = w;
		WeightVector waNew = w.getAverageWeight();
		wwNew.clear();
		waNew.clear();

		int iNewSize = 0;

		for (Entry<String,Integer> e: oldIndex.entrySet())
		{
			String sKey = e.getKey();
			int oldidx = e.getValue();

			if (ww.get(oldidx) != 0.0d || wa.get(oldidx) != 0.0d)
			{
				int newidx = iNewSize++;
				m_index.put(sKey, newidx);
				m_rindex.add(sKey);
				wwNew.put(newidx, ww.get(oldidx));
				waNew.put(newidx, wa.get(oldidx));
			}
		}
		m_size = iNewSize;
		assert (m_size == m_index.size());

		Console.writeLine(String.format("Vocab and AveragedWeight rebuilt: size %d => %d.", oldIndex.size(), m_rindex.size()));
	}

	@SuppressWarnings("deprecation") public void setUseTrie()
	{
		if (m_index instanceof HashMap)
		{
			Console.writeLine("HashMap converted to PatriciaTrie.");
			m_index = new PatriciaTrie<String,Integer>(StringKeyAnalyzer.INSTANCE, m_index);
		}
	}

	public void clear()
	{
		m_index.clear();
		m_rindex.clear();
		m_size = 0;
	}

	public boolean contains(String s)
	{
		return m_index.containsKey(s);
	}

	public int get(String s)
	{
		return m_index.get(s);
	}

	public Integer getBoxed(String s)
	{
		return m_index.get(s);
	}

	public String get(int i)
	{
		return m_rindex.get(i);
	}

	public int getIndex(String s)
	{
		assert (s != null);
		Integer i;
		if ((i = m_index.get(s)) != null)
			return i.intValue();
		else
		{
			m_index.put(s, m_size);
			m_rindex.add(s);
			return m_size++;
		}
	}

	public void save(String sFile) throws FileNotFoundException, UnsupportedEncodingException
	{
		PrintWriter sw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sFile), "UTF-8"));
		save(sw);
		sw.close();
	}

	public void save(PrintWriter sw)
	{
		int n = m_rindex.size();
		sw.println(n);
		for (int i = 0; i < n; ++i)
			sw.println(m_rindex.get(i));
		Console.writeLine(n + " vocabulary entries saved.");
	}

	public void load(String sFile) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sFile), "UTF-8"));
		int numWords = 0;
		String sLine;
		while ((sLine = br.readLine()) != null)
		{
			String[] fields = Normalizer.normalize(sLine, Normalizer.Form.NFKC).split("[ \t]+");
			if (!m_index.containsKey(fields[0]))
			{
				m_rindex.add(fields[0]);
				m_index.put(fields[0], numWords++);
			}
		}
		m_size = numWords;
		br.close();
		System.err.println(m_size + " dictionary entries loaded from " + sFile + ".");
	}

	public void load(BufferedReader sr) throws NumberFormatException, IOException
	{
		m_size = Integer.parseInt(sr.readLine());
		for (int i = 0; i < m_size; ++i)
		{
			String s = sr.readLine();
			assert (!m_index.containsKey(s));
			m_rindex.add(s);
			m_index.put(s, i);
		}
		System.err.println(m_size + " vocabulary entries loaded from the model.");
	}
}
