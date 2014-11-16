import lejos.nxt.*;
import lejos.robotics.navigation.*;  //contains differential pilot class

public class Maze {

	public static int[] openings = new int[3];
  	public static char[] turns = new char[32];
	public static int size = 0; // Keeps track of how many entries in turns

	public static DifferentialPilot pilot = new DifferentialPilot(5.0, 29.21, Motor.B, Motor.C); //create objects for Maze navigation
	public static UltrasonicSensor sonic = new UltrasonicSensor(SensorPort.S1);
	public static ColorSensor light = new ColorSensor(SensorPort.S3);

	// Decides to turn or go straight
	public static void move() {
		pilot.setTravelSpeed(6);
		pilot.setRotateSpeed(6);
		if (openings[2] == 1) {
			// Turn right
			makeRight();
			return;
		}
		
		if (openings[1] == 1) {
			// Go Straight
			lineFollow();
			return;
		}
		
		if (openings[0] == 1) {
			// Turn left
			makeLeft();
			return;
		}
	}
	
    public static void main (String[] args){
		pilot.setTravelSpeed(3.0);
		pilot.setRotateSpeed(6);
		boolean deadend = false;
	
		boolean debugReturn = false;
		if (debugReturn == true) {
			size = 4;
			turns[0] = 'l';
			turns[1] = 'r';
			turns[2] = 'r';
			turns[3] = 'l';
			
			pilot.setRotateSpeed(6);
			returnHome();
			Sound.beep();
			return;
		}
	
		while(true){
		
			int numTurns = checkTurns();
			// openings is repopulated
			if (numTurns == 2) { // Decision Point
				// check wether we turn left or right
				
				if (!deadend) {
					// right bias
					move();
					turns[size] = 'r';
					size++;
					// evacuate the block
					if (openings[2] == 0) {
						// This is the only case where we don't turn when going to the finish
						makeForward();
					}
				} else {
					// remember its facing the other way
					// turns are wrong; correct them
					turns[size - 1] = 'l';
					move(); // this will always turn rightmost option. we're facing the other way, so
					// we would make a right which would of been a left if we didn't mess up
					deadend = false;
				}
			} else if (numTurns == 1) {
				// either turn or straight, move() will figure it out
				move();
			} else if (numTurns == 0) { // Deadend
				pilot.travel(3.3);
				numTurns = checkTurns();
				if (numTurns == 0){
					deadend = true;
					// Do a 180
					pilot.travel(3.3);
					lineFollow();
					pilot.setRotateSpeed(4);
					pilot.rotate(32);
					if (endMazeCheck()){
						pilot.rotate(32);
						pilot.setRotateSpeed(6);
						returnHome();
						Sound.beep();
						return;
					}
					pilot.rotate(32);
					lineFollow();
					pilot.setRotateSpeed(6);
				}
			} else {
				System.out.println("TOO MANY OPENINGS");
			}
		}
    }
	
   
	public static boolean endMazeCheck(){
		if (light.getLightValue() > 215){       
			Sound.beep();
			return true;
		}
		return false;
	} 

	public static void lineFollow(){
		double w = 2.8;
		int i;
		for (i = 0; i < 10; i++) {
			pilot.rotate(Math.pow(-1,i) * w);
			w = w + 3.0;
			
			if (isBlack()) {
				pilot.travel(5.0);
				return;
			}
		}
		
		// Recenter robot
		pilot.rotate((w - 0.8) / 2);
		pilot.travel(2.5);
	}
  
	public static boolean isBlack(){
		return light.getLightValue() < 90;
	}
  
	public static int checkTurns() {
		openings[0] = 0;
		openings[1] = 0;
		openings[2] = 0;
		int numTurns = 0;
		
		int d = sonic.getDistance();
		LCD.drawString("R: " + d, lejos.nxt.LCD.SCREEN_WIDTH - 6 * lejos.nxt.LCD.FONT_WIDTH, lejos.nxt.LCD.FONT_HEIGHT * 1);
		if (d > 25){
			openings[2] = 1; // right
			numTurns++;
		}
		Motor.A.rotate(90);
		try {
			Thread.sleep(100);
		} catch (Exception e) {
			// nada
		}
		
		d = sonic.getDistance();
		LCD.drawString("S: " + d, lejos.nxt.LCD.SCREEN_WIDTH - 6 * lejos.nxt.LCD.FONT_WIDTH, lejos.nxt.LCD.FONT_HEIGHT * 2);
		if (d > 25){
			openings[1] = 1; // straight
			numTurns++;
		}
		Motor.A.rotate(90);
		try {
			Thread.sleep(100);
		} catch (Exception e) {
			// nada
		}
		
		d = sonic.getDistance();
		LCD.drawString("L: " + d, lejos.nxt.LCD.SCREEN_WIDTH - 6 * lejos.nxt.LCD.FONT_WIDTH, lejos.nxt.LCD.FONT_HEIGHT * 3);
		if (d > 25){
			openings[0] = 1; // left
			numTurns++;
		}
		Motor.A.rotate(-180);
		
		System.out.println("{" + openings[0] + ", " + openings[1] + ", " + openings[2] + "}");
		return numTurns;
	}
	
	public static void returnHome(){
		while (true) {
			int numTurns = checkTurns();
			// openings is repopulated
			if (numTurns == 2) { // Decision Point
				// read from list what our old decision was
				System.out.println("Size was: " + size);
				size = size - 1;
				char oldDecision = turns[size];
				
				if (oldDecision == 'r') {
					// Overwrite rightmost turns with nulls
					if (openings[2] == 1) {
						// Check for CLOSE OPEN OPEN (there is a left wall)
						if (openings[1] == 1) {
							// evacuate square
							makeForward();
						}
						openings[2] = 0;
						if (openings[0] == 1){
						makeLeft();
						}
					}
					else if (openings[1] == 1) {
						openings[1] = 0;
					}
				} else { // We took left the first time.
					if (openings[2] == 0) { // OPEN OPEN CLOSE  (there is a right wall)
						// evacuate the square
						makeForward();
					}
				}
				
				// if oldDecision was left we will always take the most right turn in move
				System.out.println("Old was: " + oldDecision);
				System.out.println("Size is: " + size);
				move();
			} else if (numTurns == 1) {
				move();
			} else if (numTurns == 0) {
				// to fix false deadends (ultrasonic is innacurate)
				if (size == 0) {
					// Only real deadend possible is home
					System.out.println("We're home!");
					Button.waitForPress();
					return;
				} else {
					System.out.println("ERR: FAKE DEADEND");
					pilot.travel(1);
				}
			} else {
				System.out.println("ERR: TOO MANY OPTION");
			}
		}
	}

// These functions are fined tuned for each square such that a right will assume
// that you're barely in the square; move forward to get in the center;
// make a right turn; evacuate the square
	public static void makeRight(){
		pilot.travel(16);
		pilot.rotate(31);
		pilot.travel(10.2);
		//lineFollow();
		return;
	}
  
	public static void makeLeft(){
		pilot.travel(16);
		pilot.rotate(-31);
		pilot.travel(10.2);
		//lineFollow();
		return;
	}
	
	public static void makeForward() {
		pilot.travel(24);
	}
}
