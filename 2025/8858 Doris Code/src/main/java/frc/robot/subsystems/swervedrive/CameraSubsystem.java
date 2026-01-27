package frc.robot.subsystems.swervedrive;

import edu.wpi.first.cscore.CvSink;
import edu.wpi.first.cscore.CvSource;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.VideoSink;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import edu.wpi.first.cameraserver.CameraServer;
import frc.robot.Constants;

public class CameraSubsystem extends SubsystemBase{
    private UsbCamera camera_coral, camera_climb;
    private VideoSink server;
    private Thread cameraThread;

    private int camera_sel = 0;

    public CameraSubsystem(){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // Cameras
        camera_coral = CameraServer.startAutomaticCapture(Constants.OperatorConstants.CAM_CORAL);
        camera_coral.setResolution(160, 120);
        camera_coral.setFPS(Constants.OperatorConstants.CAM_FPS);
        camera_climb = CameraServer.startAutomaticCapture(Constants.OperatorConstants.CAM_CLIMB);
        camera_climb.setResolution(160, 120);
        camera_climb.setFPS(Constants.OperatorConstants.CAM_FPS);
        server = CameraServer.getServer();
        server.setSource(camera_coral);
        camera_sel = Constants.OperatorConstants.CAM_CLIMB;
        CvSink cvSink = CameraServer.getVideo(camera_coral);
        CvSource outputStream = CameraServer.putVideo("Rotated Coral Camera", 160, 120);

        // rotate the Coral Camera 90 degrees
        cameraThread = new Thread(() -> {

            Mat source = new Mat();
            Mat rotated = new Mat();

            while(!Thread.interrupted()) {
                if(cvSink.grabFrame(source) == 0) {
                    outputStream.notifyError(cvSink.getError());
                    continue;
                }
            }

            Core.rotate(source, rotated, Core.ROTATE_90_CLOCKWISE);
            outputStream.putFrame(rotated);

        });

        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    @Override
    public void periodic(){
    }

    public void SwitchCamera(int camera_select){

        if(camera_select == Constants.OperatorConstants.CAM_CORAL){
            // CORAL CAMERA
            server.setSource(camera_coral);
            camera_sel = camera_select;

        } else if (camera_select == Constants.OperatorConstants.CAM_CLIMB) {
            // CLIMB CAMERA
            server.setSource(camera_climb);
            camera_sel = camera_select;

        } else {
            // DEFAULT CASE
            server.setSource(camera_climb);
            camera_select = Constants.OperatorConstants.CAM_CLIMB;
        }
    }
}
