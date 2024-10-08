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
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Fork;

/**
 * Creates a processor chain that evaluates a function on events that mark the
 * start and the end of an "episode". The chain is represented graphically as:
 * <p>
 * <img src="{@docRoot}/doc-files/EpisodePattern.png" alt="Pattern" />
 * 
 * @author Sylvain Hallé
 */
public class EpisodePattern extends GroupProcessor
{
	/**
	 * Creates a new instance of the pattern.
	 * @param f_s The function to evaluate on the start of the episode
	 * @param f_e The function to evaluate on the end of the episode
	 * @param f_delta The function to evaluate on the two events identified as
	 * the start and the end of the episode
	 */
	public EpisodePattern(Function f_s, Function f_e, Function f_delta)
	{
		super(1, 1);
		Fork f = new Fork();
		FilterOn fo_s = new FilterOn(f_s);
		Connector.connect(f, 0, fo_s, 0);
		FilterOn fo_e = new FilterOn(f_e);
		Connector.connect(f, 1, fo_e, 0);
		ApplyFunction af = new ApplyFunction(f_delta);
		Connector.connect(fo_s, 0, af, 0);
		Connector.connect(fo_e, 0, af, 1);
		addProcessors(f, fo_s, fo_e, af);
		associateInput(f);
		associateOutput(af);
	}
}
