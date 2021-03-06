package testv2;
import battlecode.common.*;

import java.lang.reflect.Array;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int numScouts = 0;
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")

    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
        case LUMBERJACK:
                runLumberjack();
                break;
            case TANK:
                runTank();
            case SCOUT:
                runScout();
        }
    }

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction

                if (rc.canHireGardener(dir) && rc.getRoundNum() <= 150) {
                    rc.hireGardener(dir);
                } else if (rc.canHireGardener(dir) && Math.random() < 0.01) {
                    rc.hireGardener(dir);
                }

                // Move randomly
                // tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0, (int) myLocation.x);
                rc.broadcast(1, (int) myLocation.y);
                RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                if(robots.length>0){
                    rc.broadcast(2, (int) robots[0].location.x);
                    rc.broadcast(3, (int) robots[0].location.y);
                }
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

    static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Listen for home archon's location
                int xPos = rc.readBroadcast(0);
                int yPos = rc.readBroadcast(1);
                MapLocation archonLoc = new MapLocation(xPos,yPos);

                // Generate a random direction
                Direction dir = randomDirection();

                if(rc.getRoundNum() <= 100 && numScouts < 1) {
                    if (rc.canBuildRobot(RobotType.SCOUT, dir)) {
                        rc.buildRobot(RobotType.SCOUT, dir);
                        System.out.println("Number of scouts generated: " + numScouts);
                        numScouts++;
                    }
                } else {
                        // Randomly attempt to build a soldier or lumberjack in this direction
                        if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() > 0.5) {
                            rc.buildRobot(RobotType.SOLDIER, dir);
                            rc.broadcast(123, rc.readBroadcast(123) + 1);
                        } else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && rc.isBuildReady()) {
                            rc.buildRobot(RobotType.LUMBERJACK, dir);
                        }
                    }

                // Move in opposite direction of enemy when found
                // Move in opposite direction of enemy when found
                RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                if(enemyRobots.length > 0) {
                    MapLocation myLocation = rc.getLocation();
                    Direction toEnemy = myLocation.directionTo(enemyRobots[0].location);
                    MapLocation away = myLocation.subtract(toEnemy);
                    Direction awayFromEnemy = myLocation.directionTo(away);
                    tryMove(awayFromEnemy);

                    // Broadcast location to soldiers if enemies found
                    rc.broadcast(2, (int) enemyRobots[0].location.x);
                    rc.broadcast(3, (int) enemyRobots[0].location.y);
                } else {
                    // Move randomly
                    tryMove(randomDirection());
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }



    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();
        boolean dead = false;
        // The code you want your robot to perform every round should be in this loop
        while (true) {
            System.out.println("Current Soldier Count:" + rc.readBroadcast(123));
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode

            try {
                if (rc.getHealth() < 6 && rc.readBroadcast(123) > 0 && dead == false)
                {
                    rc.broadcast(123,rc.readBroadcast(123) - 1);
                    dead = true;
                }
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                RobotInfo[] friendly = rc.senseNearbyRobots(-1,rc.getTeam());
                MapLocation[] arcArray = rc.getInitialArchonLocations(rc.getTeam());

                MapLocation arcLocation = arcArray[0];
                // If there are some enemies detected nearby...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn..
                    rc.broadcast(2, Math.round(robots[0].location.x));
                    rc.broadcast(3, Math.round(robots[0].location.y));
                    RobotInfo[] friends = rc.senseNearbyRobots(-1, rc.getTeam());

                    rc.broadcast(707,rc.getRoundNum());
                    
                    if (rc.canFireTriadShot() && rc.senseNearbyRobots(-1, rc.getTeam()).length == 0 && ((rc.getLocation().directionTo(robots[0].location)).degreesBetween(rc.getLocation().directionTo(arcLocation))) > 90) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireTriadShot(rc.getLocation().directionTo(robots[0].location));
                    }
                    else if (rc.canFireSingleShot())
                    {
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }
                else
                {
                    if ((rc.getRoundNum()-rc.readBroadcast(707)) > 20)
                    {
                        System.out.println("Broadcast:" + (rc.getRoundNum()-rc.readBroadcast(707)));
                        rc.broadcast(1,0);
                        rc.broadcast(2,0);
                    }
                }

                float x = (rc.readBroadcast(2));
                float y = (rc.readBroadcast(3));

                MapLocation target = new MapLocation(x, y);

                // Movement control conditions and statments, with combat priority
                // Move towards target if not at range 6, to get into attack range
                if ((rc.readBroadcast(2) > 0) && (myLocation.distanceTo(target) > 6) && rc.senseNearbyBullets(2).length == 0)
                {
                    System.out.println("toward target");
                    tryMove(myLocation.directionTo(target));
                }
                // Pull back if too close, within range of 6 or if bullets nearby
                else if ((rc.readBroadcast(2) > 0) && (6 > (myLocation.distanceTo(target)) || rc.senseNearbyBullets(2).length > 0))
                {
                    System.out.println("away from target");
                    tryMove(rc.getLocation().directionTo(target).opposite());
                }
                // Auxiliary movement, set distance from archon + spacing
                // Move towards archon if further than distance of 45 from archon
                else if (rc.getLocation().distanceTo(arcLocation) > 45 && rc.canMove(rc.getLocation().directionTo(arcLocation)))
                {
                    System.out.println("toward archon");
                    tryMove(rc.getLocation().directionTo(arcLocation));
                }
                    // Move away from archon if closer than distance 35 from archon
                else if (rc.getLocation().distanceTo(arcLocation) < 35 && rc.canMove(rc.getLocation().directionTo(arcLocation).opposite()))
                {
                    System.out.println("away from archon");
                    tryMove(rc.getLocation().directionTo(arcLocation).opposite());
                }

                    // Move away from friendly[0] if soldier, need better way to do this
                else if (friendly.length > 0 && friendly[0].type == RobotType.SOLDIER)
                {
                    tryMove(rc.getLocation().directionTo(friendly[0].location).opposite());
                }

                else
                {
                    System.out.println("random");
                    tryMove(randomDirection());
                }

                TreeInfo[] trees = rc.senseNearbyTrees(-1);
                if (trees.length > 0) {
                    if (!trees[0].getTeam().equals(rc.getTeam())) {
                        if (trees[0].containedBullets > 0 || trees[0].containedRobot != null) {
                            rc.broadcast(12, (int) trees[0].location.x);
                            rc.broadcast(13, (int) trees[0].location.y);
                        }
                    }
                }


                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }


    static void runScout() throws GameActionException {
        System.out.println("I'm a scout!");
        Team enemy = rc.getTeam().opponent();
        boolean enemyChecker = true;
        boolean treeChecker = true;
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // Make array of all nearby trees and get bullets from them
                TreeInfo[] trees = rc.senseNearbyTrees(-1);
                MapLocation treeLocation;
                if(treeChecker && trees.length > 0) {
                        rc.broadcast(12, (int) trees[0].location.x);
                        rc.broadcast(13, (int) trees[0].location.y);
                        for (int i=0; i<=trees.length-1; i++) {
                            if (trees[i].containedBullets > 0) {
                                treeLocation = trees[i].getLocation();
                                rc.move(treeLocation);
                                rc.shake(treeLocation);
                            }
                        }
                    treeChecker = false;
                }

                if(enemyChecker) {
                    // Move randomly
                    tryMove(randomDirection());

                    if (robots.length > 0) {
                        rc.broadcast(4, (int) robots[0].location.x);
                        rc.broadcast(5, (int) robots[0].location.y);
                        enemyChecker = false;

                    }
                }else if(robots.length > 0){

                    tryMove(rc.getLocation().directionTo(robots[0].location));

                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));

                    }
                }



                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Scout Exception");
                e.printStackTrace();
            }
        }
    }

    static void runTank() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();
        boolean dead = false;
        // The code you want your robot to perform every round should be in this loop
        while (true) {
            System.out.println("Current tank Count:" + rc.readBroadcast(234));
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode

            try {
                if (rc.getHealth() < 6 && rc.readBroadcast(234) > 0 && dead == false)
                {
                    rc.broadcast(234,rc.readBroadcast(234) - 1);
                    dead = true;
                }
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                RobotInfo[] friendly = rc.senseNearbyRobots(-1,rc.getTeam());
                MapLocation[] arcArray = rc.getInitialArchonLocations(rc.getTeam());

                MapLocation arcLocation = arcArray[0];
                // If there are some enemies detected nearby...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn..
                    rc.broadcast(2, Math.round(robots[0].location.x));
                    rc.broadcast(3, Math.round(robots[0].location.y));
                    RobotInfo[] friends = rc.senseNearbyRobots(-1, rc.getTeam());

                    rc.broadcast(777,rc.getRoundNum());

                    if (rc.canFireTriadShot() && rc.senseNearbyRobots(-1, rc.getTeam()).length == 0 && ((rc.getLocation().directionTo(robots[0].location)).degreesBetween(rc.getLocation().directionTo(arcLocation))) > 90) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireTriadShot(rc.getLocation().directionTo(robots[0].location));
                    }
                    else if (rc.canFireSingleShot())
                    {
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }
                else
                {
                    if ((rc.getRoundNum()-rc.readBroadcast(777)) > 20)
                    {
                        System.out.println("Broadcast:" + (rc.getRoundNum()-rc.readBroadcast(777)));
                        rc.broadcast(1,0);
                        rc.broadcast(2,0);
                    }
                }

                float x = (rc.readBroadcast(2));
                float y = (rc.readBroadcast(3));

                MapLocation target = new MapLocation(x, y);

                //Movment control conditions and statments, with combat priority
                //Move towards target if not at range 6, to get into attack range
                if ((rc.readBroadcast(2) > 0) && (myLocation.distanceTo(target) > 6) && rc.senseNearbyBullets(2).length == 0)
                {
                    System.out.println("toward target");
                    tryMove(myLocation.directionTo(target));
                }
                //pull back if too close, within range of 6 or if bullets nearby
                else if ((rc.readBroadcast(2) > 0) && (6 > (myLocation.distanceTo(target)) || rc.senseNearbyBullets(2).length > 0))
                {
                    System.out.println("away from target");
                    tryMove(rc.getLocation().directionTo(target).opposite());
                }
                //Auxilary movment, set distance from archon + spacing
                //Move towards archon if further than distance of 45 from archon
                else if (rc.getLocation().distanceTo(arcLocation) > 45 && rc.canMove(rc.getLocation().directionTo(arcLocation)))
                {
                    System.out.println("toward archon");
                    tryMove(rc.getLocation().directionTo(arcLocation));
                }
                //Move away from archon if closer than distance 35 from archon
                else if (rc.getLocation().distanceTo(arcLocation) < 35 && rc.canMove(rc.getLocation().directionTo(arcLocation).opposite()))
                {
                    System.out.println("away from archon");
                    tryMove(rc.getLocation().directionTo(arcLocation).opposite());
                }

                //Move away from friendly[0] if soldier, need better way to do this
                else if (friendly.length > 0 && friendly[0].type == RobotType.SOLDIER)
                {
                    tryMove(rc.getLocation().directionTo(friendly[0].location).opposite());
                }

                else
                {
                    System.out.println("random");
                    tryMove(randomDirection());
                }

                TreeInfo[] trees = rc.senseNearbyTrees(-1);
                if (trees.length > 0) {
                    if (!trees[0].getTeam().equals(rc.getTeam())) {
                        if (trees[0].containedBullets > 0 || trees[0].containedRobot != null) {
                            rc.broadcast(12, (int) trees[0].location.x);
                            rc.broadcast(13, (int) trees[0].location.y);
                        }
                    }
                }


                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();
        boolean friendlyFire = false;
        boolean treeChop = false;
        boolean container = false;
        int treeContainer = 0;
        int k = 0;
        MapLocation treeLoc = new MapLocation(0,0);
        MapLocation empty = new MapLocation(0,0);
        RobotInfo[] lumberjackInfo = new RobotInfo[2];
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                int xPos = rc.readBroadcast(12);
                int yPos = rc.readBroadcast(13);
                treeLoc = new MapLocation(xPos,yPos);
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, null);
                for (int i = 0; i < robots.length; i++) {
                    if (robots[i].getTeam().opponent().equals(enemy)) {
                        friendlyFire = true;
                    }
                }
                if (robots.length > 0 && !rc.hasAttacked() && !friendlyFire) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                }
                TreeInfo[] trees = rc.senseNearbyTrees(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS);
                if (trees.length > 0) {
                    if (rc.canChop(trees[0].location) && !trees[0].getTeam().equals(rc.getTeam())) {
                        if (trees[0].containedBullets > 0 || trees[0].containedRobot != null) {
                            rc.broadcast(12, (int) trees[0].location.x);
                            rc.broadcast(13, (int) trees[0].location.y);
                        }
                        rc.chop(trees[0].ID);
                        treeChop = true;
                    }
                }
                // No close robots, so search for robots within sight radius
                robots = rc.senseNearbyRobots(-1, enemy);
                if(robots.length>0){
                    rc.broadcast(2, (int) robots[0].location.x);
                    rc.broadcast(3, (int) robots[0].location.y);
                }
                // no close trees, so search for trees within sight radius
                trees = rc.senseNearbyTrees(-1);
                // If there is a robot or tree, move towards it
                if (robots.length > 0 && !treeChop) {
                    MapLocation myLocation = rc.getLocation();
                    MapLocation enemyLocation = robots[0].getLocation();
                    Direction toEnemy = myLocation.directionTo(enemyLocation);
                    tryMove(toEnemy);
                }else if (trees.length > 0 && !(treeChop)) {
                    MapLocation myLocation = rc.getLocation();
                    MapLocation treeLocation = trees[k].location;
                    Direction toTree = myLocation.directionTo(treeLocation);
                    tryMove(toTree);
                }else if(!treeChop && !treeLoc.equals(empty)) {
                    Direction toTrees = rc.getLocation().directionTo(treeLoc);
                    tryMove(toTrees);
                }else if (!treeChop) {
                    // Move Randomly
                    tryMove(randomDirection());
                }

                friendlyFire = false;
                treeChop = false;
                container = false;
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }
//    static void moveShake()
    /**
     * Returns a random Direction
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float) Math.random() * 2 * (float) Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir, 20, 3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir           The intended direction of movement
     * @param degreeOffset  Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while (currentCheck <= checksPerSide) {
            // Try the offset of the left side
            if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
                return true;
            }
            // Try the offset on the right side
            if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI / 2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}