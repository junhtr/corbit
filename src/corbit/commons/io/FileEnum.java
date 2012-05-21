package corbit.commons.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import corbit.commons.util.Generator;

public class FileEnum extends Generator<String>
{
	String sFile = null;

	public FileEnum(String s)
	{
		sFile = s;
	}

	@Override
	protected void iterate() throws InterruptedException
	{
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new InputStreamReader(new FileInputStream(sFile), "UTF-8"));
			String s = null;

			while ((s = br.readLine()) != null)
				yieldReturn(s);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (br != null)
					br.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				shutdown();
			}
		}
	}

}
