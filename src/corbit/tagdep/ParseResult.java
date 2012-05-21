package corbit.tagdep;

public class ParseResult
{
	public SRParserState parsedState;
	public SRParserState goldState;
	public boolean stopped;
	public int numMergedState;
	public int numTotalState;
	public long numNonDPState;

	public ParseResult(SRParserState so, SRParserState sg, boolean bStopped, int iMergedState, int iTotalState, long iNonDPState)
	{
		parsedState = so;
		goldState = sg;
		stopped = bStopped;
		numMergedState = iMergedState;
		numTotalState = iTotalState;
		numNonDPState = iNonDPState;
	}
}
