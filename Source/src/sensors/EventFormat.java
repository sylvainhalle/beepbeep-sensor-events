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
package sensors;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Date;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.tuples.Tuple;

/**
 * The purpose of the {@link EventFormat} class is to manipulate sensor events
 * at a level of abstraction that hides the superficial differences in the file
 * formats across multiple datasets. It assumes that events in a dataset have
 * the following features:
 * <ul>
 * <li>A <em>state</em>, which corresponds to a single value or reading
 * produced by a sensor at a given moment</li> 
 * <li>A <em>timestamp</em> that determines the time at which
 * a specific state reading has been produced</li>
 * <li>An <em>index</em>, which corresponds to the position of the
 * event in the physical ordering of the input source</li>
 * <li>Two attributes specifying the placement of the sensor in the
 * environment:<ul>
 *   <li>The <em>location</em>, which is the broad area (e.g. room) where the
 *   sensor is located</li>
 *   <li>The <em>subject</em>, which is the particular element (e.g. bedhead,
 *   stove, door) that the sensor is observing</li> 
 *   </ul></li>
 * <li>A <em>model</em>, which is the physical device producing the reading</li>
 * <li>The name of the <em>sensor</em> on the device that produces the
 * reading</li>
 * </ul>
 * It is further assumed that these last four attributes are nested within each
 * other (i.e. for a given <em>location</em>, the value of <em>subject</em>
 * uniquely defines a subject; for a given <em>location</em>-<em>subject</em>
 * pair, the value of <em>model</em> uniquely defines a physical device, etc.).
 * <p>
 * All returned functions take as input an event of a specific type, and
 * extract various features from this event.
 */
public interface EventFormat
{
	/**
	 * From a date represented in the (string) format of the event, returns a
	 * Java {@link java.util.Date Date} object for the corresponding moment.
	 * @param s The string
	 * @return The date object. The date may be {@code null} if the input
	 * string does not have the expected format.
	 */
	/*@ null @*/ public Date parseDate(String s);
	
	/**
	 * Returns the BeepBeep {@link Function} that fetches the timestamp in an
	 * event. The function must return a {@link Number} corresponding to the
	 * Unix timestamp of that event.
	 * @return The timestamp function
	 */
	/*@ non_null @*/ public Function timestamp();
	
	/**
	 * Returns the BeepBeep {@link Function} that fetches the state
	 * (i.e.<!-- --> value) produced by a sensor in an event. The function must
	 * return a string corresponding to the sensor's current state. 
	 * @return The state function
	 */
	/*@ non_null @*/ public Function stateString();
	
	/**
	 * Returns the BeepBeep {@link Function} that fetches the location
	 * of a sensor event. The function must return a string corresponding to the
	 * sensor's location. 
	 * @return The location function
	 */
	/*@ non_null @*/ public Function locationString();
	
	/**
	 * Returns the BeepBeep {@link Function} that fetches the subject
	 * of a sensor event. The function must return a string corresponding to the
	 * sensor's subject. 
	 * @return The subject function
	 */
	/*@ non_null @*/ public Function subjectString();
	
	/**
	 * Returns the BeepBeep {@link Function} that fetches the model
	 * of a sensor event. The function must return a string corresponding to the
	 * sensor's model. 
	 * @return The model function
	 */
	/*@ non_null @*/ public Function modelString();
	
	/**
	 * Returns the BeepBeep {@link Function} that fetches the sensor name
	 * of a sensor event. The function must return a string corresponding to the
	 * sensor's name. 
	 * @return The sensor function
	 */
	/*@ non_null @*/ public Function sensorString();
	
	/**
	 * Returns the BeepBeep {@link Function} that fetches an object that
	 * uniquely identifies the placement of a sensor event in an environment
	 * (typically the location and subject).
	 * @return The placement function
	 */
	/*@ non_null @*/ public Function sensorPlacement();
	
	/**
	 * Returns the BeepBeep {@link Function} that fetches an object that
	 * uniquely identifies a sensor event in an environment.
	 * @return The ID function
	 */
	/*@ non_null @*/ public Function sensorId();
	
	/**
	 * Returns the BeepBeep {@link Function} that fetches the event's index in
	 * the original source.
	 * @return The index function
	 */
	/*@ non_null @*/ public Function index();
	
	/**
	 * Returns the BeepBeep {@link Function} that checks if a sensor event is
	 * numeric. The function must return a boolean value indicating whether the
	 * sensor's state is numeric.
	 * <p>
	 * The default behavior is to simply look at an event's state (i.e. value)
	 * and check if this value is a number. This method can be overridden in
	 * subclasses to provide more sophisticated ways of determining if a sensor
	 * produces numerical values.
	 * @return The function
	 */
	/*@ non_null @*/ public default Function isNumeric()
	{
		return new FunctionTree(IsANumber.instance, stateString());
	}
	
	/**
	 * Returns the BeepBeep {@link Function} that checks if a sensor event is
	 * a temperature reading.
	 */
	/*@ non_null @*/ public Function isTemperature();
	
	/**
	 * Creates an object that uniquely identifies the placement of a device event
	 * in an environment.
	 * @param location The location of the sensor
	 * @param subject The subject in the location
	 * @param model The model of the sensor
	 * 
	 * @return The object corresponding to the placement
	 */
	public Tuple createPlacement(String location, String subject, String model);
	
	/**
	 * Creates an object that uniquely identifies the placement of a sensor in an
	 * environment.
	 * @param location The location of the sensor
	 * @param subject The subject in the location
	 * @param model The model of the sensor
	 * @param sensor The name of the sensor
	 * @return The object corresponding to the placement
	 */
	public Tuple createId(String location, String subject, String model, String sensor);
	
	/**
	 * Gets the sensor value corresponding to a "ON" state. This constant is
	 * platform-dependent.
	 * @return The sensor value
	 */
	public Object getOnConstant();
	
	/**
	 * Gets the sensor value corresponding to a "OFF" state. This constant is
	 * platform-dependent.
	 * @return The sensor value
	 */
	public Object getOffConstant();
	
	/**
	 * Gets the sensor value corresponding to a "OPEN" state. This constant is
	 * platform-dependent.
	 * @return The sensor value
	 */
	public Object getOpenConstant();
	
	/**
	 * Gets the sensor value corresponding to a "CLOSED" state. This constant is
	 * platform-dependent.
	 * @return The sensor value
	 */
	public Object getClosedConstant();
	
	/**
	 * Gets the extension that filenames of this format are expected to have.
	 * @return The extension, including the leading period (i.e. <tt>.json</tt>)
	 */
	public String getExtension();
	
	/**
	 * Gets a processor instance that can read events in the given format from
	 * an input stream.
	 * @param is The input stream to read from
	 * @return A processor, which is expected to have an arity of 0:1
	 */
	public Processor getFeeder(InputStream is);
	
	/**
	 * Gets a processor instance that can read events in the given format from
	 * one or more file paths, and optionally prints its progression into a
	 * print stream.                                  n
	 * @param out The print stream where status messages are to be sent
	 * @param filenames The filenames to read from
	 * @return A processor, which is expected to have an arity of 0:1
	 * @throws IOException If something goes wrong ;-)
	 */
	public Processor getFeeder(PrintStream out, String ... filenames) throws IOException;
	
	/**
	 * Evaluates an unary BeepBeep function.
	 * @param f The function
	 * @param inputs The arguments given to the function
	 * @return The output value of the function
	 */
	public static Object evaluateUnary(Function f, Object input)
	{
		Object[] outs = new Object[1];
		f.evaluate(new Object[] {input}, outs);
		return outs[0];
	}
}
