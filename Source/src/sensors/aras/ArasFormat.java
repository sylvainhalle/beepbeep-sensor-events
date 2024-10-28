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
package sensors.aras;

import sensors.MultiResidentLabeledEventFormat;

public abstract class ArasFormat implements MultiResidentLabeledEventFormat
{
	/**
	 * The constant used to represent the "on" state of a sensor.
	 */
	public static final String S_ON = "ON";

	/**
	 * The constant used to represent the "off" state of a sensor.
	 */
	public static final String S_OFF = "OFF";

	/**
	 * The name of the attribute containing the timestamp in an event.
	 */
	public static final String P_TIMESTAMP = "timestamp";

	/**
	 * The name of the attribute containing the location in an event.
	 */
	public static final String P_LOCATION = "location";

	/**
	 * The name of the attribute containing the subject in an event.
	 */
	public static final String P_SUBJECT = "subject";

	/**
	 * The name of the attribute containing the model in an event.
	 */
	public static final String P_MODEL = "model";

	/**
	 * The name of the attribute containing the sensor in an event.
	 */
	public static final String P_SENSOR = "sensor";

	/**
	 * The name of the attribute containing the state in an event.
	 */
	public static final String P_STATE = "state";
	
	/**
	 * The name of the attribute containing the index in an event.
	 */
	public static final String P_INDEX = "index";
	
	/**
	 * The name of the attribute containing the activity of resident 1 in an
	 * event.
	 */
	public static final String P_ACTIVITY1 = "activity1";
	
	/**
	 * The name of the attribute containing the activity of resident 2 in an
	 * event.
	 */
	public static final String P_ACTIVITY2 = "activity2";
}
