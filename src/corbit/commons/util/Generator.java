package corbit.commons.util;

import java.lang.Thread.State;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public abstract class Generator<T> implements Iterator<T>, Iterable<T>
{
	private static final Object NULL = new Object();

	private static final Object END_OF_QUEUE = new Object();

	private static final Object NOT_PREPARED = new Object();

	private final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>(1);

	private final Semaphore lock = new Semaphore(0);

	private RuntimeException thrown = null;

	private Object nextValue = NOT_PREPARED;

	private final Thread thread = new Thread(new Runnable()
	{
		public void run()
		{
			try
			{
				Generator.this.iterate();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			catch (RuntimeException e)
			{
				thrown = e;
			}
			finally
			{
				queue.add(END_OF_QUEUE);
			}
		}
	});

	@Override
	public Iterator<T> iterator()
	{
		return this;
	}

	@Override
	protected void finalize() throws Throwable
	{
		try
		{
			super.finalize();
		}
		finally
		{
			shutdown();
		}
	}

	@Override
	public boolean hasNext()
	{
		prepareNext();
		return nextValue != END_OF_QUEUE;
	}

	@Override
	public T next()
	{
		if (thread == Thread.currentThread())
			throw new IllegalStateException("Illegal call");

		prepareNext();

		if (thrown != null)
		{
			RuntimeException t = thrown;
			thrown = null;
			throw t;
		}

		if (!hasNext())
			throw new NoSuchElementException();

		@SuppressWarnings("unchecked") T val = (T)nextValue;

		nextValue = NOT_PREPARED;

		return val;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}

	protected abstract void iterate() throws InterruptedException;

	public void shutdown()
	{
		thread.interrupt();
	}

	protected void yieldReturn(T value) throws InterruptedException
	{
		if (thread != Thread.currentThread())
			throw new IllegalStateException("Illegal call");

		if (!queue.offer(value != null ? value : NULL))
			throw new AssertionError();

		lock.acquire();
	}

	protected void yieldBreak() throws InterruptedException
	{
		throw new UnsupportedOperationException("to be implemented");
	}

	private void prepareNext()
	{
		if (nextValue != NOT_PREPARED)
			return;

		if (thread.getState() == State.NEW)
			thread.start();
		else
			lock.release();

		Object value;
		boolean interrupted = false;
		while (true)
		{
			try
			{
				value = queue.take();
				break;
			}
			catch (InterruptedException e)
			{
				interrupted = true;
			}
		}
		if (interrupted)
			Thread.currentThread().interrupt();
		nextValue = (value != NULL) ? value : null;
	}
}
