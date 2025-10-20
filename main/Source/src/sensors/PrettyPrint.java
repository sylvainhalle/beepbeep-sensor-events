/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023-2024 Sylvain Hallé

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

import java.io.ByteArrayOutputStream;
import java.util.Map;

import ca.uqac.lif.cep.functions.FunctionException;
import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * A function that attempts to "pretty-print" Java objects.
 * For example, maps are displayed with indents.
 * @author Sylvain Hallé
 */
public class PrettyPrint extends UnaryFunction<Object,String>
{	
	/**
	 * Creates a new instance of the printer.
	 * @param ps The print stream where events should be printed
	 */
	public PrettyPrint()
	{
		super(Object.class, String.class);
	}
	
	@Override
	public String getValue(Object inputs)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrettyPrintStream ps = new PrettyPrintStream(baos);
		print(inputs, ps);
		ps.flush();
		return baos.toString();
	}

	@Override
	public PrettyPrint duplicate(boolean with_state)
	{
		throw new FunctionException("This function cannot be duplicated");
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
		if (o.getClass().isArray())
		{
			ps.print("[");
			for (int i = 0; i < ((Object[]) o).length; i++)
			{
				if (i > 0)
				{
					ps.print(", ");
				}
				print(((Object[]) o)[i], ps);
			}
			ps.print("]");
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
