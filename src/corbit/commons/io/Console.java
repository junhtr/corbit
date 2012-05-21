package corbit.commons.io;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class Console
{
	static PrintWriter pw;

	public static void writeLine()
	{
		pw.println();
	}

	public static void writeLine(String s)
	{
		pw.println(s);
	}

	public static void writeLine(int i)
	{
		pw.println(i);
	}

	public static void writeLine(boolean b)
	{
		pw.println(b);
	}

	public static void writeLine(Object o)
	{
		pw.println(o);
	}

	public static void write(String s)
	{
		pw.print(s);
	}

	public static void write(Object o)
	{
		pw.print(o);
	}

	public static void write(char c)
	{
		pw.print(c);
	}

	public static void write(int i)
	{
		pw.print(i);
	}

	public static void write(double d)
	{
		pw.print(d);
	}

	public static void open()
	{
		try
		{
			pw = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"), true);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
	}

	public static void close()
	{
		pw.close();
	}
}
