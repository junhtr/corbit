package corbit.commons.util;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GlobalConf {

	static GlobalConf m_inst = null;
	Map<String,Boolean> m_binaries = null;

	private GlobalConf() {
		m_binaries = new TreeMap<String,Boolean>();
	}

	public static GlobalConf getInstance() {
		if (m_inst == null)
			m_inst = new GlobalConf();
		return m_inst;
	}

	public void setConf(String s, boolean b) {
		if (!m_binaries.containsKey(s))
			m_binaries.put(s, b);
		else
			m_binaries.put(s, b);
	}

	public boolean getConf(String s) {
		if (!m_binaries.containsKey(s))
			throw new IllegalArgumentException("No such configuration: " + s);
		return m_binaries.get(s);
	}

	public Set<String> getConfs()
	{
		return m_binaries.keySet();
	}
}
