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
package sensors;

import ca.uqac.lif.cep.Duplicable;
import ca.uqac.lif.cep.functions.UnaryFunction;

public class CurrentActivity implements Duplicable
{
	protected String m_activity;

	protected int m_actCount;

	public CurrentActivity(String current)
	{
		super();
		m_actCount = 0;
		m_activity = current + m_actCount;
	}

	public CurrentActivity()
	{
		this("");
	}

	public void setActivity(String a)
	{
		m_activity = a + (++m_actCount);
	}

	public String getActivity()
	{
		return m_activity;
	}
	
	public static abstract class UpdateActivityFunction extends UnaryFunction<CurrentActivity,String>
	{
		public UpdateActivityFunction()
		{
			super(CurrentActivity.class, String.class);
		}
	}
	
	public static class NoUpdateActivity extends UpdateActivityFunction
	{
		public NoUpdateActivity()
		{
			super();
		}

		@Override
		public String getValue(CurrentActivity a)
		{
			return a.getActivity();
		}		
	}
	
	public static class UpdateActivity extends UpdateActivityFunction
	{
		protected final String m_to;
		
		public UpdateActivity(String to)
		{
			super();
			m_to = to;
		}

		@Override
		public String getValue(CurrentActivity a)
		{
			a.setActivity(m_to);
			return m_to;
		}		
	}

	@Override
	public CurrentActivity duplicate()
	{
		return duplicate(false);
	}

	@Override
	public CurrentActivity duplicate(boolean with_state)
	{
		return new CurrentActivity(m_activity);
	}
}