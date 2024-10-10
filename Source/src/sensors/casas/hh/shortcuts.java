/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023-2024 Sylvain Hallé, Rania Taleb

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
package sensors.casas.hh;

import java.io.IOException;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;

public class shortcuts extends sensors.casas.shortcuts
{
	protected static final HHFormat s_format = new HHFormat();
	
	/**
	 * Constant representing the "open" state in this event format.
	 */
	public static final Object OPEN = s_format.getOpenConstant();
	
	/**
	 * Constant representing the "closed" state in this event format.
	 */
	public static final Object CLOSED = s_format.getClosedConstant();
	
	/**
	 * Constant representing the "on" state in this event format.
	 */
	public static final Object ON = s_format.getOnConstant();
	
	/**
	 * Constant representing the "off" state in this event format.
	 */
	public static final Object OFF = s_format.getOffConstant();
	
	/**
	 * Creates a processor that read events from a file in a given format. The
	 * resulting processor will either read from the standard input if the
	 * filename is "-", or from a file with the given name.
	 * @param format The format of the events
	 * @param args The command-line arguments
	 * @return The processor
	 * @throws IOException If an error occurs while reading the file or the
	 * standard input
	 * @see sensors.shortcuts#createSource(EventFormat, String[])
	 */
	public static Processor Read(String[] args) throws IOException
	{
		return sensors.shortcuts.createSource(s_format, args);
	}
	
	public static Function Timestamp()
	{
		return s_format.timestamp();
	}
	
	public static Function Timestamp(Object x)
	{
		return new FunctionTree(s_format.timestamp(), liftFunction(x));
	}
	
	public static Function SensorId()
	{
		return s_format.sensorId();
	}
	
	public static Function Index()
	{
		return s_format.index();
	}
	
	public static Function Index(Object o)
	{
		return new FunctionTree(s_format.index(), liftFunction(o));
	}
	
	public static Function State()
	{
		return s_format.stateString();
	}
	
	public static Function State(Object o)
	{
		return new FunctionTree(s_format.stateString(), liftFunction(o));
	}	
	
	public static Function IsNumeric()
	{
		return s_format.isNumeric();
	}
}
