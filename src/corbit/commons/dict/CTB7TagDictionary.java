package corbit.commons.dict;

import java.util.Set;
import java.util.TreeSet;

public class CTB7TagDictionary extends TagDictionary
{
	private static final long serialVersionUID = -3436661170537622556L;
	
	public static final String[] ssCtbTags = { "AD", "AS", "BA", "CC", "CD", "CS",
			"DEC", "DEG", "DER", "DEV", "DT", "ETC", "FW", "IJ", "JJ", "LB",
			"LC", "M", "MSP", "NN", "NR", "NT", "OD", "ON", "P", "PN", "PU",
			"SB", "SP", "VA", "VC", "VE", "VV", "URL" };
	public static final String[] ssOpenTags =
		{ "AD", "CD", "FW", "JJ", "M", "MSP", "NN", "NR", "NT", "OD", "VA", "VV", "URL" };
	public static final String[] ssClosedTags = { "AS", "BA", "CC", "CS", "DEC", "DEG",
			"DER", "DEV", "DT", "ETC", "IJ", "LB", "LC", "ON", "P", "PN", "PU",
			"SB", "SP", "VC", "VE" };
	public static final String[] arcLabels =
		{ "AMOD", "DEP", "NMOD", "OBJ", "P", "PMOD", "PRD", "ROOT", "SBAR", "SUB", "VC", "VMOD" };
	
	public CTB7TagDictionary(boolean bUseClosedTags)
	{
		super(
				bUseClosedTags ? ssOpenTags : ssCtbTags,
				bUseClosedTags ? ssClosedTags : null,
				ssCtbTags, 
				arcLabels
				);
	}

	public String[] getArcLabels()
	{
		return arcLabels;
	}
	
	public Set<String> generateTagSet()
	{
		Set<String> ss = new TreeSet<String>();
		for (int i = 0; i < ssCtbTags.length; ++i)
			ss.add(ssCtbTags[i]);
		return ss;
	}

}
