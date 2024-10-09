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
package sensors.patterns;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.mtnp.PrintGnuPlot;
import ca.uqac.lif.cep.mtnp.UpdateTableStream;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.mtnp.plot.Plot.ImageType;
import ca.uqac.lif.mtnp.plot.gnuplot.GnuPlot;

/**
 * Generates a plot from a set of streams processed by a set of processors.
 * The first processor generates the x-axis of the plot, while the other
 * processors generate the values of the y-axis. The plot is then output as
 * a GnuPlot document. Graphically, this pattern can be represented as follows:
 * <p>
 * <img src="{@docRoot}/doc-files/PlotPattern.png" alt="Processor graph">
 * @author Sylvain Hallé
 */
public class PlotPattern extends GroupProcessor
{
	/**
	 * The processor that generates the x-axis of the plot.
	 */
	/*@ non_null @*/ protected final Processor m_x;
	
	/**
	 * The processors that generate the values of y-axis of the plot (one
	 * processor per series).
	 */
	/*@ non_null @*/ protected final Processor[] m_ys;
	
	/**
	 * The names of the series in the plot (optional, may be null).
	 */
	/*@ null @*/ protected final String[] m_names;
	
	/**
	 * The plot object that will be used to display the data.
	 */
	/*@ non_null @*/ protected final GnuPlot m_plot;
	
	/**
	 * Creates a new instance of the plot pattern.
	 * @param names The names of the series in the plot (optional, may be null
	 * if no names are provided)
	 * @param pi The plot object that will be used to display the data
	 * @param x The processor generating the x-axis
	 * @param ys The processors generating the values of the y-axis of the plot
	 * (one processor per series)
	 */
	public PlotPattern(String[] names, GnuPlot pi, Processor x, Processor ... ys)
	{
		super(1, 1);
		m_names = new String[ys.length + 1];
		if (names != null)
		{
			if (names.length != ys.length + 1)
			{
				throw new IllegalArgumentException(
						"Number of names must be equal to the number of series plus one");
			}
			System.arraycopy(names, 0, m_names, 0, names.length);
		}
		m_x = x;
		m_ys = ys;
		if (m_ys.length == 0)
		{
			throw new IllegalArgumentException("PlotPattern must have at least one series");
		}
		m_plot = pi;
		Fork f = new Fork(ys.length + 1);
		Connector.connect(f, 0, x, 0);
		UpdateTableStream uts = new UpdateTableStream(m_names);
		addProcessors(f, x, uts);
		Connector.connect(x, 0, uts, 0);
		for (int i = 0; i < ys.length; i++)
		{
			Connector.connect(f, i + 1, ys[i], 0);
			Connector.connect(ys[i], 0, uts, i + 1);
			addProcessors(ys[i]);
		}
		// The KeepLast processor avoids generating a GnuPlot file for every
		// intermediate state of the table; this has a HUGE impact on performance
		KeepLast kl = new KeepLast();
		addProcessors(kl);
		Connector.connect(uts, kl);
		PrintGnuPlot pgp = new PrintGnuPlot(m_plot, ImageType.PNG);
		addProcessors(pgp);
		Connector.connect(kl, pgp);
		associateInput(f);
		associateOutput(pgp);
	}

	@Override
	public PlotPattern duplicate(boolean with_state)
	{
		Processor[] ys = new Processor[m_ys.length];
		for (int i = 0; i < m_ys.length; i++)
		{
			ys[i] = m_ys[i].duplicate(with_state);
		}
		return new PlotPattern(m_names, m_plot, m_x.duplicate(with_state), ys);
	}
}
