package corbit.tagdep.dict;

import java.util.Set;
import java.util.TreeSet;

public class CTBTagDictionary extends TagDictionary
{
	private static final long serialVersionUID = 6692431815764780778L;

	static final String[] ssCtbTags = { "AD", "AS", "BA", "CC", "CD", "CS",
			"DEC", "DEG", "DER", "DEV", "DT", "ETC", "FW", "IJ", "JJ", "LB",
			"LC", "M", "MSP", "NN", "NR", "NT", "OD", "ON", "P", "PN", "PU",
			"SB", "SP", "VA", "VC", "VE", "VV", "URL" };
	static final String[] ssOpenTags =
		{ "AD", "CD", "FW", "JJ", "M", "MSP", "NN", "NR", "NT", "OD", "VA", "VV", "URL" };
	static final String[] ssClosedTags = { "AS", "BA", "CC", "CS", "DEC", "DEG",
			"DER", "DEV", "DT", "ETC", "IJ", "LB", "LC", "ON", "P", "PN", "PU",
			"SB", "SP", "VC", "VE" };

	public CTBTagDictionary(boolean bUseClosedTags)
	{
		super(
				bUseClosedTags ? ssOpenTags : ssCtbTags,
				bUseClosedTags ? ssClosedTags : null,
				ssCtbTags
				);
	}

	public static Set<String> copyTagSet()
	{
		Set<String> ss = new TreeSet<String>();
		for (int i = 0; i < ssCtbTags.length; ++i)
			ss.add(ssCtbTags[i]);
		return ss;
	}
	
}
