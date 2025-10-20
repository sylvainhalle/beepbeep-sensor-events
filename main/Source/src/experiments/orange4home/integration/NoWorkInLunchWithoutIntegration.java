package experiments.orange4home.integration;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Booleans.And;
import ca.uqac.lif.cep.util.Booleans.Or;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;

public class NoWorkInLunchWithoutIntegration {

    /* The folder where the data files reside. */
    protected static final LogRepository fs = new Orange4HomeLogRepository();

    /* The adapter for the event format. */
    protected static final EventFormat format = new Orange4HomeFormat();

    public static void main(String[] args) throws FileSystemException, IOException {
        final int runs = 1500; // repeat count

        long minTime = Long.MAX_VALUE, maxTime = Long.MIN_VALUE, totalTime = 0;
        long minMem = Long.MAX_VALUE, maxMem = Long.MIN_VALUE, totalMem = 0;

        // Open the repository once; reuse it across runs
        fs.open();

        for (int i = 0; i < runs; i++) {
            // --- measurement setup
            Runtime runtime = Runtime.getRuntime();
            runtime.gc(); // best-effort cleanup before measuring
            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            long startTime = System.nanoTime();

            /* ---------------- Start of the pipeline for this run ---------------- */
            InputStream is = fs.readFrom("o4h_all_events.csv");
            // Make output unique per run to avoid overwriting
            OutputStream os = fs.writeTo("pairs-num-run-" + i + ".txt");
            Processor feeder = format.getFeeder(is);

            // Filter events leaving only the events located in the livingroom and office 
            FilterOn fil = new FilterOn(
                new FunctionTree(
                    Or.instance,
                    new FunctionTree(Equals.instance, format.locationString(), new Constant("livingroom")),
                    new FunctionTree(Equals.instance, format.locationString(), new Constant("office"))
                )
            );
            connect(feeder, fil);

            // Group processor to keep only table_presence, office_presence, desk_plug_consumption
            GroupProcessor processor = new GroupProcessor(1, 1);
            {
                FilterOn filterSensors = new FilterOn(
                    new FunctionTree(
                        Or.instance,
                        new FunctionTree(Equals.instance, format.sensorString(), new Constant("table_presence")),
                        new FunctionTree(
                            Or.instance,
                            new FunctionTree(Equals.instance, format.sensorString(), new Constant("office_presence")),
                            new FunctionTree(Equals.instance, format.sensorString(), new Constant("desk_plug_consumption"))
                        )
                    )
                );

             // Slice by sensor and evaluate state: ON or > 0
                Slice sliceSensors = new Slice(
                    format.sensorString(),
                    new ApplyFunction(
                        new FunctionTree(
                            And.instance,
                            new FunctionTree(And.instance, 
                            		        new FunctionTree (Equals.instance, format.sensorString(), new Constant("table_presence")), 
                            		        new FunctionTree(Equals.instance, format.stateString(), new Constant(format.getOnConstant())
                            		        		)),
                            
                            new FunctionTree(Or.instance, 
                            		         new FunctionTree (And.instance, new FunctionTree (Equals.instance, format.sensorString(), new Constant("office_presence")), 
                                     		                                 new FunctionTree(Equals.instance, format.stateString(), new Constant(format.getOnConstant()))),
                            		         new FunctionTree (And.instance, new FunctionTree (Equals.instance, format.sensorString(), new Constant("desk_plug_consumption")), 
             		                                 new FunctionTree(Numbers.isGreaterThan, format.stateString(), new Constant(0))))
                            		         
                        ) 
                    )
                );

                ApplyFunction values = new ApplyFunction(
                    new FunctionTree(
                        Maps.Keys.instance,
                        new Maps.FilterMap(
                            new FunctionTree(Equals.instance, StreamVariable.Y, new Constant(Boolean.TRUE))
                        )
                    )
                );

                // Build inner pipeline
                processor.addProcessors(filterSensors, sliceSensors, values);
                connect(filterSensors, sliceSensors);
                connect(sliceSensors, values);
                processor.associateInput(0, filterSensors, 0);
                processor.associateOutput(0, values, 0);
            }

            connect(fil, processor);

            Pump p = new Pump();
            connect(processor, p);

            KeepLast last = new KeepLast();
            connect(p, last);

            Print print = new Print(new PrintStream(os)).setSeparator("\n");
            connect(last, print);

            // Close per-run resources
            is.close();
            os.close();
            /* ---------------- End of the pipeline for this run ---------------- */

            // --- measurement end
            long endTime = System.nanoTime();
            long memAfter = runtime.totalMemory() - runtime.freeMemory();

            long durationMs = (endTime - startTime) / 1_000_000; // ms
            long memUsedKb = (memAfter - memBefore) / 1024; // KB

            // Update stats
            minTime = Math.min(minTime, durationMs);
            maxTime = Math.max(maxTime, durationMs);
            totalTime += durationMs;

            minMem = Math.min(minMem, memUsedKb);
            maxMem = Math.max(maxMem, memUsedKb);
            totalMem += memUsedKb;
        }

        // Close repository after all runs
        fs.close();

        // Print results
        System.out.println("Execution time (ms): min=" + minTime + ", max=" + maxTime + ", avg=" + (totalTime / runs));
        System.out.println("Memory used (KB):   min=" + minMem + ", max=" + maxMem + ", avg=" + (totalMem / runs));
        System.out.println("Done!");
    }
}
