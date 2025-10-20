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
package sensors.house.examples;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.Integrate;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.HtmlPrint;
import sensors.LogRepository;
import sensors.MultiDaySource;
import sensors.house.House;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsMultiDaySource;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;

/**
 * Calculates a stream of snapshots of the house's state using integration.
 * 
 * @author Sylvain Hallé
 */
public class InstantSnapshot
{
	/**
	 *  The adapter for the event format.
	 */

	protected static final LogRepository fs = new Orange4HomeLogRepository();
	
	/* The adapter for the event format. */
	protected static final Orange4HomeFormat format = new Orange4HomeFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the range of days to process. */
		int first_day = 3, last_day = 3;

		/* Define the input and output file. */
		//FileSystem fs = new LogRepository("data/0105").open();
		//MultiDaySource feeder = new NearsMultiDaySource(fs, first_day, last_day);
		fs.open();
		InputStream is = fs.readFrom("o4h_all_events.csv");
		Processor feeder = format.getFeeder(is);
		OutputStream os = fs.writeTo("acts-dup.txt");
		
		
		/* Create the pipeline. */
		Pump p = new Pump();
		connect(feeder, p);
		FilterOn filter = new FilterOn(new FunctionTree(Equals.instance, format.subjectString(), new Constant("stove")));
		connect(p, filter);
		ApplyFunction to_delta = new ApplyFunction(new House.EventToHouseDelta(format));
		connect(filter, to_delta);
		Integrate instant = new Integrate(new House()); // integrate is a processor
		//System.out.print(instant.toString());
		connect(to_delta, instant);
		connect(instant, new HtmlPrint(new PrintStream(os)));
		p.run();

		/* Clean up. */
		feeder.stop();
		os.close();
		fs.close();
	}

}
