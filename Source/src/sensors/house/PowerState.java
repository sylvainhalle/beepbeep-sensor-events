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
package sensors.house;

import ca.uqac.lif.cep.Duplicable;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionException;
import ca.uqac.lif.cep.functions.UnaryFunction;
import sensors.EventFormat;

/**
 * A composite object maintaining the latest values of current, voltage
 * and power for a sensor.
 */
public class PowerState implements Duplicable
{
	/**
	 * The current value of the sensor.
	 */
	protected double m_current = -1;

	/**
	 * The voltage value of the sensor.
	 */
	protected double m_voltage = -1;

	/**
	 * The power value of the sensor.
	 */
	protected double m_power = -1;

	/**
	 * Sets the current value of the sensor.
	 * @param current The current value
	 */
	public void setCurrent(double current)
	{
		m_current = current;
	}

	/**
	 * Sets the voltage value of the sensor.
	 * @param voltage The voltage value
	 */
	public void setVoltage(double voltage)
	{
		m_voltage = voltage;
	}

	/**
	 * Sets the power value of the sensor.
	 * @param power The power value
	 */
	public void setPower(double power)
	{
		m_power = power;
	}

	/**
	 * Gets the current value of the sensor.
	 * @return The current value
	 */
	public double getCurrent()
	{
		return m_current;
	}

	/**
	 * Gets the voltage value of the sensor.
	 * @return The voltage value
	 */
	public double getVoltage()
	{
		return m_voltage;
	}

	/**
	 * Gets the power value of the sensor.
	 * @return The power value
	 */
	public double getPower()
	{
		return m_power;
	}

	@Override
	public String toString()
	{
		return "I = " + m_current + ", V = " + m_voltage + ", P = " + m_power;
	}

	@Override
	public PowerState duplicate()
	{
		return duplicate(false);
	}

	@Override
	public PowerState duplicate(boolean with_state)
	{
		PowerState ps = new PowerState();
		if (with_state)
		{
			ps.m_current = m_current;
			ps.m_voltage = m_voltage;
			ps.m_power = m_power;
		}
		return ps;
	}
	
	/**
	 * A function that sets the power value in a {@link PowerState} object.
	 */
	protected static class SetPower extends UnaryFunction<PowerState,PowerState>
	{
		/**
		 * The value to set.
		 */
		protected double m_value;

		public SetPower(double value)
		{
			super(PowerState.class, PowerState.class);
			m_value = value;
		}

		@Override
		public PowerState getValue(PowerState x)
		{
			x.setPower(m_value);
			return x;
		}
	}

	/**
	 * A function that sets the voltage value in a {@link PowerState} object.
	 */
	protected static class SetVoltage extends UnaryFunction<PowerState,PowerState>
	{
		/**
		 * The value to set.
		 */
		protected double m_value;

		public SetVoltage(double value)
		{
			super(PowerState.class, PowerState.class);
			m_value = value;
		}

		@Override
		public PowerState getValue(PowerState x)
		{
			x.setVoltage(m_value);
			return x;
		}
	}

	/**
	 * A function that sets the current value in a {@link PowerState} object.
	 */
	protected static class SetCurrent extends UnaryFunction<PowerState,PowerState>
	{
		/**
		 * The value to set.
		 */
		protected double m_value;

		public SetCurrent(double value)
		{
			super(PowerState.class, PowerState.class);
			m_value = value;
		}

		@Override
		public PowerState getValue(PowerState x)
		{
			x.setCurrent(m_value);
			return x;
		}
	}

	/**
	 * Converts a sensor event into a function that sets the current, voltage or
	 * power value in a {@link PowerState} object.
	 */
	public static class EventAsFunction extends UnaryFunction<Object,Function>
	{
		/**
		 * The format of the event.
		 */
		protected final EventFormat m_format;
		
		/**
		 * Creates a new instance of the function.
		 * @param format The format of the event
		 */
		public EventAsFunction(EventFormat format)
		{
			super(Object.class, Function.class);
			m_format = format;
		}

		@Override
		public Function getValue(Object x)
		{
			String quantity;
			double value;
			{
				Object[] out = new Object[1];
				m_format.sensorString().evaluate(new Object[] {x}, out);
				quantity = (String) out[0];
			}
			{
				Object[] out = new Object[1];
				m_format.stateString().evaluate(new Object[] {x}, out);
				value = Double.parseDouble((String) out[0]);
			}
			if (quantity.equals("instant_current"))
			{
				return new SetCurrent(value);
			}
			if (quantity.equals("instant_voltage"))
			{
				return new SetVoltage(value);
			}
			if (quantity.equals("instant_power"))
			{
				return new SetPower(value);
			}
			throw new FunctionException("Unknown quantity: " + quantity);
		}
	}
}