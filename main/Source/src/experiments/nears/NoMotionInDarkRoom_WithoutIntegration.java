package experiments.nears;


import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IfThenElse;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Booleans.And;
import ca.uqac.lif.cep.util.Booleans.Not;
import ca.uqac.lif.cep.util.Booleans.Or;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.Size;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.MultiDaySource;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsMultiDaySource;

public class NoMotionInDarkRoom_WithoutIntegration {
	
	protected static final EventFormat format = new NearsJsonFormat();

  public static void main(String[] args) throws FileSystemException, IOException {
        final int runs = 35; // number of repetitions

        long minTime = Long.MAX_VALUE, maxTime = Long.MIN_VALUE, totalTime = 0;
        long minMem  = Long.MAX_VALUE, maxMem  = Long.MIN_VALUE, totalMem  = 0;

        int first_day = 1, last_day = 36;

  	    FileSystem fs = new LogRepository("data/0105").open();
  	    fs.open(); // open once

        for (int i = 0; i < runs; i++) {
            Runtime rt = Runtime.getRuntime();
            rt.gc();

            long memBefore = rt.totalMemory() - rt.freeMemory();
            long start = System.nanoTime();

            // Input & feeder
            MultiDaySource feeder = new NearsMultiDaySource(fs, first_day, last_day);

            //slice per location
            Slice sliceByLocation = new Slice(format.locationString(), LightMotionGroup());

            //keep only locations that violates the property
            ApplyFunction keepViolations = new ApplyFunction(
                    new FunctionTree(
                        new Maps.FilterMap(
                            new FunctionTree(Equals.instance, StreamVariable.Y, new Constant(Boolean.FALSE))
                        ),
                        StreamVariable.X   
                    )
                );

            //check the size of the map. If size of map is Zero then property holds
            ApplyFunction sizeIsZero = new ApplyFunction(
                    new FunctionTree(
                        Equals.instance,
                        new FunctionTree(Size.instance, StreamVariable.X), // size(map_of_violations)
                        new Constant(0)
                    )
                );

            // Output verdicts (true/false) one per line
            OutputStream os = fs.writeTo("NoMotionInDark-WithoutIntegration-Nears.txt");
            Print sink = new Print(new PrintStream(os)).setSeparator("\n");

            // connect pipeline
            Pump p = new Pump();
            connect(feeder, p);                 // feeder → p
            connect(p, sliceByLocation);        // p → sliceByLocation
            connect(sliceByLocation, keepViolations);
            connect(keepViolations, sizeIsZero);
            connect(sizeIsZero, sink);
            
            // RUN the pipeline
            p.run();
            feeder.stop();

            // Close resources
            os.close();

            long end = System.nanoTime();
            long memAfter = rt.totalMemory() - rt.freeMemory();

            long durationMs = (end - start) / 1_000_000; // ms
            long memUsedKb  = (memAfter - memBefore) / 1024; // KB

            // Stats
            minTime = Math.min(minTime, durationMs);
            maxTime = Math.max(maxTime, durationMs);
            totalTime += durationMs;

            minMem  = Math.min(minMem, memUsedKb);
            maxMem  = Math.max(maxMem, memUsedKb);
            totalMem += memUsedKb;

            System.out.printf("Run %d: time=%d ms, mem=%d KB%n", i + 1, durationMs, memUsedKb);
        }

        fs.close();

        System.out.println("----- Summary -----");
        System.out.println("Execution time (ms): min=" + minTime + ", max=" + maxTime + ", avg=" + (totalTime / runs));
        System.out.println("Memory used (KB):   min=" + minMem  + ", max=" + maxMem  + ", avg=" + (totalMem  / runs));
        System.out.println("Done!");
  }
  
  private static GroupProcessor LightMotionGroup() {
      GroupProcessor gp = new GroupProcessor(1, 1);
      
      // Allowed sensor names
      final String PRESENCE = "motion";
      final String L  = "luminance"; // NEARS uses luminance

      // Filter by keeping only presence & light sensors at this location
      FilterOn keepMotionLight = new FilterOn(
                new FunctionTree(
                    Or.instance,
                    new FunctionTree(Equals.instance, format.sensorString(), new Constant(PRESENCE)),
                    new FunctionTree(Equals.instance, format.sensorString(), new Constant(L))
                )
            );

      
      //This processor maps each event to a Boolean depending on sensor type and state
      ApplyFunction sensorToBoolean = new ApplyFunction(
                new FunctionTree(
                    IfThenElse.instance,
                    // sensor == presence ?
                    new FunctionTree(Equals.instance, format.sensorString(), new Constant(PRESENCE)),
                    // then: presence flag = (state == ON)
                    new FunctionTree(Equals.instance, format.stateString(), new Constant(format.getOnConstant())),
                    // else: sensor is any of the light* ?
                    new FunctionTree(
                        IfThenElse.instance,
                        new FunctionTree(Equals.instance, format.sensorString(), new Constant(L)),
                        // then: dark flag = (state == 0)
                        new FunctionTree(Equals.instance, format.stateString(), new Constant(0)),
                        // else: ignore others → false
                        new Constant(false)
                    )
                )
            );
      
      //For each substream, slice based on sensor string and apply sensorToBoolean function on each substream to extract the value in each stream
      // this will produce a hasmaps <sensorname,Boolean>
      Slice bySensor = new Slice(format.sensorString(), sensorToBoolean);
      
      FunctionTree presenceIsOn = new FunctionTree(
                Equals.instance,
                new FunctionTree(new Maps.Get(PRESENCE), StreamVariable.X),
                new Constant(Boolean.TRUE)
            );
      
   // Any of the light keys is dark?
      FunctionTree anyLightDark = new FunctionTree(
                Equals.instance,
                new FunctionTree(new Maps.Get(L),  StreamVariable.X),
                new Constant(Boolean.TRUE)
            );
      
      
      // Then in locationOk:
      ApplyFunction locationOk = new ApplyFunction(
                new FunctionTree(
                    Not.instance,
                    new FunctionTree(And.instance, presenceIsOn, anyLightDark)
                )
            );

      
      // connect the pipeline
      gp.addProcessors(keepMotionLight, bySensor, locationOk);
      connect(keepMotionLight, bySensor);
      connect(bySensor, locationOk);
      gp.associateInput(0, keepMotionLight, 0);
      gp.associateOutput(0, locationOk, 0);
      
      return gp;
}
  
}
