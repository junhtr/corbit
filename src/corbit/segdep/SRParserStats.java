package corbit.segdep;

import corbit.commons.io.Console;

public class SRParserStats
{
	/**
	 * Even with volatile declaration, these variables are not thread-safe.
	 * However, it is practically ok for getting rough statistics.
	 */

	public volatile double totalScore = 0.0;

	public volatile int numSentencesProcessed = 0;
	public volatile int numSentencesStopped = 0;

	public volatile long numNonDPStatesEvaluated = 0;
	public volatile int numStatesEvaluated = 0;
	public volatile int numStatesMerged = 0;

	public volatile int numSegCandidates = 0;
	public volatile int numSegsPruned = 0;
	public volatile int numSegsEvaluated = 0;

	public volatile int numTagCandidates = 0;
	public volatile int numTagsConsidered = 0;
	public volatile int numTagsPruned = 0;
	public volatile int numTagsEvaluated = 0;

	public volatile int numGoldMoves = 0;
	public volatile int numGoldMovesSegCovered = 0;
	public volatile int numGoldMovesCovered = 0;

	public volatile int numSegsIncorrectlyPruned = 0;
	public volatile int numSegsPrematurelyPruned = 0;

	public volatile int numGoldOutOfBeamHigher = 0;
	public volatile int numGoldOutOfBeamLower = 0;
	public volatile int numGoldInBeamHigher = 0;
	public volatile int numGoldInBeamLower = 0;

	public void print()
	{
		/* evaluated sentences and states */

		if (numSentencesStopped > 0)
			Console.writeLine(String.format("%d/%d sentences (%f%%) have been early updated.", numSentencesStopped, numSentencesProcessed, (double)numSentencesStopped / numSentencesProcessed * 100));
		if (numNonDPStatesEvaluated > 0)
			Console.writeLine(numNonDPStatesEvaluated + " non-DP states evaluated.");
		if (numStatesMerged > 0)
			Console.writeLine(String.format("%d/%d states (%f%%) have been merged.", numStatesMerged, numStatesEvaluated, (double)numStatesMerged / (double)numStatesEvaluated * 100));
		Console.writeLine("Average output score: " + totalScore / numSentencesProcessed);

		/* evaluated actions */

		Console.writeLine(String.format("Segmentation: %,9d evaluated, %,9d pruned, %,9d in total",
				numSegsEvaluated, numSegsPruned, numSegCandidates));
		Console.writeLine(String.format("POS tagging:  %,9d evaluated, %,9d pruned, %,9d considered, %,9d in total",
				numTagsEvaluated, numTagsPruned, numTagsConsidered, numTagCandidates));
		Console.writeLine(String.format("Gold actions: %,9d / %,9d (%f%%) covered.", numGoldMovesCovered, numGoldMoves, (double)numGoldMovesCovered / numGoldMoves * 100));
		Console.writeLine(String.format("Gold actions: %,9d / %,9d (%f%%) segs covered.", numGoldMovesSegCovered, numGoldMoves, (double)numGoldMovesSegCovered / numGoldMoves * 100));
		Console.writeLine(String.format("Gold actions: %,9d / %,9d (%f%%) segs are incorrectly pruned.", numSegsIncorrectlyPruned, numSegsPruned, (double)numSegsIncorrectlyPruned / numSegsPruned * 100));
		Console.writeLine(String.format("Gold actions: %,9d / %,9d (%f%%) segs are prematurely pruned.", numSegsPrematurelyPruned, numSegsPruned, (double)numSegsPrematurelyPruned / numSegsPruned * 100));
	}
}
