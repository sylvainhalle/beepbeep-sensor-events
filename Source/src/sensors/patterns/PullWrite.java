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
import ca.uqac.lif.cep.io.WriteOutputStream;
import ca.uqac.lif.cep.tmf.Pump;

/**
 * A {@link WriteOutputStream} processor that includes its own pump. Calling
 * {@link Processor#run() run()} on this processor therefore automatically
 * starts pulling events from upstream, until the end of the stream is
 * reached. As such, this processor does not bring any additional
 * functionality compared to chaining a {@link Pump} and a {@link WriteOutputStream},
 * except that it spares the user writing a Groovy script of a few keystrokes.
 * @author Sylvain Hallé
 */
public class PullWrite extends ca.uqac.lif.cep.GroupProcessor 
{
	/**
	 * The internal pump.
	 */
	private final Pump m_pump;

	public PullWrite()
	{
		super(1, 0);
		m_pump = new ca.uqac.lif.cep.tmf.Pump();
		WriteOutputStream pr = new WriteOutputStream(System.out);
		Connector.connect(m_pump, pr);
		associateInput(m_pump);
	}

	/**
	 * Instructs the processor to start pulling events from its upstream
	 * processor.
	 */
	public void run()
	{
		m_pump.run();
	}
}