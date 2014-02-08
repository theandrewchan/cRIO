/*
    SARCS - Semi-Automatic Robot Control System
    v1.1 'junefire'

    This code is to be run on the cRIO.

    Copyright �2014 Tommy Bohde, Jeff Meli, and Ian Rahimi.

    SARCS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SARCS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SARCS. If not, see <http://www.gnu.org/licenses/>.
*/

package com.team1672.FRC2014;

import edu.wpi.first.wpilibj.AnalogChannel;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Jaguar;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.SimpleRobot;
import edu.wpi.first.wpilibj.Timer;

public class Robot extends SimpleRobot
{
  protected long ticks;

  /* Buttons */
  public final int FIRE_BUTTON = 1;
  public final int LIFT_UP_BUTTON = 2;
  public final int LIFT_DOWN_BUTTON = 3;
  public final int CAMERA_TOGGLE = 3;
  public final int COMPRESSOR_BUTTON = 4;

  /* Joystick USB Ports */ /* XXX: This may vary on different computers at different times */
  public final int kLeftJoystick = 1;
  public final int kRightJoystick = 2;

  public Joystick leftStick, rightStick;

  /* PWM Channels controlling motors */
  public final int[] kDrivetrain = {1, 2, 3, 4};
  public final int kLift = 5;
  public final int kCameraServo = 6; /* XXX: Remember to put a jumper next to this PWM channel */
  public final RobotDrive.MotorType[] motors = {RobotDrive.MotorType.kFrontLeft,
																								RobotDrive.MotorType.kFrontRight,
																								RobotDrive.MotorType.kRearLeft,
																								RobotDrive.MotorType.kRearRight};

  public final double[] cameraAngle = {0.7, 0.85};
  
  public RobotDrive drivetrain;
  public Jaguar lift;
  public Servo cameraServo;
	public Compressor compressor;
  public AnalogChannel ultrasonic;
	
	/* Relays (off of Digital Sidecar) */
  public final int kCompressor = 1;

  /* Digital IO (off of Digital Sidecar) */
  public final int kPressureSwitch = 13;

  /* Analog IO (off of cRIO module) */
  public final int kUltrasonic = 1;

  /* Solenoids (off of the cRIO NI 9472 module (the one with the LEDs on top)) */
  public final int[] kLeftSolenoid = {1, 2};
  public final int[] kRightSolenoid = {3, 4};

  public DoubleSolenoid leftSolenoid, rightSolenoid;
  
  //Temporary
  public final DigitalInput pressureSwitch;
  public long lastSwitchTime;
  public long lastReverseTime;
  public boolean isCompressorOn;
  public boolean isInverted;
  
  public Robot()
	{
	  pressureSwitch = new DigitalInput(kPressureSwitch);
	  lastSwitchTime = 0;
	  lastReverseTime = 0;
	  isCompressorOn = false;
	  isInverted = true;
    ticks = 1;

    leftStick = new Joystick(kLeftJoystick);
    rightStick = new Joystick(kRightJoystick);
    drivetrain = new RobotDrive(kDrivetrain[0],
                                kDrivetrain[1],
                                kDrivetrain[2],
                                kDrivetrain[3]);
	  /**
		 * Inverts all motors
		 */
		for(int i = 0; i < 4; i++)
			drivetrain.setInvertedMotor(motors[i], isInverted);
	  
    lift = new Jaguar(kLift);
    cameraServo = new Servo(kCameraServo);

    leftSolenoid = new DoubleSolenoid(kLeftSolenoid[0], kLeftSolenoid[1]);
    leftSolenoid.set(DoubleSolenoid.Value.kOff);
    rightSolenoid = new DoubleSolenoid(kRightSolenoid[0], kRightSolenoid[1]);
    rightSolenoid.set(DoubleSolenoid.Value.kOff);

    compressor = new Compressor(2, 13, 2, 1);
    cameraServo.set(0.7);
    ultrasonic = new AnalogChannel(1);
	}

	public void autonomous() 
	{
	  /* TODO: Implement autonomous. */
		System.out.println("Autonomous mode has no purpose currently; switch to manual operation.");
	}

	public void operatorControl() 
	{
	  System.out.println("Driver operation enabled.");
		drivetrain.setSafetyEnabled(false);
		lift.setSafetyEnabled(false);
	
		while(this.isOperatorControl() && this.isEnabled()) 
		{
			drivetrain.tankDrive(leftStick, rightStick);
     
		/* Z-axis is the throttle lever on the Logitech Attack 3 joystick;
     * it has a value on the interval [-1, 1],
		 * where -1 is physically located at the top of the lever (near the positive sign)
	   * and 1 is at the bottom of the lever (near the negative sign)
     */
			double stickZLeft = 1 - ((leftStick.getZ() + 1) / 2);
			double stickZRight = 1 - ((rightStick.getZ() + 1) / 2);
			double liftSpeed = 0.25; //(stickZLeft + stickZRight) / 2;
			
			if(rightStick.getRawButton(LIFT_DOWN_BUTTON))
				lift.set(-liftSpeed);
			else if(rightStick.getRawButton(LIFT_UP_BUTTON))
				lift.set(liftSpeed);
			else
				lift.set(0);

			/* Compressor */
			System.out.println("Pneumatic switch: " + pressureSwitch.get());
			System.out.println("Compressor: " + isCompressorOn);
	  
		  if((leftStick.getRawButton(5) || rightStick.getRawButton(5)) && (System.currentTimeMillis() - lastSwitchTime) >= 250)
		  {
			  lastSwitchTime = System.currentTimeMillis();
				isCompressorOn = !isCompressorOn;
		  }
		  if(rightStick.getRawButton(11) && (System.currentTimeMillis() - lastReverseTime) >= 250)
			{
			  lastReverseTime = System.currentTimeMillis();
			  isInverted = !isInverted;
			  drivetrain.setInvertedMotor(RobotDrive.MotorType.kFrontLeft, isInverted);
				drivetrain.setInvertedMotor(RobotDrive.MotorType.kFrontRight, isInverted);
			  drivetrain.setInvertedMotor(RobotDrive.MotorType.kRearLeft, isInverted);
			  drivetrain.setInvertedMotor(RobotDrive.MotorType.kRearRight, isInverted);
			}
		  if(isCompressorOn)
			  compressor.setRelayValue(Relay.Value.kForward);
			else
				compressor.setRelayValue(Relay.Value.kOff);

		  if(compressor.getPressureSwitchValue())
			  compressor.start();
	    else
		    compressor.stop();
			
	    /* Solenoids */
		  if(rightStick.getRawButton(FIRE_BUTTON))
			{
				leftSolenoid.set(DoubleSolenoid.Value.kForward);
	      rightSolenoid.set(DoubleSolenoid.Value.kForward);
			}
			else if(leftStick.getRawButton(FIRE_BUTTON))
			{
				leftSolenoid.set(DoubleSolenoid.Value.kReverse);
			  rightSolenoid.set(DoubleSolenoid.Value.kReverse);
			}
	    else
			{
				leftSolenoid.set(DoubleSolenoid.Value.kOff);
	      rightSolenoid.set(DoubleSolenoid.Value.kOff);
			}
		
			/* Camera controls */
//			if(leftStick.getRawButton(CAMERA_UP)) 
//			{
//				if(cameraAngle < 0.7)
//					cameraAngle += 0.1;
//				cameraServo.set(cameraAngle);
//				System.out.println(cameraAngle);
//	    }	
//			else if(leftStick.getRawButton(CAMERA_DOWN)) 
//			{
//				if (cameraAngle > -0.7)
//					cameraAngle -= 0.1;
//		    cameraServo.set(cameraAngle);
//				System.out.println(cameraAngle);
//			}

			/* Periodic diagnostic messages */
			if(ticks % 100 == 0) 
			{
				System.out.println("Current tick: " + ticks);
				System.out.println("Analog channel 1: " + ultrasonic.getValue());
			}
	    ticks++;
		}
	}

	public void test() 
	{
		System.out.println("Test mode enabled. \n");
		System.out.println(drivetrain.getDescription() + ", " + drivetrain.toString());
		System.out.println(leftStick.toString());
		System.out.println(rightStick.toString());
		System.out.println(leftSolenoid.toString());
		System.out.println(rightSolenoid.toString());
	}

	public void disabled() 
	{
	//	compressor.setRelayValue(Relay.Value.kForward);
	//	compressor.start();
	//  while (!this.isEnabled()) {
	//    if(!compressor.getPressureSwitchValue()) {
	//      compressor.start();
	//    } else {
	//      compressor.stop();
	//    }
	//  }
	}
}
