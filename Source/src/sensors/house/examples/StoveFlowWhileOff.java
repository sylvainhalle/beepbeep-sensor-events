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
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.Integrate;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.HtmlPrint;
import sensors.LogRepository;
import sensors.MultiDaySource;
import sensors.house.House;
import sensors.house.House.Device;
import sensors.house.House.Location;
import sensors.house.House.Subject;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsMultiDaySource;

public class StoveFlowWhileOff
{
	/**
	 *  The adapter for the event format.
	 */
	protected static final EventFormat format = new NearsJsonFormat();
	
  public static void main(String[] args) throws FileSystemException, IOException
  {
    /* Define the range of days to process. */
    int first_day = 4, last_day = 4;

    /* Define the input and output file. */
    FileSystem fs = new LogRepository("0105").open();
    MultiDaySource feeder = new NearsMultiDaySource(fs, first_day, last_day);
    OutputStream os = fs.writeTo("InstantSnapshot.html");
    
    /* Create the pipeline. */
    Pump p = new Pump();
    connect(feeder, p);
    ApplyFunction to_delta = new ApplyFunction(new House.EventToHouseDelta(format));
    connect(p, to_delta);
    Integrate house = new Integrate(new House());
    connect(to_delta, house);
    ApplyFunction e1 = new ApplyFunction(new FunctionTree(
    		new Maps.Get("stove_monitoring_devices_binder", new Device()), 
    		new FunctionTree(new Maps.Get("stove", new Subject()), 
    				new Maps.Get("kitchen", new Location()))));
    connect(house, e1);
    
    connect(e1, new HtmlPrint(new PrintStream(os)));
    p.run();

    /* Clean up. */
    feeder.stop();
    os.close();
    fs.close();

  }

}
