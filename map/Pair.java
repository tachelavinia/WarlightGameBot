package map;

/** Pair that gets compared by the second member*/
public class Pair<O, S extends Comparable<S>> implements Comparable<Pair<O,S>>{
	O member1;
	S member2;
	public Pair(O member1, S member2) {
		this.member1 = member1;
		this.member2 = member2;
	}
	
	public int compareTo(Pair<O, S> o) {
		return member2.compareTo(o.member2);
	}
	public O getMember1() {
		return member1;
	}
	public void setMember1(O member1) {
		this.member1 = member1;
	}
	public S getMember2() {
		return member2;
	}
	public void setMember2(S member2) {
		this.member2 = member2;
	}
	
	public String toString() {
		return member1.toString() + " : " + member2.toString();
	}
	
}
