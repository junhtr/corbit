package corbit.commons.ml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class AveragedWeight extends WeightVector
{
	WeightVector wa = new WeightVector();
	int iStep = 0;

	public AveragedWeight(AveragedWeight v)
	{
		super(v);
		wa = new WeightVector(v.wa);
		iStep = v.iStep;
	}

	public void nextStep()
	{
		++iStep;
	}

	public int getStep()
	{
		return iStep;
	}

	public WeightVector getAverageWeight()
	{
		return wa;
	}

	@Override
	public void append(IntFeatVector v)
	{
		super.append(v);
		wa.append(IntFeatVector.multiply(v, (double)iStep));
	}

	@Override
	public void subtract(IntFeatVector v)
	{
		super.subtract(v);
		wa.subtract(IntFeatVector.multiply(v, (double)iStep));
	}

	public AveragedWeight()
	{
		wa = new WeightVector();
	}

	public WeightVector getAveragedWeight()
	{
//    	Stopwatch sw = new Stopwatch("Averaging weight...");
		double dStep = (double)iStep;
		WeightVector v = new WeightVector(this);
		for (int i = 0; i < wa.capacity; ++i)
			v.put(i, v.get(i) - wa.get(i) / dStep);
//      sw.lap();
		return v;
	}

	public void save(PrintWriter sw)
	{
		sw.println(iStep);
		super.save(sw);
		wa.save(sw);
	}

	public void load(BufferedReader sr) throws IOException
	{
		iStep = Integer.parseInt(sr.readLine());
		super.load(sr);
		wa.load(sr);
	}
}
