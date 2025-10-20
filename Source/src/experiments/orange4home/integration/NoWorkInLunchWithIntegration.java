package experiments.orange4home.integration;


/**
 * Property to monitor: Nothing happens in the office during the lunch break.
 * Lunch break takes place in the living room using the table_presence sensor.
 * Work in the office is detected using the office_presence and desk_pluf_consumption sensors.
 * If (table_presence=ON and office_presence=ON) or (table_presence=ON and desk_pluf_consumption > 0) then the property is violated.
 */

  /**
   * With integration, we don`t need to use filtering and slicing.
   */

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
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.util.Booleans.And;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.HtmlPrint;
import sensors.LogRepository;
import sensors.house.House;
import sensors.house.House.Device;
import sensors.house.House.Location;
import sensors.house.House.Sensor;
import sensors.house.House.Subject;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;

public class NoWorkInLunchWithIntegration {

	/* The folder where the data files reside. */
	protected static final LogRepository fs = new Orange4HomeLogRepository();
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new Orange4HomeFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException {
	    int runs = 1500; // number of times to repeat the experiment

	    long minTime = Long.MAX_VALUE, maxTime = Long.MIN_VALUE, totalTime = 0;
	    long minMem = Long.MAX_VALUE, maxMem = Long.MIN_VALUE, totalMem = 0;

	    for (int i = 0; i < runs; i++) {
	        Runtime runtime = Runtime.getRuntime();
	        runtime.gc(); // clean up memory before each run

	        long memBefore = runtime.totalMemory() - runtime.freeMemory();
	        long startTime = System.nanoTime();

	        /* ---------------- Start of the pipeline ---------------- */
	        fs.open();
	        InputStream is = fs.readFrom("o4h_all_events.csv");
	        OutputStream os = fs.writeTo("pairs-num.txt");
	        Processor feeder = format.getFeeder(is);

	        Pump p = new Pump();
	        connect(feeder, p);
	        ApplyFunction to_delta = new ApplyFunction(new House.EventToHouseDelta(format));
	        connect(p, to_delta);

	        Integrate house = new Integrate(new House());
	        connect(to_delta, house);

	        FunctionTree tablePresence = new FunctionTree(Equals.instance, new FunctionTree(
	            new Maps.Get("presence", new Sensor()), new FunctionTree(
	            new Maps.Get("table", new Subject()),
	            new Maps.Get("livingroom", new Location()))),
	                  new Constant(format.getOnConstant()));

	        FunctionTree officePresence = new FunctionTree(Equals.instance, new FunctionTree(
	            new Maps.Get("presence", new Sensor()), new FunctionTree(
	                    new Maps.Get("", new Subject()), 
	                    new Maps.Get("office", new Location()))),
	                      new Constant(format.getOnConstant()));

	        FunctionTree deskConsumption = new FunctionTree(Numbers.isGreaterThan, new FunctionTree(
	            new Maps.Get("plugconsumption", new Sensor()), new FunctionTree(new Maps.Get("state", Object.class),new FunctionTree(
	                    new Maps.Get("", new Device()), new FunctionTree(
	                    new Maps.Get("desk", new Subject()), 
	                    new Maps.Get("office", new Location()))))),
	                      new Constant(0));

	        ApplyFunction e1 = new ApplyFunction(new FunctionTree(And.instance, (new FunctionTree(And.instance, tablePresence, officePresence)) , deskConsumption));	    		
	        connect(house, e1);

	        KeepLast last = new KeepLast();
	        connect(e1, last);
	        connect(last, new HtmlPrint(new PrintStream(os)));

	        is.close();
	        os.close();
	        
	        /* ---------------- End of the pipeline ---------------- */

	        long endTime = System.nanoTime();
	        long memAfter = runtime.totalMemory() - runtime.freeMemory();

	        long durationMs = (endTime - startTime) / 1_000_000; // ms
	        long memUsedKb = (memAfter - memBefore) / 1024; // KB

	        // Track min, max, total
	        minTime = Math.min(minTime, durationMs);
	        maxTime = Math.max(maxTime, durationMs);
	        totalTime += durationMs;

	        minMem = Math.min(minMem, memUsedKb);
	        maxMem = Math.max(maxMem, memUsedKb);
	        totalMem += memUsedKb;
	    }

	    // Print results
	    System.out.println("Execution time (ms): min=" + minTime + ", max=" + maxTime + ", avg=" + (totalTime / runs));
	    System.out.println("Memory used (KB):   min=" + minMem + ", max=" + maxMem + ", avg=" + (totalMem / runs));
	    System.out.println("Done!");
	    
	    fs.close();
	}
    
	
}
