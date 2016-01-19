package bot;

import map.Region;



public class PlacementRegion implements Comparable<PlacementRegion> {
	private Region region;
	private Region toAttack;
	private int priority;
	private int needed;
	
	public PlacementRegion(Region r, Region t, int p, int n) {
		region = r;
		toAttack = t;
		priority = p;
		needed = n;
	}

	public int compareTo(PlacementRegion arg0) {
		if(this.priority > arg0.priority) {
			return -1;
		}
		else
			if(this.priority < arg0.priority) {
				return 1;
			}
			else {
				return this.needed - arg0.needed ;
			}
	}
	
	public Region getToAttack() {
		return toAttack;
	}
	
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public Region getRegion() {
		return region;
	}
	public void setRegion(Region region) {
		this.region = region;
	}

	public int getNeeded() {
		return needed;
	}
	
	public String toString() {
		return "Region " + region.getId() + " with priority " + priority + " needed " + needed
				+ " toAttack " + toAttack.getId();
	}
	
}
