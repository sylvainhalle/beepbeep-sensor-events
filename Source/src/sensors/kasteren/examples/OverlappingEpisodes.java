/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023-2024 Sylvain Hallé, Rania Taleb

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
package sensors.kasteren.examples;

import static ca.uqac.lif.cep.Connector.connect;

import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Pump;
import sensors.kasteren.KasterenEventFormat;
import sensors.kasteren.KasterenEventFormat.KasterenFeeder;

public class OverlappingEpisodes
{

	protected static final KasterenEventFormat s_format = new KasterenEventFormat();
	
	public static void main(String[] args)
	{
		KasterenFeeder feeder = s_format.getFeeder(null);
		
		Pump p = new Pump();
		connect(feeder, p);
		Print print = new Print.Println();
		connect(p, print);
		
		p.run();

	}

}
