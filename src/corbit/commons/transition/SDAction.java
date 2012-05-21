package corbit.commons.transition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import corbit.commons.word.ArcLabel;

public class SDAction
{
	public static final SDAction SHIFT = new SDAction("S");
	public static final SDAction APPEND = new SDAction("A");
	public static final SDAction REDUCE = new SDAction("R");
	public static final SDAction REDUCE_RIGHT = new SDAction("RR");
	public static final SDAction REDUCE_LEFT = new SDAction("RL");
	public static final SDAction NOT_AVAILABLE = new SDAction("NA");
	public static final SDAction END_STATE = new SDAction("E");
	public static final SDAction PENDING = new SDAction("P");

	public static final int numMaxWordLength = 256;

	private static final Map<String,SDAction> m_tagActions;
	private static final Map<String,SDAction> m_shiftTagActions;
	private static final List<Map<String,SDAction>> m_chunkTagActions;
	private static final Map<ArcLabel,SDAction> m_labeledReduceLeftActions;
	private static final Map<ArcLabel,SDAction> m_labeledReduceRightActions;

	private static final String m_actTag = "T";
	private static final String m_actShiftTag = "ST";
	private static final String m_actChunkTag = "CT";
	private static final String m_actReduceLeft = "RL";
	private static final String m_actReduceRight = "RR";

	static
	{
		m_tagActions = new ConcurrentHashMap<String,SDAction>();
		m_shiftTagActions = new ConcurrentHashMap<String,SDAction>();
		m_chunkTagActions = new ArrayList<Map<String,SDAction>>();
		for (int i = 0; i < numMaxWordLength; ++i)
			m_chunkTagActions.add(new ConcurrentHashMap<String,SDAction>());
		m_labeledReduceLeftActions = new ConcurrentHashMap<ArcLabel,SDAction>();
		m_labeledReduceRightActions = new ConcurrentHashMap<ArcLabel,SDAction>();
	}

	private final String m_action;
	private final String m_tag;
	private final int m_length;

	private SDAction(String s)
	{
		this(s, 0, null);
	}

	private SDAction(String sAct, String sTag)
	{
		this(sAct, 0, sTag);
	}

	private SDAction(String sAct, int length, String sTag)
	{
		if (length < 0 || length > numMaxWordLength)
			throw new IllegalArgumentException();
		m_action = sAct;
		m_tag = sTag;
		m_length = length;
	}

	public static SDAction getLabeledReduceAction(boolean bRight, ArcLabel label)
	{
		String sLabel = label.toString();
		if (bRight)
		{
			if (!m_labeledReduceRightActions.containsKey(label))
				m_labeledReduceRightActions.put(label, new SDAction(m_actReduceRight, sLabel));
			return m_labeledReduceRightActions.get(label);
		}
		else
		{
			if (!m_labeledReduceLeftActions.containsKey(label))
				m_labeledReduceLeftActions.put(label, new SDAction(m_actReduceLeft, sLabel));
			return m_labeledReduceLeftActions.get(label);
		}
	}

	public static SDAction getLabeledReduceAction(boolean bRight, String sLabel)
	{
		ArcLabel label = ArcLabel.getLabel(sLabel);
		if (bRight)
		{
			if (!m_labeledReduceRightActions.containsKey(label))
				m_labeledReduceRightActions.put(label, new SDAction(m_actReduceRight, sLabel));
			return m_labeledReduceRightActions.get(label);
		}
		else
		{
			if (!m_labeledReduceLeftActions.containsKey(label))
				m_labeledReduceLeftActions.put(label, new SDAction(m_actReduceLeft, sLabel));
			return m_labeledReduceLeftActions.get(label);
		}
	}

	public static SDAction getTagAction(String sPos)
	{
		if (!m_tagActions.containsKey(sPos))
			m_tagActions.put(sPos, new SDAction(m_actTag, sPos));
		return m_tagActions.get(sPos);

	}

	public static SDAction getShiftTagAction(String sPos)
	{
		if (!m_shiftTagActions.containsKey(sPos))
			m_shiftTagActions.put(sPos, new SDAction(m_actShiftTag, sPos));
		return m_shiftTagActions.get(sPos);
	}

	public static SDAction getChunkTagAction(int length, String sPos)
	{
		if (length < m_chunkTagActions.size())
		{
			Map<String,SDAction> m = m_chunkTagActions.get(length - 1);
			if (!m.containsKey(sPos))
				m.put(sPos, new SDAction(m_actChunkTag, length, sPos));
			return m.get(sPos);
		}
		else
			throw new IllegalArgumentException("Extraordinary lengthy word found (" + length + ")");
	}

	public boolean isLabeledReduceRight()
	{
		return m_action == m_actReduceRight && m_tag != null;
	}

	public boolean isLabeledReduceLeft()
	{
		return m_action == m_actReduceLeft && m_tag != null;
	}

	public boolean isTagAction()
	{
		return m_action == m_actTag;
	}

	public boolean isShiftTagAction()
	{
		return m_action == m_actShiftTag;
	}

	public boolean isChunkTagAction()
	{
		return m_action == m_actChunkTag;
	}

	/**
	 * returns a part of speech for tagging actions, and an arc label for reduce
	 * actions
	 * 
	 * @return
	 */
	public String getTag()
	{
		return m_tag;
	}

	public int getLength()
	{
		return m_length;
	}

	@Override
	public String toString()
	{
		return m_length > 0 ?
				m_action + m_length + "-" + m_tag :
				m_tag != null ? m_action + "-" + m_tag : m_action;
	}

}
