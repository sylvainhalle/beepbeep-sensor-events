package experiments.orange4home.integration;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.Integrate;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.util.Booleans.And;
import ca.uqac.lif.cep.util.Booleans.Not;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.HtmlPrint;
import sensors.LogRepository;
import sensors.house.House;
import sensors.house.House.Location;
import sensors.house.House.Sensor;
import sensors.house.House.Subject;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;

public class NoMotionInDarkRoom_WithIntegration {

  /* Repository and format (shared) */
  protected static final LogRepository fs = new Orange4HomeLogRepository();
  protected static final EventFormat format = new Orange4HomeFormat();

  /* Rooms to cover (adapt to your dataset) */
  private static final String[] ROOMS = {
      "livingroom", "kitchen", "bedroom", "bathroom", "office", "hallway", "entrance"
  };

  /* presence == ON at room r  (Orange4Home: subject/model is "") */
  private static FunctionTree presenceOnAt(String room, EventFormat format) {
    return new FunctionTree(
        Equals.instance,
        new FunctionTree(
            new Maps.Get("presence", new Sensor()),
            new FunctionTree(new Maps.Get("", new Subject()), new Maps.Get(room, new Location()))
        ),
        new Constant(format.getOnConstant())
    );
  }

  /* light == 0 at room r */
  private static FunctionTree lightZeroAt(String room) {
    return new FunctionTree(
        Equals.instance,
        new FunctionTree(
            new Maps.Get("light", new Sensor()),
            new FunctionTree(new Maps.Get("", new Subject()), new Maps.Get(room, new Location()))
        ),
        new Constant(0)
    );
  }

  /* Global predicate: for all rooms, NOT(presence==ON AND light==0) */
  private static FunctionTree noMotionInDark_AllRooms(EventFormat format) {
    FunctionTree globalOk = null;
    for (String room : ROOMS) {
      FunctionTree violationAtRoom =
          new FunctionTree(And.instance, presenceOnAt(room, format), lightZeroAt(room));
      FunctionTree okAtRoom = new FunctionTree(Not.instance, violationAtRoom);
      globalOk = (globalOk == null) ? okAtRoom : new FunctionTree(And.instance, globalOk, okAtRoom);
    }
    return globalOk; // true iff all rooms satisfy the condition
  }

  public static void main(String[] args) throws FileSystemException, IOException {
    final int runs = 35; // number of repetitions

    long minTime = Long.MAX_VALUE, maxTime = Long.MIN_VALUE, totalTime = 0;
    long minMem  = Long.MAX_VALUE, maxMem  = Long.MIN_VALUE, totalMem  = 0;

    fs.open(); // open once

    for (int i = 0; i < runs; i++) {
      // Best-effort cleanup before each run
      Runtime rt = Runtime.getRuntime();
      rt.gc();

      long memBefore = rt.totalMemory() - rt.freeMemory();
      long start = System.nanoTime();

      /* ---------------- build + run pipeline (fresh each run) ---------------- */
      InputStream is = fs.readFrom("o4h_all_events.csv");
      Processor feeder = format.getFeeder(is);

      Pump p = new Pump();
      connect(feeder, p);

      ApplyFunction to_delta = new ApplyFunction(new House.EventToHouseDelta(format));
      connect(p, to_delta);

      Integrate house = new Integrate(new House());
      connect(to_delta, house);

      ApplyFunction checkAllRooms = new ApplyFunction(noMotionInDark_AllRooms(format));
      connect(house, checkAllRooms);

      // In-memory sink to avoid file I/O noise
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      KeepLast last = new KeepLast();
      connect(checkAllRooms, last);
      connect(last, new HtmlPrint(new PrintStream(baos)));

      // Execute
      p.run();
      feeder.stop();

      // Close per-run resources
      is.close();
      /* ---------------- end pipeline ---------------- */

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

    fs.close(); // close once

    System.out.println("----- Summary -----");
    System.out.println("Execution time (ms): min=" + minTime + ", max=" + maxTime + ", avg=" + (totalTime / runs));
    System.out.println("Memory used (KB):   min=" + minMem  + ", max=" + maxMem  + ", avg=" + (totalMem  / runs));
    System.out.println("Done!");
  }
}
