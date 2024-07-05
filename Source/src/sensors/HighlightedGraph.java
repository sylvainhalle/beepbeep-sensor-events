/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023-2-24 Sylvain Hallé

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
package sensors;

import java.util.Set;

import ca.uqac.lif.cep.graphviz.Graph;

/**
 * A {@link Graph} with a customized display of its edges when exported to the
 * DOT format. More precisely, when an edge is associated to a weight in the
 * interval [0,1], a different color is used depending on the sub-interval in
 * which its weight lies.
 */
public class HighlightedGraph extends Graph
{
	@Override
	public String renderEdge(Edge e)
	{
		float cur_weight = e.getWeight();
		float total_weight = 0;
		Set<Edge> outgoing = m_edges.get(e.getSource());
		for (Edge out : outgoing)
		{
			total_weight += out.getWeight();
		}
		float fraction = cur_weight / total_weight;
		String color = getColor(fraction);
	  return "label=\"" + formatWeight(fraction) + "\"" + ",color=\"" + color + "\",fontcolor=\"" + color + "\"";
	}
	
	@Override
	public String renderVertex(String vertex)
  {
		String v = vertex.replaceAll("\\(location,\"(.*?)\"\\)", "$1");
    return "label=\"" + v + "\"";
  }
	
	/**
	 * Gets the HTML color associated to a fraction in the interval [0,1].
	 * @param fraction The fraction
	 * @return A string representing the HTML color
	 */
	protected static String getColor(float fraction)
	{
		if (fraction < 0.1)
		{
			return "#0A2F51";
		}
		else if (fraction < 0.2)
		{
			return "#0E4D64";
		}
		else if (fraction < 0.3)
		{
			return "#137177";
		}
		else if (fraction < 0.4)
		{
			return "#188977";
		}
		else if (fraction < 0.5)
		{
			return "#1D9A6C";
		}
		else if (fraction < 0.6)
		{
			return "#39A96B";
		}
		else if (fraction < 0.7)
		{
			return "#56B870";
		}
		else if (fraction < 0.8)
		{
			return "#74C67A";
		}
		else if (fraction < 0.9)
		{
			return "#99D492";
		}
		else
		{
			return "#BFE1B0";
		}
	}
}
