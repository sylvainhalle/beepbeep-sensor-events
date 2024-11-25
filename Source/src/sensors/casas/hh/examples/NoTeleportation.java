package sensors.casas.hh.examples;

import ca.uqac.lif.cep.graphviz.Graph;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;

public class NoTeleportation
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new HHLogRepository("hh115");
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();
	
	public static void main(String[] args)
	{
		
		Graph g = new Graph();
		addBidirectionalEdge(g, "M005", "M003");
		addBidirectionalEdge(g, "M005", "M003");
	}
	
	protected static void addBidirectionalEdge(Graph g, String end1, String end2)
	{
		g.add(end1, end2, 1);
		g.add(end2, end1, 1);
	}

}
