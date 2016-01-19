/**
 * Warlight AI Game Bot
 *
 * Last update: January 29, 2015
 *
 * @author Jim van Eeden
 * @version 1.1
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package map;
import java.util.LinkedList;

public class SuperRegion implements Comparable<SuperRegion>{
	
	private int id;
	private int armiesReward;
	private LinkedList<Region> subRegions;
	private Double ownedPercentage;
	
	public SuperRegion(int id, int armiesReward)
	{
		this.id = id;
		this.armiesReward = armiesReward;
		subRegions = new LinkedList<Region>();
	}
	
	public void addSubRegion(Region subRegion)
	{
		if(!subRegions.contains(subRegion))
			subRegions.add(subRegion);
	}
	
	/**
	 * @return A string with the name of the player that fully owns this SuperRegion
	 */
	public String ownedByPlayer()
	{
		String playerName = subRegions.getFirst().getPlayerName();
		for(Region region : subRegions)
		{
			if (!playerName.equals(region.getPlayerName()))
				return null;
		}
		return playerName;
	}
	
	/**
	 * @return The id of this SuperRegion
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * @return The number of armies a Player is rewarded when he fully owns this SuperRegion
	 */
	public int getArmiesReward() {
		return armiesReward;
	}
	
	/**
	 * @return A list with the Regions that are part of this SuperRegion
	 */
	public LinkedList<Region> getSubRegions() {
		return subRegions;
	}
	
	

	public double getOwnedPercentage() {
		return ownedPercentage;
	}

	public void setOwnedPercentage(double ownedPercentage) {
		this.ownedPercentage = ownedPercentage;
	}

	
	public int compareTo(SuperRegion arg0) {
		return - ownedPercentage.compareTo(arg0.getOwnedPercentage());
	}
	public void updateOwnedPercentage(String name) {
		Double counter = new Double(0);
		for(Region r: getSubRegions()) {
			if(r.ownedByPlayer(name)) {
				counter++;
			}
		}
		ownedPercentage = counter / subRegions.size();
	}
}
