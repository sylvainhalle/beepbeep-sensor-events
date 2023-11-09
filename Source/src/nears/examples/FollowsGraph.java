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

import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;
import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.graphviz.ToDot;
import ca.uqac.lif.cep.graphviz.UpdateGraph;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.util.Booleans.Not;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Strings.ToString;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.json.JsonString;
import nears.HighlightedGraph;
import nears.LogRepository;
import nears.MultiDaySource;

/**
 * Draws a graph showing successive locations of motion sensor events.
 * <p>
 * The pipeline first filters out any event that is not from a motion
 * sensor. It then looks for the location of two successive motion events.
 * If these two locations differ (e.g. living &rarr; kitchen), an edge between
 * these two locations is added to a graph. The resulting graph shows the
 * number of times a pair of successive locations has been observed; for
 * example:
 * <p>
 * <img src="{@docRoot}/doc-files/FollowsGraph_output.png" alt="Graph" />
 * <p>
 * The pipeline corresponding to this calculation is illustrated below:
 * <p>
 * <img src="{@docRoot}/doc-files/FollowsGraph.png" alt="Pipeline" />
 * <p>
 * The pipeline actually produces a source file for the
 * <a href="https://graphviz.org">Graphviz</a> program; the graph shown above
 * is a beautified version of the real output.
 *  
 * @author Sylvain Hallé
 */
public class FollowsGraph
{
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		FileSystem fs = new LogRepository("0102").open();
		MultiDaySource feeder = new MultiDaySource(fs);
		OutputStream os = fs.writeTo("FollowsGraph.dot");
		
		/* Create the pipeline. */
		Pump p = new Pump();
		connect(feeder, p);
		Fork f0 = new Fork();
		connect(p, f0);
		ApplyFunction is_motion = new ApplyFunction(new FunctionTree(Equals.instance,
				new JPathFunction("sensor"),
				new Constant(new JsonString("motion"))));
		connect(f0, BOTTOM, is_motion, INPUT);
		Filter f_is_motion = new Filter();
		connect(is_motion, OUTPUT, f_is_motion, BOTTOM);
		connect(f0, TOP, f_is_motion, TOP);
		GetEdges ge = new GetEdges();
		connect(f_is_motion, ge);
		UpdateGraph u_graph = new UpdateGraph(new HighlightedGraph());
		connect(ge, TOP, u_graph, TOP);
		connect(ge, BOTTOM, u_graph, BOTTOM);
		KeepLast last = new KeepLast();
		connect(u_graph, last);
		//ApplyFunction to_dot = new ApplyFunction(new FunctionTree(ToDot.instance, RenameNodes.deleteLabels));
		ApplyFunction to_dot = new ApplyFunction(ToDot.instance);
		connect(last, to_dot);

		/* Connect the pipeline to an output and run. */
		connect(to_dot, new Print(new PrintStream(os)).setSeparator("\n"));
		p.run();
		
		/* Clean up. */
		os.close();
		fs.close();
	}
	
	/**
	 * The part of the pipeline that produces the edges between two locations.
	 * It is encapsulated into a {@link GroupProcessor} so it can be reused in
	 * other examples.
	 */
	public static class GetEdges extends GroupProcessor
	{
		public GetEdges()
		{
			super(1, 2);
			ApplyFunction get_id = new ApplyFunction(new FunctionTree(ToString.instance, new FunctionTree(new MergeScalars("location"), new JPathFunction("location"))));
			associateInput(INPUT, get_id, INPUT);
			Fork fork = new Fork(2);
			connect(get_id, fork);
			Trim trim = new Trim(1);
			connect(fork, BOTTOM, trim, INPUT);
			Fork f1 = new Fork();
			connect(fork, TOP, f1, INPUT);
			Fork f2 = new Fork();
			connect(trim, OUTPUT, f2, INPUT);
			ApplyFunction same = new ApplyFunction(new FunctionTree(Not.instance, Equals.instance));
			connect(f1, BOTTOM, same, TOP);
			connect(f2, TOP, same, BOTTOM);
			Fork f3 = new Fork();
			connect(same, f3);
			Filter fil_1 = new Filter();
			connect(f1, TOP, fil_1, TOP);
			connect(f3, TOP, fil_1, BOTTOM);
			Filter fil_2 = new Filter();
			connect(f2, BOTTOM, fil_2, TOP);
			connect(f3, BOTTOM, fil_2, BOTTOM);
			associateOutput(TOP, fil_1, OUTPUT);
			associateOutput(BOTTOM, fil_2, OUTPUT);
			addProcessors(get_id, fork, trim, f1, f2, same, f3, fil_1, fil_2);
		}
		
		@Override
		public GetEdges duplicate(boolean with_state)
		{
			return new GetEdges();
		}
	}

}
