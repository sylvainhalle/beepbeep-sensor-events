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
	
	public static Processor Read(String[] args) throws IOException
	{
		boolean show_progress = false;
		String filename = "-";
		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if (arg.compareTo("--show-progress") == 0 || arg.compareTo("-p") == 0)
			{
				show_progress = true;
			}
			else
			{
				filename = args[i];
			}
		}
		if (filename.compareTo("-") == 0)
		{
			return s_format.getFeeder(System.in);
		}
		return s_format.getFeeder(filename, show_progress ? System.err : null);
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
	
	
}
