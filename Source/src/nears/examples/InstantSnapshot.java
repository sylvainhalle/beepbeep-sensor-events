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
package nears.examples;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.Integrate;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.json.JsonString;
import nears.House;
import nears.HtmlPrint;
import nears.LogRepository;
import nears.MultiDaySource;
import nears.SensorEvent;

/**
 * Calculates a stream of snapshots of the house's state using integration.
 * 
 * @author Sylvain Hallé
 */
public class InstantSnapshot
{
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the range of days to process. */
		int first_day = 1, last_day = 1;

		/* Define the input and output file. */
		FileSystem fs = new LogRepository().open();
		OutputStream os = fs.writeTo("InstantSnapshot.html");
		fs.chdir("0032");
		
		/* Create the pipeline. */
		MultiDaySource feeder = new MultiDaySource(fs, first_day, last_day);
		Pump p = new Pump();
		connect(feeder, p);
		FilterOn filter = new FilterOn(new FunctionTree(Equals.instance, new JPathFunction(SensorEvent.JP_LOCATION), new Constant(new JsonString("kitchen"))));
		connect(p, filter);
		ApplyFunction to_delta = new ApplyFunction(new House.EventToHouseDelta());
		connect(filter, to_delta);
		Integrate instant = new Integrate(new House());
		connect(to_delta, instant);
		connect(instant, new HtmlPrint(new PrintStream(os)));
		p.run();

		/* Clean up. */
		feeder.stop();
		os.close();
		fs.close();
	}

}
