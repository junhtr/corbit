package corbit.commons.transition;

import java.util.HashMap;
import java.util.Map;

import corbit.tagdep.dict.CTBTagDictionary;

public class PDAction
{
	public static final PDAction SHIFT = new PDAction("S");
	public static final PDAction REDUCE_RIGHT = new PDAction("RR");
	public static final PDAction REDUCE_LEFT = new PDAction("RL");
	public static final PDAction NOT_AVAILABLE = new PDAction("NA");
	public static final PDAction END_STATE = new PDAction("E");
	public static final PDAction PENDING = new PDAction("SS");

	final String m_action;
	final String m_pos;

	static Map<String,PDAction> m_posActions;
	static Map<String,PDAction> m_sPosActions;
	static final int m_numPos;
	static final String[] m_posSet;
	static final CTBTagDictionary m_dict;

	static
	{
		m_dict = new CTBTagDictionary(true); // it doesn't matter even if closed tags are disabled.
		m_posActions = new HashMap<String,PDAction>();
		m_sPosActions = new HashMap<String,PDAction>();
		for (String sPos: m_dict.getTagList())
		{
			m_posActions.put(sPos, new PDAction("R-" + sPos));
			m_sPosActions.put(sPos, new PDAction("RS-" + sPos));
		}
		m_numPos = m_dict.getTagCount();
		m_posSet = m_dict.getTagList();
	}

	private PDAction(String s)
	{
		m_action = s;
		String[] p = m_action.split("-");
		m_pos = p.length > 1 ? p[1] : null;
	}

	public static PDAction getAction(int index)
	{
		if (index == 0)
			return SHIFT;
		else if (index == 1)
			return REDUCE_RIGHT;
		else if (index == 2)
			return REDUCE_LEFT;
		else if (index == 3)
			return NOT_AVAILABLE;
		else if (index == 4)
			return END_STATE;
		else if (index == 5)
			return PENDING;
		else if (index < 6 + m_numPos)
			return getPosAction(m_posSet[index - 6]);
		else
			throw new IllegalArgumentException();
	}

	public static int getActionIndex(PDAction act)
	{
		if (act == SHIFT)
			return 0;
		else if (act == REDUCE_RIGHT)
			return 1;
		else if (act == REDUCE_LEFT)
			return 2;
		else if (act == NOT_AVAILABLE)
			return 3;
		else if (act == END_STATE)
			return 4;
		else if (act == PENDING)
			return 5;
		else if (act.isPosAction() || act.isShiftPosAction())
			return 6 + m_dict.getTagIndex(act.getPos());
		else
			throw new IllegalArgumentException("Unknown act: " + act);
	}

	public static int getActionCount()
	{
		return 6 + m_numPos;
	}

	public static PDAction getPosAction(String sPos)
	{
//		assert(m_posActions.containsKey(sPos));
		return m_posActions.get(sPos);
	}

	public static PDAction getShiftPosAction(String sPos)
	{
//		assert(m_sPosActions.containsKey(sPos));
		return m_sPosActions.get(sPos);
	}

	public boolean isPosAction()
	{
		return m_action.startsWith("R-");
	}

	public boolean isShiftPosAction()
	{
		return m_action.startsWith("RS-");
	}

	public String getPos()
	{
		assert (isPosAction() || isShiftPosAction());
		return m_pos;
	}

	@Override
	public String toString()
	{
		return m_action;
	}

	public boolean shallowEquals(PDAction a)
	{
		return m_action.equals(a.m_action);
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof PDAction))
			return false;
		PDAction a = (PDAction)o;
		return m_action.equals(a.m_action);
	}

	@Override
	public int hashCode()
	{
		return m_action.hashCode() * 17 + 1;
	}
}
