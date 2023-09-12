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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ca.uqac.lif.json.JsonElement;
import ca.uqac.lif.json.JsonMap;
import ca.uqac.lif.json.JsonParser;
import ca.uqac.lif.json.JsonParser.JsonParseException;

/**
 * A class containing utility properties and methods to manipulate JSON events
 * in the format of the platform. This avoids hard-coding them in each code
 * example.
 * 
 * @author Sylvain Hallé
 */
public abstract class SensorEvent
{
  /**
   * The date formatter used to parse the date string.
   */
  /*@ non_null @*/ public static final DateFormat DATE_FORMAT;

  /*@ non_null @*/ public static final String JP_LOCATION = "location";

  /*@ non_null @*/ public static final String JP_MODEL = "model";

  /*@ non_null @*/ public static final String JP_SENSOR = "sensor";

  /*@ non_null @*/ public static final String JP_STATE = "state";

  /*@ non_null @*/ public static final String JP_SUBJECT = "subject";

  /*@ non_null @*/ public static final String JP_TIMESTAMP = "sentAt/$date";

  /*@ non_null @*/ public static final String V_CONTACT = "contact";

  /*@ non_null @*/ public static final String V_TEMPERATURE = "temperature";

  protected static final JsonParser s_parser = new JsonParser();
  
  protected static final TimeZone s_utc = TimeZone.getTimeZone("UTC");
  
  static
  {
    // Sets the date format to print in Zulu time
    DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    DATE_FORMAT.setTimeZone(s_utc);
  }
  
  /**
   * Invisible constructor, as this class should not be instantiated.
   */
  private SensorEvent()
  {
    super();
  }

  /**
   * Creates a new sensor event in the format of the platform, by providing
   * values to each mandatory field. The method takes care of reproducing the
   * "quirks" of the format, such as encoding temperature values as strings
   * ending in <tt>" °C"</tt>.
   * @param location The value of the <tt>location</tt> field
   * @param subject The value of the <tt>subject</tt> field
   * @param model The value of the <tt>model</tt> field
   * @param timestamp A timestamp, expressed as a string in the expected format
   * (see {@link #DATE_FORMAT})
   * @param sensor The value of the <tt>sensor</tt> field
   * @param state The value of the <tt>state</tt> field
   * @return The sensor event
   */
  public static JsonElement newEvent(String location, String subject, String model, String timestamp, String sensor, Object state)
  {
    JsonMap map = new JsonMap();
    map.put(JP_LOCATION, location);
    map.put(JP_SUBJECT, subject);
    map.put(JP_MODEL, model);
    // Look out, this part is hard-coded as it is a nested field
    {
      JsonMap sentat = new JsonMap();
      sentat.put("$date", timestamp);
      map.put("sentAt", sentat);
    }
    map.put(JP_SENSOR, sensor);
    if (state instanceof String)
    {
      map.put(JP_STATE, state);
    }
    else if (state instanceof Number)
    {
      if (sensor.compareTo(V_TEMPERATURE) == 0)
      {
        map.put(JP_STATE, String.format("%.2f °C", ((Number) state).floatValue()));
      }
      else
      {
        map.put(JP_STATE, ((Number) state).floatValue());
      }
    }
    return map;
  }

  /**
   * Creates a new sensor event in the format of the platform, by providing
   * values to each mandatory field. The method takes care of reproducing the
   * "quirks" of the format, such as encoding temperature values as strings
   * ending in <tt>" °C"</tt>.
   * @param location The value of the <tt>location</tt> field
   * @param subject The value of the <tt>subject</tt> field
   * @param model The value of the <tt>model</tt> field
   * @param timestamp A Unix timestamp (in milliseconds)
   * @param sensor The value of the <tt>sensor</tt> field
   * @param state The value of the <tt>state</tt> field
   * @return The sensor event
   */
  public static JsonElement newEvent(String location, String subject, String model, long timestamp, String sensor, Object state)
  {
    return newEvent(location, subject, model, DATE_FORMAT.format(new Date(timestamp)), sensor, state);
  }
  
  /**
   * Creates a new sensor event in the format of the platform, by providing
   * values to each mandatory field. The method takes care of reproducing the
   * "quirks" of the format, such as encoding temperature values as strings
   * ending in <tt>" °C"</tt>.
   * @param location The value of the <tt>location</tt> field
   * @param subject The value of the <tt>subject</tt> field
   * @param model The value of the <tt>model</tt> field
   * @param timestamp A timestamp expressed as a Java {@link Date} object
   * @param sensor The value of the <tt>sensor</tt> field
   * @param state The value of the <tt>state</tt> field
   * @return The sensor event
   */
  public static JsonElement newEvent(String location, String subject, String model, Date timestamp, String sensor, Object state)
  {
    return newEvent(location, subject, model, DATE_FORMAT.format(timestamp), sensor, state);
  }

  /**
   * Parses a new sensor event from a JSON string.
   * @param content The string containing the event
   * @return The parsed JSON event, or <tt>null</tt> if the parsing could not
   * succeed
   */
  /*@ null @*/ public static JsonElement newEvent(/*@ null @*/ String content)
  {
    if (content == null)
    {
      return null;
    }
    try
    {
      return s_parser.parse(content);
    }
    catch (JsonParseException e)
    {
      return null;
    }
  }
}
