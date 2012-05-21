package corbit.commons.util;

import java.util.Iterator;

public class PairEnum<T1, T2> implements Iterator<Pair<T1,T2>> {
	Iterator<T1> en1;
	Iterator<T2> en2;

	public PairEnum(Iterator<T1> e1, Iterator<T2> e2) {
		en1 = e1;
		en2 = e2;
	}

	public PairEnum(Iterable<T1> e1, Iterable<T2> e2) {
		en1 = e1.iterator();
		en2 = e2.iterator();
	}

	@Override
	public boolean hasNext() {
		return en1.hasNext() && en2.hasNext();
	}

	@Override
	public Pair<T1,T2> next() {
		return new Pair<T1,T2>(en1.next(), en2.next());
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
	}
}
