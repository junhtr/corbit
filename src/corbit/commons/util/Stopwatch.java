package corbit.commons.util;

import java.text.DateFormat;
import java.util.Date;

import corbit.commons.io.Console;

public class Stopwatch
{
	String m_sMessage;
	long lStart;
	long lElapsed;
	boolean bRunning = false;

	public Stopwatch(String s)
	{
		m_sMessage = s;
		Console.writeLine(String.format("[%s | 0.00:00:00.000] start: %s", DateFormat.getInstance().format(new Date()), s));
		reset();
		start();
	}

	public void reset()
	{
		lElapsed = 0;
		lStart = System.currentTimeMillis();
	}

	public void start()
	{
		if (bRunning)
			return;
		bRunning = true;
		lStart = System.currentTimeMillis();
	}

	public void pause()
	{
		if (!bRunning)
			return;
		bRunning = false;
		lElapsed += System.currentTimeMillis() - lStart;
	}

	public void lap()
	{
		pause();
		DateFormat df1 = DateFormat.getInstance();

		final long n[] = { 1000, 60, 60, 24 };
		long t[] = new long[n.length + 1];

		long l = lElapsed;
		for (int i = 0; i < n.length; ++i) {
			t[i] = l % n[i];
			l = l / n[i];
		}
		t[n.length] = l;

		Console.writeLine(String.format("[%s | %s] end: %s",
				df1.format(new Date()),
				String.format("%1d.%02d:%02d:%02d.%03d", t[4], t[3], t[2], t[1], t[0]),
				m_sMessage));
	}
}
