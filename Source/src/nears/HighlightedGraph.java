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
package nears;

import java.util.Set;

import ca.uqac.lif.cep.graphviz.Graph;

/**
 * A {@link Graph} with a customized display of its edges when exported to the
 * DOT format.
 */
public class HighlightedGraph extends Graph
{
	@Override
	public String renderEdge(Edge e)
	{
		float cur_weight = e.getWeight();
		boolean max = true;
		Set<Edge> outgoing = m_edges.get(e.getSource());
		for (Edge out : outgoing)
		{
			if (out.getWeight() > cur_weight)
			{
				max = false;
				break;
			}
		}
		String color = max ? "red" : "grey";
	  return "label=\"" + formatWeight(cur_weight) + "\"" + ",color=\"" + color + "\",fontcolor=\"" + color + "\"";
	}
	
	@Override
	public String renderVertex(String vertex)
  {
		String v = vertex.replaceAll("\\(location,\"(.*?)\"\\)", "$1");
    return "label=\"" + v + "\"";
  }
}
