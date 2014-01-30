package org.frc4931.robot;

import org.frc4931.robot.command.ChangeDriveMode;
import org.frc4931.zach.control.FlightStick;
import org.frc4931.zach.control.LogitechAttack;
import org.frc4931.zach.control.LogitechPro;

public class OperatorInterface {
	public static FlightStick[] joysticks;
	/**
	 * Sets up the operator interface
	 */
	public static void init(){
		initJoysticks();
		initButtonCommands();
	}
	/**
	 * Creates new joystick objects
	 */
	public static void initJoysticks(){
		joysticks = new FlightStick[2];
		joysticks[0] = new LogitechPro(1);
		joysticks[1] = new LogitechAttack(2);
	}
	/**
	 * Initializes commands on joystick buttons.
	 */
	public static void initButtonCommands(){
		joysticks[0].buttons[7].whenPressed(new ChangeDriveMode(0));
		joysticks[0].buttons[8].whenPressed(new ChangeDriveMode(1));
		joysticks[0].buttons[9].whenPressed(new ChangeDriveMode(2));
		joysticks[0].buttons[11].whenPressed(new ChangeDriveMode(3));
	}
}
