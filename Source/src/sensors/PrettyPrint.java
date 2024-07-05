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
package sensors;

import java.io.PrintStream;
import java.util.Map;
import java.util.Queue;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.tmf.Sink;

/**
 * A sink that attempts to "pretty-print" Java objects to a print stream.
 * For example, maps are displayed with indents.
 * @author Sylvain Hallé
 */
public class PrettyPrint extends Sink
{
	/**
	 * The print stream where events should be printed.
	 */
	protected final PrintStream m_out;
	
	/**
	 * The print stream that formats the incoming events.
	 */
	protected final PrettyPrintStream m_ps;
	
	/**
	 * Creates a new instance of the printer.
	 * @param ps The print stream where events should be printed
	 */
	public PrettyPrint(PrintStream ps)
	{
		m_out = ps;
		m_ps = new PrettyPrintStream(m_out);
	}
	
	/**
	 * Creates a new instance of the printer that prints to the standard output.
	 */
	public PrettyPrint()
	{
		this(System.out);
	}
	
	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		print(inputs[0], m_ps);
		return true;
	}

	@Override
	public Processor duplicate(boolean with_state)
	{
		throw new ProcessorException("This processor cannot be duplicated");
	}
	
	public static void print(Object o, PrettyPrintStream ps)
	{
		if (o == null)
		{
			ps.print("null");
			return;
		}
		if (o instanceof Map)
		{
			prettyPrint((Map<?,?>) o, ps);
			return;
		}
		ps.print(o);
	}
	
	/**
	 * Prints a Java {@link Map} by using indents.
	 * @param m The map to print
	 * @param ps The print stream where the contents should be printed
	 */
	protected static void prettyPrint(Map<?,?> m, PrettyPrintStream ps)
	{
		boolean first = true;
		ps.println("{");
		ps.indent();
		for (Map.Entry<?,?> e : m.entrySet())
		{
			if (first)
			{
				first = false;
			}
			else
			{
				ps.println(",");
			}
			print(e.getKey(), ps);
			ps.print(" \u21a6 ");
			print(e.getValue(), ps);
		}
		ps.outdent();
		ps.println();
		ps.println("}");
	}

}
