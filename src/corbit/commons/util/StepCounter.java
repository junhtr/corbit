package corbit.commons.util;

public class StepCounter
{
	int iCount;
	int iFrequency;

	public int count() {
		return iCount;
	}

	public StepCounter() {
		this(100);
	}

	public StepCounter(int iFrequency)
	{
		iCount = 0;
		this.iFrequency = iFrequency;
	}

	public void increment()
	{
		++iCount;
		if (iFrequency > 0 && (iCount % iFrequency == 0))
		{
			if (iCount % (iFrequency * 20) == 0)
				System.out.println(String.format("%5d", iCount));
			else
				System.out.print(String.format("%3d ", iCount / iFrequency));
		}

	}

	public void dispose()
	{
		if (!(iFrequency > 0 && iCount % (iFrequency * 20) == 0))
			System.out.println();
		System.out.println("Total of " + iCount + " sentences are processed.");
	}

}
