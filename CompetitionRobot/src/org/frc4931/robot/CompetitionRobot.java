package org.frc4931.robot;

import org.frc4931.robot.command.SetState;
import org.frc4931.robot.command.TwoState.State;
import org.frc4931.robot.command.drive.PIDDriveInterface;
import org.frc4931.robot.command.drive.PIDTurnInterface;
import org.frc4931.robot.command.groups.DriveAndScore;
import org.frc4931.robot.command.net.AddCommandAfterDelay;
import org.frc4931.robot.command.pneumatics.Pressurize;
import org.frc4931.robot.command.roller.RollIn;
import org.frc4931.robot.command.roller.RollOut;
import org.frc4931.robot.command.roller.StopRoller;
import org.frc4931.robot.subsystems.Compressor;
import org.frc4931.robot.subsystems.DriveTrain;
import org.frc4931.robot.subsystems.IMU;
import org.frc4931.robot.subsystems.Nets;
import org.frc4931.robot.subsystems.Ranger;
import org.frc4931.robot.subsystems.Roller;
import org.frc4931.robot.subsystems.RollerArm;
import org.frc4931.zach.drive.ContinuousMotor;
import org.frc4931.zach.drive.LimitedMotor;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.Ultrasonic;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class CompetitionRobot extends IterativeRobot{
	public static final long START_TIME = System.currentTimeMillis();
	public static final String[] AUTO_MODE_NAMES = new String[]{"Drive Straight and Score","Drop Ball, Turn Left, Drive Straight","Drop Ball, Turn Right, Drive Straight"};
	/*
	 * Constant Convention:
	 * SUBSYSTEM_COMPONET_POSITION_DESCRIPTOR
	 */
	public static boolean DRIVE_ENABLED = true;
	public static boolean COMPRESSOR_ENABLED = true;
	public static boolean ROLLER_ENABLED = true;
	public static boolean ARM_ENABLED = true;
	public static boolean NETS_ENABLED = true;
	
	/*Drive Train Constants*/
	public static final int DRIVE_MOTOR_FRONTLEFT = 1;
	public static final int DRIVE_MOTOR_FRONTRIGHT = 3;
	public static final int DRIVE_MOTOR_BACKLEFT = 2;
	public static final int DRIVE_MOTOR_BACKRIGHT = 4;
	
	/*Roller Constants*/
	public static final int ROLLER_MOTOR = 5;
	
	/*Net Constants*/
	public static final int NET_MOTOR_LEFT = 6;
	public static final int NET_SWITCH_LEFT = 3;
	public static final int NET_MOTOR_RIGHT = 7;
	public static final int NET_SWITCH_RIGHT = 2;
	public static final int NET_PROX_LEFT = 8;
	public static final int NET_PROX_RIGHT = 7;
	
	/*Compressor Constants*/
	public static final int COMPRESSOR_RELAY = 1;
	public static final int COMPRESSOR_PRESSURESWITCH = 1;
	
	/*Solenoid Constants*/
	public static final int SOLENOID_LEFT_EXTEND = 1;
	public static final int SOLENOID_LEFT_RETRACT = 2;
	public static final int SOLENOID_RIGHT_EXTEND = 3;
	public static final int SOLENOID_RIGHT_RETRACT = 4;
	
	public static final int GYRO_CHANNEL = 1;
	public static final int RANGER_CHANNEL = 2;
	
	public static final int ULTRASONIC_LEFT_PING = 11;
	public static final int ULTRASONIC_LEFT_ECHO = 12;
	
	public static final int ULTRASONIC_RIGHT_PING = 9;
	public static final int ULTRASONIC_RIGHT_EHCO = 10;
	
    // Wall following constants
    public static final double ROBOT_FRAME_LENGTH_IN_INCHES = 28.0;
    public static final double ROBOT_FRAME_WIDTH_IN_INCHES = 28.0;
    public static final double MINIMUM_RANGE_TO_GOAL_WALL_IN_INCHES = 36.0;
    public static final double TARGET_DISTANCE_FROM_ROBOT_TO_WALL_IN_INCHES = 12.0;
    public static final double TARGET_DISTANCE_FROM_CENTER_OF_ROBOT_TO_WALL_IN_INCHES = TARGET_DISTANCE_FROM_ROBOT_TO_WALL_IN_INCHES
                                                                                        + 0.5 * ROBOT_FRAME_WIDTH_IN_INCHES;
    public static final double TURN_SPEED_SCALE_FACTOR = 3.0;
    public static final double CORRECTION_RANGE_FACTOR = 0.6;

    public int autoMode = 0;
	public int driveMode = 1;
	public void robotInit(){
		Subsystems.robot = this;
		Subsystems.imu = new IMU(GYRO_CHANNEL);
		Subsystems.imu.reset();
		
		Subsystems.driveTrain = new DriveTrain(DRIVE_MOTOR_FRONTLEFT, DRIVE_MOTOR_BACKLEFT
				, DRIVE_MOTOR_FRONTRIGHT, DRIVE_MOTOR_BACKRIGHT, ContinuousMotor.SpeedControllerType.TALON);
//		Subsystems.driveTrain = new DriveTrain(1,2,ContinuousMotor.SpeedControllerType.JAGUAR);
		
		Subsystems.compressor = new Compressor(COMPRESSOR_RELAY, COMPRESSOR_PRESSURESWITCH);
		
		Subsystems.nets = new Nets
				( new LimitedMotor(NET_MOTOR_LEFT, LimitedMotor.SpeedControllerType.VICTOR, NET_SWITCH_LEFT, NET_PROX_LEFT)
				, new LimitedMotor(NET_MOTOR_RIGHT, LimitedMotor.SpeedControllerType.VICTOR, NET_SWITCH_RIGHT, NET_PROX_RIGHT));
		
		Subsystems.arm = new RollerArm(SOLENOID_LEFT_EXTEND,SOLENOID_LEFT_RETRACT,SOLENOID_RIGHT_EXTEND,SOLENOID_RIGHT_RETRACT);
		Subsystems.roller = new Roller(ROLLER_MOTOR, ContinuousMotor.SpeedControllerType.VICTOR);
		Subsystems.ranger = new Ranger(RANGER_CHANNEL);
		Subsystems.pid = new PIDController(0.5,0,0,Subsystems.ranger,new PIDDriveInterface());
		Subsystems.turnPID = new PIDController(0.003,0,0.007,Subsystems.imu, new PIDTurnInterface());
		
		Subsystems.leftUltrasonicSensor = new Ultrasonic(ULTRASONIC_LEFT_PING, ULTRASONIC_LEFT_ECHO, Ultrasonic.Unit.kInches);
		Subsystems.rightUltrasonicSensor = new Ultrasonic(ULTRASONIC_RIGHT_PING, ULTRASONIC_RIGHT_EHCO, Ultrasonic.Unit.kInches);
		
		Subsystems.leftUltrasonicSensor.setAutomaticMode(true);
		Subsystems.rightUltrasonicSensor.setAutomaticMode(true);
		
		//Subsystems.compressor.init();
		OperatorInterface.init();
		
		smartDashboardInit();
		
//		Subsystems.compressor.activate();
//		long then = System.currentTimeMillis();
//		while(System.currentTimeMillis()-then<100*1000){
//			Subsystems.compressor.activate();
//		}
//		Subsystems.compressor.deactive();
		Subsystems.compressor.init();
		
		//TODO Have drivers refine these values and make them constants in DriveTrain.
		SmartDashboard.putNumber("Range 1", 0.4);
		SmartDashboard.putNumber("Range 2", 0.8);
		SmartDashboard.putNumber("Range 3", 1.0);
		
		SmartDashboard.putNumber("Max Delta 1", 1.0);
		SmartDashboard.putNumber("Max Delta 2", 0.1);
		SmartDashboard.putNumber("Max Delta 3", 0.01);
		Scheduler.getInstance().add(new SetState(Subsystems.nets.leftNet, State.CLOSED, Nets.CLOSE_SPEED));
		
		Scheduler.getInstance().add(new AddCommandAfterDelay
				(new SetState(Subsystems.nets.rightNet, State.CLOSED, Nets.OPEN_SPEED),0.5));
	}
	
	public void smartDashboardInit(){
		/*Operator Interface Booleans*/
		SmartDashboard.putBoolean("Pressure Switch", false);
		SmartDashboard.putBoolean("Verbose", true);
		
		SmartDashboard.putNumber("Auto Mode", 0);
		
		SmartDashboard.putNumber("MinDriveSpeed", 0.3);
		SmartDashboard.putNumber("MaxDriveSpeed", 1.0);
		SmartDashboard.putNumber("DriveDeadZone", 0.08);
		
		SmartDashboard.putNumber("MinTurnSpeed", 0.3);
		SmartDashboard.putNumber("MaxTurnSpeed", 1.0);
		SmartDashboard.putNumber("TurnDeadZone", 0.1);
		
		SmartDashboard.putNumber("MaxApproachSpeed", 0.4);
		
		SmartDashboard.putData("Drive PID", Subsystems.pid);
		SmartDashboard.putData("Turn PID", Subsystems.turnPID);
		
		/*Net Override Commands*/
		SmartDashboard.putData("Close Left Net",new SetState(Subsystems.nets.leftNet, State.CLOSED));
		SmartDashboard.putData("Close Right Net",new SetState(Subsystems.nets.rightNet, State.CLOSED));
		SmartDashboard.putData("Open Left Net",new SetState(Subsystems.nets.leftNet, State.OPEN));
		SmartDashboard.putData("Open Right Net",new SetState(Subsystems.nets.rightNet, State.OPEN));

		/*Roller Arm Override Commands*/
		SmartDashboard.putData("Lower Roller Arm", new SetState(Subsystems.arm, State.DOWN));
		SmartDashboard.putData("Raise Roller Arm", new SetState(Subsystems.arm, State.UP));
	
		/*Roller Override Commands*/
		SmartDashboard.putData("Roll Arm In", new RollIn());
		SmartDashboard.putData("Roll Arm Out", new RollOut());
		SmartDashboard.putData("Stop Roller", new StopRoller());
		
//		SmartDashboard.putData("Turn 90", new TurnRelativeAngle(90));
//		SmartDashboard.putData("Drive Box", new DriveBox());
//		SmartDashboard.putData("Drive To Score",new DriveAndScore());
	}
	
	public void updateSmartDashboard(){
//		SmartDashboard.putString("Current Autonomous Mode", AUTO_MODE_NAMES[autoMode]);
//		/*Put Sensor Values*/
//		//output(""+Subsystems.leftUltrasonicSensor.getRangeInches());
//		output(""+Subsystems.rightUltrasonicSensor.getRangeInches());
		SmartDashboard.putNumber("Left Range",Subsystems.leftUltrasonicSensor.getRangeInches());
		SmartDashboard.putNumber("Right Range",Subsystems.rightUltrasonicSensor.getRangeInches());
		
		/*Put Subsystems*/
		Subsystems.driveTrain.putToDashboard();
		Subsystems.compressor.putToDashboard();
		Subsystems.roller.putToDashboard();
		Subsystems.arm.putToDashboard();
		Subsystems.ranger.putToDashboard();
		Subsystems.imu.putToDashboard();
		
		/*Put Subsystem Values*/
		SmartDashboard.putBoolean("Left Net Status", Subsystems.nets.leftNet.isOpen());
		SmartDashboard.putBoolean("Right Net Status", Subsystems.nets.rightNet.isOpen());
	}
	
	public void robotPeriodic(){
		if(Subsystems.compressor.testPressure()&&!isDisabled()){
			Scheduler.getInstance().add(new Pressurize());
		}
		autoMode = (int)SmartDashboard.getNumber("Auto Mode");
		autoMode = Math.max(autoMode, 0);
		autoMode = Math.min(autoMode, 2);
		updateSmartDashboard();
	}
	
	public void disabledPeriodic(){
		robotPeriodic();
	}
	
	public void teleopPeriodic(){
		robotPeriodic();
		Subsystems.driveTrain.drive(driveMode);
		Subsystems.driveTrain.update();
		Subsystems.roller.roll();
		updateSmartDashboard();

		Scheduler.getInstance().run();
	}
	
	public void autonomousInit(){  
	    //Scheduler.getInstance().add(new FollowWall(18));
        Scheduler.getInstance().add(new DriveAndScore());
		/*switch(autoMode){
			case 0:
				Scheduler.getInstance().add(new DriveAndScore());
				break;
			case 1:
				Scheduler.getInstance().add(new DropTurnLeftDrive());
				break;
			case 2:
				Scheduler.getInstance().add(new DropTurnRightDrive());
				break;
		}*/
	}
	
	public void autonomousPeriodic(){
		robotPeriodic();
		Scheduler.getInstance().run();
		Subsystems.driveTrain.update();
		Subsystems.roller.roll();
	}
	
	public static void output(String string){
//		if(SmartDashboard.getBoolean("Verbose")){
			System.out.println(Math.floor(getTime()/100.0d)/10.0d+":"+"\t"+string);
//		}
	}
	
	public static void printToUserConsole(String string){
	}
	
	public static long getTime(){
		return System.currentTimeMillis()-START_TIME;
	}
}
