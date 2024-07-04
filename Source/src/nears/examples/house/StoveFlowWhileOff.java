package nears.examples.house;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.Integrate;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.json.JsonString;
import nears.HtmlPrint;
import nears.LogRepository;
import nears.MultiDaySource;
import nears.SensorEvent;
import nears.house.House;
import nears.house.House.Device;
import nears.house.House.Location;
import nears.house.House.Subject;

public class StoveFlowWhileOff
{

  public static void main(String[] args) throws FileSystemException, IOException
  {
    /* Define the range of days to process. */
    int first_day = 4, last_day = 4;

    /* Define the input and output file. */
    FileSystem fs = new LogRepository("0105").open();
    MultiDaySource feeder = new MultiDaySource(fs, first_day, last_day);
    OutputStream os = fs.writeTo("InstantSnapshot.html");
    
    /* Create the pipeline. */
    Pump p = new Pump();
    connect(feeder, p);
    ApplyFunction to_delta = new ApplyFunction(new House.EventToHouseDelta());
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
