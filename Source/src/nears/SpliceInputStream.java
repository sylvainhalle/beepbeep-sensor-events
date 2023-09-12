/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023 Sylvain Hallé

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
package nears;

import java.io.InputStream;
import java.util.Queue;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.Pullable;
import ca.uqac.lif.cep.Pushable;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tmf.Source;
import ca.uqac.lif.cep.tuples.TupleFeeder;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.fs.HardDisk;

public class SpliceInputStream extends Source
{ 
	protected final Processor[] m_sources;

	protected int m_streamIndex;
	
	public SpliceInputStream(FileSystem fs, boolean read_stdin, String ... filenames)
	{
		super(1);
		m_sources = new Processor[filenames.length + (read_stdin ? 1 : 0)];
		for (int i = 0; i < filenames.length; i++)
		{
			m_sources[i] = guessSource(fs, filenames[i]);
		}
		if (read_stdin)
		{
			m_sources[filenames.length] = guessSource(fs, "-");
		}
		m_streamIndex = 0;
	}
	
	public SpliceInputStream(boolean read_stdin, String ... filenames) throws FileSystemException
	{
		this(new HardDisk().open(), read_stdin, filenames);
	}
	
	public SpliceInputStream(String ... filenames) throws FileSystemException
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

	protected static Processor guessSource(FileSystem fs, String filename) throws ProcessorException
	{
		try
		{
			InputStream is;
			if (filename.compareTo("-") == 0)
			{
				is = System.in;
			}
			else
			{
				is = fs.readFrom(filename);
			}
			if (filename.endsWith(".json"))
			{
				return new JsonLineFeeder(is);
			}
			if (filename.endsWith(".csv"))
			{
				return new GroupProcessor(0, 1) {{
					ReadLines rl = new ReadLines(is);
					TupleFeeder tf = new TupleFeeder();
					Connector.connect(rl, tf);
					addProcessors(rl, tf);
					associateOutput(tf);
				}};
			}
			return new ReadLines(is);
		}		
		catch (FileSystemException e)
		{
			throw new ProcessorException(e);
		}

	}
}