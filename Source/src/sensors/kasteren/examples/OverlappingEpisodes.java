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
package sensors.kasteren.examples;

import static ca.uqac.lif.cep.Connector.connect;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import sensors.kasteren.KasterenEventFormat;
import sensors.kasteren.KasterenEventFormat.KasterenFeeder;

public class OverlappingEpisodes
{

	protected static final KasterenEventFormat s_format = new KasterenEventFormat();
	
	public static void main(String[] args)
	{
		KasterenFeeder feeder = s_format.getFeeder(null);
		Slice slice = new Slice(s_format.sensorId(), new GroupProcessor(1, 1) {{
			Fork f = new Fork();
			Trim t = new Trim(1);
			connect(f, 1, t, 0);
			ApplyFunction same = new ApplyFunction(new FunctionTree(Equals.instance,
					new FunctionTree(s_format.stateString(), StreamVariable.X),
					new FunctionTree(s_format.stateString(), StreamVariable.Y)
					));
			connect(f, 0, same, 0);
			connect(t, 0, same, 1);
			Cumulate or = new Cumulate(Booleans.or);
			connect(same, or);
			addProcessors(f, t, same, or);
			associateInput(0, f, 0);
			associateOutput(0, or, 0);
		}});
		connect(feeder, slice);
		KeepLast last = new KeepLast();
		connect(slice, last);
		Pump p = new Pump();
		connect(last, p);
		Print print = new Print.Println();
		connect(p, print);
		
		p.run();

	}

}
