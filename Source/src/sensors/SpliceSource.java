/*
    BeepBeep, an event stream processor
    Copyright (C) 2008-2024 Sylvain Hallé

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package sensors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Queue;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.Pullable;
import ca.uqac.lif.cep.Pushable;
import ca.uqac.lif.cep.io.ReadInputStream;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.io.ReadTokens;
import ca.uqac.lif.cep.tmf.Source;
import ca.uqac.lif.cep.tuples.TupleFeeder;
import sensors.nears.JsonFeeder;
import sensors.nears.JsonLineFeeder;

/**
 * Source that sequentially reads from multiple input files, and optionally
 * from the standard input.
 * 
 * @author Sylvain Hallé
 */
/* package */ abstract class SpliceSource extends Source
{
	/**
	 * The array of sources to successively read from.
	 */
	/*@ non_null @*/ protected final Processor[] m_sources;

	/**
	 * The index of the source the processor is currently reading from.
	 */
	protected int m_streamIndex;

	/**
	 * Creates a new splice source.
	 * @param read_stdin Set to {@code true} to read from standard input after
	 * having read from all other provided sources first
	 * @param filenames The names of the files to read sequentially from
	 */
	public SpliceSource(boolean read_stdin, String ... filenames)
	{
		super(1);
		m_sources = new Processor[filenames.length + (read_stdin ? 1 : 0)];
		for (int i = 0; i < filenames.length; i++)
		{
			m_sources[i] = getSource(filenames[i]);
		}
		if (read_stdin)
		{
			m_sources[filenames.length] = getSource("-");
		}
		m_streamIndex = 0;
	}

	/**
	 * Creates a new splice source.
	 * having read from all other provided sources first
	 * @param filenames The names of the files to read sequentially from
	 */
	public SpliceSource(String ... filenames)
	{
		this(false, filenames);
	}

	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		Object o = getNextEvent();
		if (o == null)
		{
			return false;
		}
		outputs.add(new Object[] {o});
		return true;
	}

	@Override
	public Processor duplicate(boolean with_state)
	{
		throw new UnsupportedOperationException("This source cannot be duplicated");
	}

	@Override
	public void start()
	{
		super.start();
		Pushable p = getPushableOutput(0);
		Object o;
		do
		{
			o = getNextEvent();
			if (o != null)
			{
				p.push(o);
			}
		} while (o != null);
		p.notifyEndOfTrace();
		stop();
	}

	@Override
	public void stop()
	{
		super.stop();
		for (Processor p : m_sources)
		{
			p.stop();
		}
	}

	/**
	 * Obtains the next event available. If the source currently being read has
	 * no more events to provide, switches to the next source until all sources
	 * have been exhausted.
	 * @return The next event, or {@code null} if no more event is available
	 */
	protected Object getNextEvent()
	{
		while (m_streamIndex < m_sources.length)
		{
			Pullable p = m_sources[m_streamIndex].getPullableOutput();
			if (p.hasNext())
			{
				return p.pull();
			}
			else
			{
				m_sources[m_streamIndex].stop();
				m_streamIndex++;
			}
		}
		return null;
	}

	/**
	 * Obtains a source from a filename.
	 * @param filename The name of the file
	 * @return The source
	 */
	protected abstract Processor getSource(String filename);

	public static class SpliceLineSource extends SpliceSource
	{
		public SpliceLineSource(boolean read_stdin, String ... filenames)
		{
			super(read_stdin, filenames);
		}

		@Override
		protected Processor getSource(String filename) throws ProcessorException
		{
			try
			{
				if (filename.compareTo("-") == 0)
				{
					return new ReadLines(System.in);
				}
				else
				{
					return new ReadLines(new File(filename));
				}
			}
			catch (FileNotFoundException e)
			{
				throw new ProcessorException(e);
			}
		}
	}

	public static class SpliceTokenSource extends SpliceSource
	{
		protected final String m_separator;

		public SpliceTokenSource(boolean read_stdin, String separator, String ... filenames)
		{
			super(read_stdin, filenames);
			m_separator = separator;
		}

		@Override
		protected Processor getSource(String filename) throws ProcessorException
		{
			try
			{
				if (filename.compareTo("-") == 0)
				{
					return new ReadTokens(System.in, m_separator);
				}
				else
				{
					return new ReadTokens(new File(filename), m_separator);
				}
			}
			catch (FileNotFoundException e)
			{
				throw new ProcessorException(e);
			}
		}
	}

	public static class SpliceByteSource extends SpliceSource
	{
		public SpliceByteSource(boolean read_stdin, String ... filenames)
		{
			super(read_stdin, filenames);
		}

		@Override
		protected Processor getSource(String filename) throws ProcessorException
		{
			try
			{
				if (filename.compareTo("-") == 0)
				{
					return new ReadInputStream(System.in);
				}
				else
				{
					return new ReadInputStream(new File(filename));
				}
			}
			catch (FileNotFoundException e)
			{
				throw new ProcessorException(e);
			}
		}
	}

	public static class SpliceTupleSource extends SpliceSource
	{
		public SpliceTupleSource(boolean read_stdin, String ... filenames)
		{
			super(read_stdin, filenames);
		}

		@Override
		protected Processor getSource(String filename)
		{
			return new GroupProcessor(0, 1) {{
				Processor in;
				try
				{
					if (filename.compareTo("-") == 0)
					{
						in = new ReadLines(System.in);
					}
					else
					{
						in = new ReadLines(new File(filename));
					}
				}
				catch (FileNotFoundException e)
				{
					throw new ProcessorException(e);
				}
				TupleFeeder feeder = new TupleFeeder();
				Connector.connect(in, feeder);
				addProcessors(in, feeder);
				associateOutput(0, feeder, 0);
			}};
		}

	}

	public static class SpliceJsonStreamSource extends SpliceSource
	{
		public SpliceJsonStreamSource(boolean read_stdin, String ... filenames)
		{
			super(read_stdin, filenames);
		}

		@Override
		protected Processor getSource(String filename)
		{
			try
			{
				if (filename.compareTo("-") == 0)
				{
					return new JsonLineFeeder(System.in);
				}
				else
				{
					return new JsonLineFeeder(new FileInputStream(new File(filename)));
				}
			}
			catch (FileNotFoundException e)
			{
				throw new ProcessorException(e);
			}
		}
	}

	public static class SpliceJsonSource extends SpliceSource
	{
		public SpliceJsonSource(boolean read_stdin, String ... filenames)
		{
			super(read_stdin, filenames);
		}

		@Override
		protected Processor getSource(String filename)
		{
			try
			{
				if (filename.compareTo("-") == 0)
				{
					return new JsonFeeder(System.in);
				}
				else
				{
					return new JsonFeeder(new FileInputStream(new File(filename)));
				}
			}
			catch (FileNotFoundException e)
			{
				throw new ProcessorException(e);
			}
		}
	}
}
