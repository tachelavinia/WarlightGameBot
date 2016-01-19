/**
 * Warlight AI Game Bot
 *
 * Last update: January 29, 2015
 *
 * @author Jim van Eeden
 * @version 1.1
 * @License MIT License (http://opensource.org/Licenses/MIT)
 */

package bot;

/**
 * This is a simple bot that does random (but correct) moves.
 * This class implements the Bot interface and overrides its Move methods.
 * You can implement these methods yourself very easily now,
 * since you can retrieve all information about the match from variable â€œstateâ€�.
 * When the bot decided on the move to make, it returns an ArrayList of Moves. 
 * The bot is started by creating a Parser to which you add
 * a new instance of your bot, and then the parser is started.
 */

import java.awt.font.NumericShaper;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import map.Region;
import map.SuperRegion;
import move.AttackTransferMove;
import move.PlaceArmiesMove;
import map.Pair;

public class BotStarter implements Bot {
	private static final double MINIMUM_OWNED_PERCENTAGE = 0.1;

	// queue of possible starting choices sorted by convenience
	PriorityQueue<Pair<Region, Integer>> startingRegions = null;
	private String myName = null;
	private String opponentName = null;
	ArrayList<Region> placedArmies;
	ArrayList<PlacementRegion> canAttack1 = new ArrayList<PlacementRegion>();
	ArrayList<PlacementRegion> canAttack2 = new ArrayList<PlacementRegion>();
	ArrayList<Region> canAttack3 = new ArrayList<Region>();
	ArrayList<Region> canAttack4 = new ArrayList<Region>();

	private int roundId = 0;


	public Region getStartingRegion(BotState state, Long timeOut) {
		Region chosen = null;
		// if it's the first time I choose a region, initialize the names
		if (myName == null) {
			myName = state.getMyPlayerName();
			opponentName = state.getOpponentPlayerName();

		}

		startingRegions = new PriorityQueue<Pair<Region, Integer>>();
		for (Region it : state.getPickableStartingRegions()) {
			startingRegions.add(new Pair<Region, Integer>(it, it
					.getRegionScore(state)));
		}

		// choose the region with the best score
		chosen = startingRegions.peek().getMember1();

		return chosen;
	}


	public ArrayList<PlaceArmiesMove> getPlaceArmiesMoves(BotState state,
			Long timeOut) {

		// Increment round id
		roundId++;

		ArrayList<PlaceArmiesMove> placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		int armiesLeft = state.getStartingArmies();
		int armies = 0;
		int needed = 0;
		PriorityQueue<PlacementRegion> placementRegions = new PriorityQueue<PlacementRegion>();
		PriorityQueue<SuperRegion> superRegionsInhabited;
		PriorityQueue<SuperRegion> superRegionsInhabitedCase4;
		canAttack1 = new ArrayList<PlacementRegion>();
		canAttack2 = new ArrayList<PlacementRegion>();
		canAttack3 = new ArrayList<Region>();
		canAttack4 = new ArrayList<Region>();


		superRegionsInhabited = new PriorityQueue<SuperRegion>();
		superRegionsInhabitedCase4 = new PriorityQueue<SuperRegion>();
		// If start of game prioritize case 3
		if (roundId < 4) {
			// CASE 3
			// /////////////////////////////
			for (SuperRegion superRegion : state.getVisibleMap()
					.getSuperRegions()) {
				superRegion.updateOwnedPercentage(myName);
				if (superRegion.getOwnedPercentage() > 0
						&& superRegion.getOwnedPercentage() < 1) {
					// Add partially owned super regions in queue
					superRegionsInhabited.add(superRegion);
				} else if (superRegion.getOwnedPercentage() == 1) {
					// Add owned super regions in queue
					superRegionsInhabitedCase4.add(superRegion);
				}
			}

			while (superRegionsInhabited.size() > 0) {
				SuperRegion superRegion = superRegionsInhabited.poll();

				// max number of neutral neighbours
				int max = Integer.MIN_VALUE;

				for (Region region : superRegion.getSubRegions()) {
					if (region.ownedByPlayer(myName)) {
						int counter_neutral = neutralNeighboursSameSuperRegion(region);
						// Update max
						if (counter_neutral > max) {
							max = counter_neutral;
						}
					}
				}

				// Max number of armies from regions with max number of neutral
				// neighbours
				int maxArmies = 0;
				// The region with that max
				Region maxNeutralRegion = null;

				// Find that region
				for (Region region : superRegion.getSubRegions()) {
					if (max == neutralNeighboursSameSuperRegion(region)
							&& region.getArmies() > maxArmies
							&& region.ownedByPlayer(myName)) {
						maxArmies = region.getArmies();
						maxNeutralRegion = region;
					}
				}

				// Number of armies needed to destroy neutral neighbours
				int counter = 0;
				for (Region r : maxNeutralRegion.getNeighbors()) {
					if (!r.ownedByPlayer(myName)
							&& !r.ownedByPlayer(opponentName)
							&& r.getSuperRegion().getId() == maxNeutralRegion
									.getSuperRegion().getId()) {
						counter += howManyToDestroy(r.getArmies());
					}
				}

				// Number of armies needed for attack
				needed = counter - maxNeutralRegion.getArmies() + 1;
				// Mark region as ready to attack
				canAttack3.add(maxNeutralRegion);

				if (needed > 0) {
					// Place armies
					if (needed >= armiesLeft && armiesLeft > 0) {
						// Add command
						placeArmiesMoves.add(new PlaceArmiesMove(myName,
								maxNeutralRegion, armiesLeft));
						// Set armies on that region
						maxNeutralRegion.setArmies(maxNeutralRegion.getArmies()
								+ armiesLeft);
						// Update number of armies left
						armiesLeft = 0;
					} else if (needed < armiesLeft) {
						// Add command
						placeArmiesMoves.add(new PlaceArmiesMove(myName,
								maxNeutralRegion, needed));
						// Set armies on that region
						maxNeutralRegion.setArmies(maxNeutralRegion.getArmies()
								+ needed);
						// Update number of armies left
						armiesLeft -= needed;
					}
				}

			}
		}

		for (SuperRegion superRegion : state.getVisibleMap().getSuperRegions()) {

			// CASE 1
			// ///////////////////////

			// Maximum number of enemies from same super region
			int max = 0;
			if (ownedPercentage(superRegion) >= MINIMUM_OWNED_PERCENTAGE) {
				for (Region r : superRegion.getSubRegions()) {
					if (r.ownedByPlayer(myName)
							&& numberOfEnemiesSameSuperRegion(r) > max) {
						max = numberOfEnemiesSameSuperRegion(r);
					}
				}
			}

			// If we are in case 1
			if (max > 0) {
				placementRegions.add(getPlacementRegionCase1(superRegion, max));
			}

			// CASE 2
			// /////////////////////////////

			max = 0; // Max number of neighbours from different super region
			// For all regions in super region
			// Compute max
			for (Region r : superRegion.getSubRegions()) {
				if (r.ownedByPlayer(myName)
						&& numberOfEnemiesDifferentSuperRegion(r) > max) {
					max = numberOfEnemiesDifferentSuperRegion(r);
				}
			}
			// If we are in case 2
			if (max > 0) {
				placementRegions.add(getPlacementRegionCase2(superRegion, max));
			}
		}




	

		// Treating cases 1 and 2
		// ///////////////////////
		while (placementRegions.size() > 0) {
			PlacementRegion toPlace = placementRegions.poll();
			if (toPlace.getNeeded() <= 0) {
				switch (toPlace.getPriority()) {
				case 3:
					canAttack1.add(toPlace);
				case 2:
					canAttack2.add(toPlace);
				}
			} else {
				// Place armies
				if (toPlace.getNeeded() > armiesLeft && armiesLeft > 0) {
					// Add command
					placeArmiesMoves.add(new PlaceArmiesMove(myName, toPlace
							.getRegion(), armiesLeft));
					// Set armies on that region
					toPlace.getRegion().setArmies(
							toPlace.getRegion().getArmies() + armiesLeft);
					// Update number of armies left
					armiesLeft = 0;
				} else if (toPlace.getNeeded() <= armiesLeft) {
					// Add command
					placeArmiesMoves.add(new PlaceArmiesMove(myName, toPlace
							.getRegion(), toPlace.getNeeded()));
					// Set armies on that region
					toPlace.getRegion().setArmies(
							toPlace.getRegion().getArmies()
									+ toPlace.getNeeded());
					// Mark region as ready to attack
					switch (toPlace.getPriority()) {
					case 3:
						canAttack1.add(toPlace);
					case 2:
						canAttack2.add(toPlace);
					}
					// Update number of armies left
					armiesLeft -= needed;
				}
			}

		}

		if (roundId >= 4) {
			// CASE 3
			// /////////////////////////////

			for (SuperRegion superRegion : state.getVisibleMap()
					.getSuperRegions()) {
				superRegion.updateOwnedPercentage(myName);
				if (superRegion.getOwnedPercentage() > 0
						&& superRegion.getOwnedPercentage() < 1) {
					// Add partially owned super regions in queue
					superRegionsInhabited.add(superRegion);
				} else if (superRegion.getOwnedPercentage() == 1) {
					// Add owned super regions in queue
					superRegionsInhabitedCase4.add(superRegion);
				}
			}

			while (superRegionsInhabited.size() > 0) {
				SuperRegion superRegion = superRegionsInhabited.poll();

				// max number of neutral neighbours
				int max = Integer.MIN_VALUE;

				for (Region region : superRegion.getSubRegions()) {
					if (region.ownedByPlayer(myName)) {
						int counter_neutral = neutralNeighboursSameSuperRegion(region);
						// Update max
						if (counter_neutral > max) {
							max = counter_neutral;
						}
					}
				}

				// Max number of armies from regions with max number of neutral
				// neighbours
				int maxArmies = 0;
				// The region with that max
				Region maxNeutralRegion = null;

				// Find that region
				for (Region region : superRegion.getSubRegions()) {
					if (max == neutralNeighboursSameSuperRegion(region)
							&& region.getArmies() > maxArmies
							&& region.ownedByPlayer(myName)) {
						maxArmies = region.getArmies();
						maxNeutralRegion = region;
					}
				}

				// Number of armies needed to destroy neutral neighbours
				int counter = 0;
				for (Region r : maxNeutralRegion.getNeighbors()) {
					if (!r.ownedByPlayer(myName)
							&& !r.ownedByPlayer(opponentName)
							&& r.getSuperRegion().getId() == maxNeutralRegion
									.getSuperRegion().getId()) {
						counter += howManyToDestroy(r.getArmies());
					}
				}

				// Number of armies needed for attack
				needed = counter - maxNeutralRegion.getArmies() + 1;
				// Mark region as ready to attack
				canAttack3.add(maxNeutralRegion);

				if (needed > 0) {
					// Place armies
					if (needed >= armiesLeft && armiesLeft > 0) {
						// Add command
						placeArmiesMoves.add(new PlaceArmiesMove(myName,
								maxNeutralRegion, armiesLeft));
						// Set armies on that region
						maxNeutralRegion.setArmies(maxNeutralRegion.getArmies()
								+ armiesLeft);
						// Update number of armies left
						armiesLeft = 0;
					} else if (needed < armiesLeft) {
						// Add command
						placeArmiesMoves.add(new PlaceArmiesMove(myName,
								maxNeutralRegion, needed));
						// Set armies on that region
						maxNeutralRegion.setArmies(maxNeutralRegion.getArmies()
								+ needed);
						// Update number of armies left
						armiesLeft -= needed;
					}
				}

			}
		}

		// CASE 4
		// ///////////////////////

		while (superRegionsInhabitedCase4.size() > 0) {
			SuperRegion superRegion = superRegionsInhabitedCase4.poll();

			// number of neutral neighbours
			int max = Integer.MIN_VALUE;

			for (Region region : superRegion.getSubRegions()) {
				if (region.ownedByPlayer(myName)) {
					int counter_neutral = neutralNeighboursDifferentSuperRegion(region);
					// Update max
					if (counter_neutral > max) {
						max = counter_neutral;
					}
				}
			}

			// Max number of armies from regions with max number of neutral
			// neighbours
			int maxArmies = 0;
			Region maxNeutralRegion = null;

			// Find that region
			for (Region region : superRegion.getSubRegions()) {
				if (max == neutralNeighboursDifferentSuperRegion(region)
						&& region.getArmies() > maxArmies
						&& region.ownedByPlayer(myName)) {
					maxArmies = region.getArmies();
					maxNeutralRegion = region;
				}
			}

			// Number of armies needed to destroy neutral neighbours
			int counter = 0;
			for (Region r : maxNeutralRegion.getNeighbors()) {
				if (!r.ownedByPlayer(myName)
						&& !r.ownedByPlayer(opponentName)
						&& r.getSuperRegion().getId() != maxNeutralRegion
								.getSuperRegion().getId()) {
					counter += howManyToDestroy(r.getArmies());
				}
			}

			// Number of armies needed for transfer
			needed = counter - maxNeutralRegion.getArmies() + 1;

			if (needed <= 0) {
				canAttack4.add(maxNeutralRegion);
			} else {
				// Place armies
				if (needed >= armiesLeft && armiesLeft > 0) {
					placeArmiesMoves.add(new PlaceArmiesMove(myName,
							maxNeutralRegion, armiesLeft));
					maxNeutralRegion.setArmies(maxNeutralRegion.getArmies()
							+ armiesLeft);
					armiesLeft = 0;
					canAttack4.add(maxNeutralRegion);
				} else if (needed < armiesLeft) {
					placeArmiesMoves.add(new PlaceArmiesMove(myName,
							maxNeutralRegion, needed));
					maxNeutralRegion.setArmies(maxNeutralRegion.getArmies()
							+ needed);
					canAttack4.add(maxNeutralRegion);
					armiesLeft -= needed;
				}
			}

		}

		if (armiesLeft > 0) {
			ArrayList<PlaceArmiesMove> r = placeLeftArmies(armiesLeft);
			placeArmiesMoves.addAll(r);
		}
		return placeArmiesMoves;
	}


	public ArrayList<AttackTransferMove> getAttackTransferMoves(BotState state,
			Long timeOut) {

		ArrayList<AttackTransferMove> attackTransferMoves = new ArrayList<AttackTransferMove>();



		// For each region in case 1
		for (PlacementRegion pR : canAttack1) {
		
			// Use placement region info to attack
			AttackTransferMove attackTransferMove = computeEnemyAttack(
					pR.getRegion(), pR.getToAttack());
			if (attackTransferMove != null) {
				attackTransferMoves.add(attackTransferMove);
			}
		}
	



		// For each region in case 2
		for (PlacementRegion pR : canAttack2) {



			// Use placement region info to attack
			AttackTransferMove attackTransferMove = computeEnemyAttack(
					pR.getRegion(), pR.getToAttack());
			if (attackTransferMove != null) {
				attackTransferMoves.add(attackTransferMove);
			}
		}

	



		// For each region in case 3
		for (Region region : canAttack3) {

	

			for (Region neighbour : region.getNeighbors()) {
				if (!neighbour.ownedByPlayer(myName)
						&& !neighbour.ownedByPlayer(opponentName)
						&& neighbour.getSuperRegion().getId() == region
								.getSuperRegion().getId()) {
					int attackArmies = howManyToDestroy(neighbour.getArmies());
					if (region.getArmies() > attackArmies) {
						// Make the attack
						attackTransferMoves.add(new AttackTransferMove(myName,
								region, neighbour, attackArmies));
						// Update armies
						region.setArmies(region.getArmies() - attackArmies);
					}
				}
			}
		}


		// For each region in case 4
		for (Region region : canAttack4) {



			for (Region neighbour : region.getNeighbors()) {
				if (!neighbour.ownedByPlayer(myName)
						&& !neighbour.ownedByPlayer(opponentName)
						&& neighbour.getSuperRegion().getId() != region
								.getSuperRegion().getId()) {
					int attackArmies = howManyToDestroy(neighbour.getArmies());
					if (region.getArmies() > attackArmies) {
						// Make the attack
						attackTransferMoves.add(new AttackTransferMove(myName,
								region, neighbour, attackArmies));
						// Update armies
						region.setArmies(region.getArmies() - attackArmies);
					}
				}
			}
		}



		// Attacking enemy regions and neutral regions if possible
		for (Region region : state.getVisibleMap().getRegions()) {
			if (region.ownedByPlayer(myName)) {
				for (Region neighbour : region.getNeighbors()) {
					// Attack enemies if possible
					if (neighbour.ownedByPlayer(opponentName)) {
						AttackTransferMove attackTransferMove = computeEnemyAttack(
								region, neighbour);
						if (attackTransferMove != null) {
							attackTransferMoves.add(attackTransferMove);
						}
					}
					// Attack neutral neighbours if possible
					if (!neighbour.ownedByPlayer(myName)
							&& !neighbour.ownedByPlayer(opponentName)
							&& neighbour.getSuperRegion().getId() != region
									.getSuperRegion().getId()) {
						int attackArmies = howManyToDestroy(neighbour
								.getArmies());
						if (region.getArmies() > attackArmies) {
							// Make the attack
							attackTransferMoves.add(new AttackTransferMove(
									myName, region, neighbour, attackArmies));
							// Update armies
							region.setArmies(region.getArmies() - attackArmies);
						}
					}
				}
			}
		}
		return attackTransferMoves;
	}

	public int howManyToDestroy(int defenders) {
		int result = ((int) (defenders / 0.504));
		return result + 1;
	}

	public double ownedPercentage(SuperRegion superregion) {
		double counter = 0;
		for (Region r : superregion.getSubRegions()) {
			if (r.ownedByPlayer(myName)) {
				counter++;
			}
		}

		return counter / superregion.getSubRegions().size();
	}

	public int numberOfEnemiesSameSuperRegion(Region region) {
		int counter = 0;
		for (Region r : region.getNeighbors()) {
			if (r.ownedByPlayer(opponentName)
					&& r.getSuperRegion().getId() == region.getSuperRegion()
							.getId()) {
				counter++;
			}
		}
		return counter;
	}

	public int numberOfEnemiesDifferentSuperRegion(Region region) {
		int counter = 0;
		for (Region r : region.getNeighbors()) {
			if (r.ownedByPlayer(opponentName)
					&& r.getSuperRegion().getId() != region.getSuperRegion()
							.getId()) {
				counter++;
			}
		}
		return counter;
	}

	public int howManyArmiesMaxNeighboursSameSuperRegion(Region region) {
		int max = 0;
		for (Region r : region.getNeighbors()) {
			if (r.ownedByPlayer(opponentName)
					&& r.getSuperRegion().getId() == region.getSuperRegion()
							.getId() && r.getArmies() > max) {
				max = r.getArmies();
			}
		}

		return max;
	}

	/**
	 * Compute new PlacementRegion for case 1
	 * 
	 * @param superRegion
	 *            The region of interest
	 * @param max
	 *            maximum number of enemy neighbours from same sR in this super
	 *            region (ALWAYS > 0)
	 * @return new PlacementRegion
	 */
	private PlacementRegion getPlacementRegionCase1(SuperRegion superRegion,
			int max) {
		Region maxRegion = null;
		Region toAttack;
		int needed;
		int maxArmies = 0;
		// Find from regions with max the one with max armies
		for (Region region : superRegion.getSubRegions()) {
			if (region.ownedByPlayer(myName)
					&& numberOfEnemiesSameSuperRegion(region) == max
					&& region.getArmies() > maxArmies) {
				maxArmies = region.getArmies();
				maxRegion = region;
			}
		}
		toAttack = toAttackCase1(maxRegion);
		needed = howManyToDestroy(toAttack.getArmies()) - maxRegion.getArmies()
				+ 1;
		return new PlacementRegion(maxRegion, toAttack, 3, needed);
	}

	/**
	 * Computes strongest enemy neighbour from same superregion
	 * 
	 * @param region
	 * @return The strongest neighbour or null if region does not have enemy
	 *         neighbours
	 */
	private Region toAttackCase1(Region region) {
		// Max number of armies from enemy neighbours
		int max = 0;
		Region toAttack = null;
		for (Region r : region.getNeighbors()) {
			if (r.ownedByPlayer(opponentName)
					&& r.getSuperRegion().getId() == region.getSuperRegion()
							.getId() && r.getArmies() > max) {
				max = r.getArmies();
				toAttack = r;
			}
		}
		return toAttack;
	}

	/**
	 * Compute new PlacementRegion for case 2
	 * 
	 * @param superRegion
	 *            The region of interest
	 * @param max
	 *            maximum number of enemy neighbours from different sR in this
	 *            super region (ALWAYS > 0)
	 * @return new PlacementRegion
	 */
	private PlacementRegion getPlacementRegionCase2(SuperRegion superRegion,
			int max) {
		Region maxRegion = null;
		Region toAttack;
		int needed;
		int maxArmies = 0;
		// Find from regions with max the one with max armies
		for (Region region : superRegion.getSubRegions()) {
			if (region.ownedByPlayer(myName)
					&& numberOfEnemiesDifferentSuperRegion(region) == max
					&& region.getArmies() > maxArmies) {
				maxArmies = region.getArmies();
				maxRegion = region;
			}
		}
		toAttack = toAttackCase2(maxRegion);
		needed = howManyToDestroy(toAttack.getArmies()) - maxRegion.getArmies()
				+ 1;
		return new PlacementRegion(maxRegion, toAttack, 2, needed);
	}

	/**
	 * Computes strongest enemy neighbour from different superregion
	 * 
	 * @param region
	 * @return The strongest neighbour or null if region does not have enemy
	 *         neighbours
	 */
	private Region toAttackCase2(Region region) {
		// Max number of armies from enemy neighbours
		int max = 0;
		Region toAttack = null;
		for (Region r : region.getNeighbors()) {
			if (r.ownedByPlayer(opponentName)
					&& r.getSuperRegion().getId() != region.getSuperRegion()
							.getId() && r.getArmies() > max) {
				max = r.getArmies();
				toAttack = r;
			}
		}
		return toAttack;
	}

	/**
	 * Checks if region and neighbour are neighbours
	 * 
	 * @param region
	 * @param neighbour
	 * @return true or false whether they are neighbours or not\
	 */
	private boolean isNeighbourWith(Region region, Region neighbour) {
		return region.getNeighbors().contains(neighbour);
	}

	public int howManyArmiesMaxNeighboursDifferentSuperRegion(Region region) {
		int max = 0;
		for (Region r : region.getNeighbors()) {
			if (r.ownedByPlayer(opponentName)
					&& r.getSuperRegion().getId() != region.getSuperRegion()
							.getId() && r.getArmies() > max) {
				max = r.getArmies();
			}
		}
		return howManyToDestroy(max);
	}

	public int neutralNeighboursSameSuperRegion(Region region) {
		int counterNeutral = 0;
		// Compute max from neighbours
		for (Region neighbour : region.getNeighbors()) {
			if (!neighbour.ownedByPlayer(myName)
					&& !neighbour.ownedByPlayer(opponentName)
					&& neighbour.getSuperRegion().getId() == region
							.getSuperRegion().getId()) {
				counterNeutral++;
			}
		}
		return counterNeutral;
	}

	public int neutralNeighboursDifferentSuperRegion(Region region) {
		int counterNeutral = 0;
		// Compute max from neighbours
		for (Region neighbour : region.getNeighbors()) {
			if (!neighbour.ownedByPlayer(myName)
					&& !neighbour.ownedByPlayer(opponentName)
					&& neighbour.getSuperRegion().getId() != region
							.getSuperRegion().getId()) {
				counterNeutral++;
			}
		}
		return counterNeutral;
	}

	/**
	 * Distributes remaining armies
	 * 
	 * @param armiesLeft
	 * @return Array with place moves
	 */
	private ArrayList<PlaceArmiesMove> placeLeftArmies(int armiesLeft) {
		// Array of moves to return
		ArrayList<PlaceArmiesMove> toReturn = new ArrayList<PlaceArmiesMove>();

		// Create an array for regions with higher priority of distribution
		ArrayList<Region> toPlaceIn1 = new ArrayList<Region>();
		for (PlacementRegion pR : canAttack1) {
			toPlaceIn1.add(pR.getRegion());
		}
		for (PlacementRegion pR : canAttack2) {
			toPlaceIn1.add(pR.getRegion());
		}

		// Set percent of total armies for these regions
		int armiesLeft1 = 70 * armiesLeft / 100;
		// Update total armies left
		armiesLeft -= armiesLeft1;

		// Start placing if there are regions to place on and armies to place
		if (toPlaceIn1.size() != 0 && armiesLeft1 > 0) {
			// Amount of armies for each region
			int ratio = toPlaceIn1.size() / armiesLeft1;
			// Place armies
			if (ratio == 0) { // If not enough armies for all regions
				for (Region region : toPlaceIn1) { // Place 1 army on what we
													// can
					toReturn.add(new PlaceArmiesMove(myName, region, 1));
					if (--armiesLeft1 == 0)
						break;
				}
			} else { // If enough armies
				for (Region region : toPlaceIn1) { // Place ratio armies on each
					toReturn.add(new PlaceArmiesMove(myName, region, ratio));
					armiesLeft -= ratio;
				}
			}
			// And place rest on first region
			toReturn.add(new PlaceArmiesMove(myName, toPlaceIn1.get(0),
					armiesLeft1));
		}

		// Another array for regions with lower priority
		ArrayList<Region> toPlaceIn2 = new ArrayList<Region>();
		toPlaceIn2.addAll(canAttack3);
		toPlaceIn2.addAll(canAttack4);

		// Set total armies for these regions
		int armiesLeft2 = armiesLeft;

		// Start placing if there are regions to place on and armies to place
		if (toPlaceIn2.size() != 0 && armiesLeft2 > 0) {
			// Amount of armies for each region
			int ratio = toPlaceIn2.size() / armiesLeft2;
			// Place armies
			if (ratio == 0) { // If not enough armies for all regions
				for (Region region : toPlaceIn2) { // Place 1 army on what we
													// can
					toReturn.add(new PlaceArmiesMove(myName, region, 1));
					if (--armiesLeft2 == 0)
						break;
				}
			} else { // If enough armies
				for (Region region : toPlaceIn2) { // Place ratio armies on each
					toReturn.add(new PlaceArmiesMove(myName, region, ratio));
					armiesLeft2 -= ratio;
				}
			}
			// And place rest on first region
			toReturn.add(new PlaceArmiesMove(myName, toPlaceIn2.get(0),
					armiesLeft2));
		}

		return toReturn;
	}

	private AttackTransferMove computeEnemyAttack(Region myRegion,
			Region toAttack) {
		int myArmy = myRegion.getArmies();
		int enemyArmy = toAttack.getArmies();
		int neededToDestroy = howManyToDestroy(enemyArmy);
		if (myArmy > neededToDestroy * 7) {
			myRegion.setArmies(myRegion.getArmies() - neededToDestroy * 7);
			return new AttackTransferMove(myName, myRegion, toAttack,
					neededToDestroy * 7);
		}
		if (myArmy > neededToDestroy * 5) {
			myRegion.setArmies(myRegion.getArmies() - neededToDestroy * 5);
			return new AttackTransferMove(myName, myRegion, toAttack,
					neededToDestroy * 5);
		}
		if (myArmy > neededToDestroy * 3) {
			myRegion.setArmies(myRegion.getArmies() - neededToDestroy * 3);
			return new AttackTransferMove(myName, myRegion, toAttack,
					neededToDestroy * 3);
		}
		if (myArmy > neededToDestroy * 2) {
			myRegion.setArmies(myRegion.getArmies() - neededToDestroy * 2);
			return new AttackTransferMove(myName, myRegion, toAttack,
					neededToDestroy * 2);
		}
		if (myArmy > neededToDestroy) {
			myRegion.setArmies(myRegion.getArmies() - neededToDestroy);
			return new AttackTransferMove(myName, myRegion, toAttack,
					neededToDestroy);
		}
		return null;
	}

	public static void main(String[] args) {
		BotParser parser = new BotParser(new BotStarter());
		parser.run();
		//
		// BotStarter bot = new BotStarter();
		// bot.startingRegions = new PriorityQueue<Pair<Region,Integer>>();
		// bot.startingRegions.add(new Pair<Region, Integer>(null, 5));
		// bot.startingRegions.add(new Pair<Region, Integer>(null, 7));
		// bot.startingRegions.add(new Pair<Region, Integer>(null, 3));
		//
		// Iterator<Pair<Region, Integer>> it = bot.startingRegions.iterator();
		// while(it.hasNext()){
		// if(it.next().getMember2() == 5){
		// it.remove();
		// }
		// }
		//
		// System.out.println(bot.startingRegions.poll().getMember2());
		// System.out.println(bot.startingRegions.poll().getMember2());
		//
		// SuperRegion r1 = new SuperRegion(1,1);
		// SuperRegion r2 = new SuperRegion(1,1);
		// SuperRegion r3 = new SuperRegion(1,1);
		// r1.setOwnedPercentage(0.1);
		// r2.setOwnedPercentage(0.8);
		// r3.setOwnedPercentage(0.65);
		// PriorityQueue<SuperRegion> pq = new PriorityQueue<SuperRegion>();
		// pq.add(r1);
		// pq.add(r2);
		// pq.add(r3);
		// System.out.println(pq.poll().getOwnedPercentage());

	}

}
