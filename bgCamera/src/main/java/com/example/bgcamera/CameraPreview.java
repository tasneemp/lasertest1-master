package com.example.bgcamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Looper;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.bgcamera.config.CameraResolution;
import com.example.*;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.ContentValues.TAG;
import static android.content.Context.WINDOW_SERVICE;


@SuppressLint("ViewConstructor")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private CameraCallbacks mCameraCallbacks;
    public static String coord="0x0";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Size mPreviewSize;
    private Point[] mPreviewPoints;
    public long h,w;
    private CameraConfig mCameraConfig;
    private WindowManager mWindowManager;
    private volatile boolean safeToTakePicture = false;
    public static int x,y,hofimage,wofimage;
    CameraPreview(@NonNull Context context, CameraCallbacks cameraCallbacks) {
        super(context);

        mCameraCallbacks = cameraCallbacks;

        //Set surface holder
        initSurfaceView();
    }

    /**
     * Initilize the surface view holder.
     */
    private void initSurfaceView() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        //Do nothing
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //Do nothing
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mCamera == null) {  //Camera is not initialized yet.
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
            return;
        } else if (surfaceHolder.getSurface() == null) { //Surface preview is not initialized yet
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore: tried to stop a non-existent preview
        }

        // Make changes in preview size
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();

        //Sort descending
        Collections.sort(pictureSizes, new PictureSizeComparator());

        //set the camera image size based on config provided
        Camera.Size cameraSize;
        switch (mCameraConfig.getResolution()) {
            case CameraResolution.HIGH_RESOLUTION:
                cameraSize = pictureSizes.get(0);   //Highest res
                break;
            case CameraResolution.MEDIUM_RESOLUTION:
                cameraSize = pictureSizes.get(pictureSizes.size() / 2);     //Resolution at the middle
                break;
            case CameraResolution.LOW_RESOLUTION:
                cameraSize = pictureSizes.get(pictureSizes.size() - 1);       //Lowest res
                break;
            default:
                throw new RuntimeException("Invalid camera resolution.");
        }
        parameters.setPictureSize(cameraSize.width, cameraSize.height);
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_SHADE);
        requestLayout();


        mCamera.setParameters(parameters);
        Log.i("cambright", "Supported Exposure Modes:" + parameters.get("exposure-mode-values"));
        Log.i("cambright", "Supported White Balance Modes:" + parameters.get("whitebalance-values"));
        Log.i("tag", "Exposure setting = " + parameters.get("exposure"));
        Log.i("tag", "White Balance setting = " + parameters.get("whitebalance"));
        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

            safeToTakePicture = true;
        } catch (IOException | NullPointerException e) {
            //Cannot start preview
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Call stopPreview() to stop updating the preview surface.
        if (mCamera != null) mCamera.stopPreview();
    }

    /**
     * Initialize the camera and start the preview of the camera.
     *
     * @param cameraConfig camera config builder.
     */
    void startCameraInternal(@NonNull CameraConfig cameraConfig) {
        mCameraConfig = cameraConfig;

        if (safeCameraOpen(mCameraConfig.getFacing())) {
            if (mCamera != null) {
                requestLayout();

                try {
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                    mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
                }
            }
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    private Mat detectDocument(Bitmap src1)
    {
        Mat src=new Mat();
        Utils.bitmapToMat(src1,src);
        Mat blurred = src.clone();

        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2RGB);

        Log.d("channels", String.valueOf(src.channels())+String.valueOf(src));

        Imgproc.bilateralFilter(src,blurred,11,17,17,Core.BORDER_DEFAULT);
        Imgproc.cvtColor(blurred, blurred, Imgproc.COLOR_RGB2RGBA);

        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U),gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        List<Mat> blurredChannel = new ArrayList<Mat>();
        blurredChannel.add(blurred);
        List<Mat> gray0Channel = new ArrayList<Mat>();
        gray0Channel.add(gray0);

        MatOfPoint2f approxCurve;

        double maxArea = 0;
        int maxId = -1;

        for (int c = 0; c < 3; c++) {
            int ch[] = { c, 0 };
            Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

            int thresholdLevel = 1;
        for (int t = 0; t < thresholdLevel; t++) {
            if (t == 0) {
                Imgproc.Canny(gray0, gray, 10, 20, 3, true); // true ?
                Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                // ?
            } else {
                Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                        Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                        Imgproc.THRESH_BINARY,
                        (src.width() + src.height()) / 200, t);
            }

            Imgproc.findContours(gray, contours, new Mat(),
                    Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            for (MatOfPoint contour : contours) {
                MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                double area = Imgproc.contourArea(contour); //obtain contour area
                approxCurve = new MatOfPoint2f();
                Imgproc.approxPolyDP(temp, approxCurve,     //Approximates a polygonal curve with the specified precision.
                        Imgproc.arcLength(temp, true) * 0.02, true);
                //if the total number of curves is 4, it is likely to be quadrilateral and we also check if the area of this contour is maximum,
                //it is most likely to be our phone screen.
                if (approxCurve.total() == 4 && area >= maxArea) {
                    double maxCosine = 0;

                    List<Point> curves = approxCurve.toList();
                    for (int j = 2; j < 5; j++) {

                        double cosine = Math.abs(angle(curves.get(j % 4),
                                curves.get(j - 2), curves.get(j - 1)));
                        maxCosine = Math.max(maxCosine, cosine);
                    }

                    if (maxCosine < 0.3) {
                        maxArea = area;
                        maxId = contours.indexOf(contour);
                    }
                }
            }
        }
    }

  if (maxId >= 0) {
        Imgproc.drawContours(src, contours, maxId, new Scalar(255, 0, 0,
                .8), 8);

    }
        Rect rect = Imgproc.boundingRect(contours.get(maxId));
        Imgproc.rectangle(src, rect.tl(), rect.br(), new Scalar(255, 0, 0,.8), 2);
        Log.d("rect", String.valueOf(rect));
        int width = rect.width;
        int height = rect.height;
        Log.d("src", String.valueOf(src));
        Log.d("blurred", String.valueOf(blurred));
        Mat test=blurred.clone();
        blurred.convertTo(test,CvType.CV_32FC2);
        Log.d("test", String.valueOf(test));

        Point tr=new Point(rect.br().x,rect.tl().y);
        Point bl=new Point(rect.tl().x,rect.br().y);
        MatOfPoint2f src_mat = new MatOfPoint2f(
                rect.tl(), // tl
                tr, // tr
                rect.br(), // br
                 bl// bl
        );

        MatOfPoint2f dst_mat = new MatOfPoint2f(
                new org.opencv.core.Point(0,0), // awt has a Point class too, so needs canonical name here
                new org.opencv.core.Point(width,0),
                new org.opencv.core.Point(width,height),
                new org.opencv.core.Point(0,height)
        );

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
        Mat doc=new Mat(height,width,CvType.CV_32FC2);
        Imgproc.warpPerspective(blurred, doc, m, doc.size());
        src_mat.release();
        dst_mat.release();
        m.release();
        blurred.release();
        src.release();
        test.release();
        return doc;
    }


private double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
        / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
        + 1e-10);
        }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Bitmap detectLight(Bitmap bitmap, double gaussianBlurValue) {
        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

        Mat frame = new Mat();
        List<Mat> mChannels = new ArrayList<Mat>();
        Mat frameH;
        Mat frameV;
        Mat frameS;
        Mat frameH1,frameH2;
        // Convert it to HSV
        //Log.d("")
        Imgproc.cvtColor(rgba, frame, Imgproc.COLOR_RGB2HSV);
        // Split the frame into individual components (separate images for H, S,
        // and V)

        mChannels.clear();
        Core.split(frame, mChannels); // Split channels: 0-H, 1-S, 2-V
        frameH = mChannels.get(0);
        frameS = mChannels.get(1);
        frameV = mChannels.get(2);
        frameH1=frameH;frameH2=frameH;
        // Apply a threshold to each component
        Imgproc.threshold(frameH, frameH, 160, 180, Imgproc.THRESH_BINARY);
       // Imgproc.threshold(frameH, frameH2, 0, 10, Imgproc.THRESH_BINARY);
        //Core.bitwise_or(frameH1,frameH2,frameH);
        Imgproc.threshold(frameV, frameV, 200, 256, Imgproc.THRESH_BINARY);
        // Perform an AND operation
        Core.bitwise_and(frameH, frameV, frame);
        //
        //   Core.bitwise_and(frame,frameS,frame);

        //Mat grayScaleGaussianBlur = new Mat();
        //Imgproc.cvtColor(frame, grayScaleGaussianBlur, Imgproc.COLOR_HSV2BGR,1);
        //Imgproc.cvtColor(grayScaleGaussianBlur, grayScaleGaussianBlur, Imgproc.COLOR_BGR2GRAY,1);
        //Imgproc.GaussianBlur(grayScaleGaussianBlur, grayScaleGaussianBlur, new Size(gaussianBlurValue, gaussianBlurValue), 0);

        Core.MinMaxLocResult minMaxLocResultBlur = Core.minMaxLoc(frame);
        Imgproc.circle(rgba, minMaxLocResultBlur.maxLoc, 5, new Scalar(255), 1);

        double x1 = minMaxLocResultBlur.maxLoc.x;
        double y1=minMaxLocResultBlur.maxLoc.y;
        coord=Double.toString(x1)+"x"+Double.toString(y1);

        x=(int)x1;
        y=(int)y1 ;
        hofimage=bitmap.getHeight();
        wofimage=bitmap.getWidth();

        Intent intent1=new Intent("intentkey");
        intent1.putExtra("key",x+"x"+y);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent1);
        Bitmap resultBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, resultBitmap);
        rgba.release();
        frame.release();

        return resultBitmap;

    }

 /*       @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
     private Bitmap detectLight(Bitmap bitmap, double gaussianBlurValue) {

        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

       // Mat hsv = new Mat();
       // Mat mask = new Mat();
        Mat grayScaleGaussianBlur = new Mat();
        Imgproc.cvtColor(rgba, grayScaleGaussianBlur, Imgproc.COLOR_BGR2GRAY);
       // Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_BGR2HSV);
        Imgproc.GaussianBlur(grayScaleGaussianBlur, grayScaleGaussianBlur, new Size(gaussianBlurValue, gaussianBlurValue), 0);
        //Core.inRange(hsv,new Scalar(0,0,50),new Scalar(0,0,255),mask);
        Core.MinMaxLocResult minMaxLocResultBlur = Core.minMaxLoc(grayScaleGaussianBlur);
        Imgproc.circle(rgba, minMaxLocResultBlur.maxLoc, 5, new Scalar(255), 1);


        double x1 = minMaxLocResultBlur.maxLoc.x;
        double y1=minMaxLocResultBlur.maxLoc.y;
        coord=Double.toString(x1)+"x"+Double.toString(y1);

        x=(int)x1;
        y=(int)y1 ;
        hofimage=bitmap.getHeight();
        wofimage=bitmap.getWidth();

        Intent intent1=new Intent("intentkey");
        intent1.putExtra("key",x+"x"+y);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent1);
        Bitmap resultBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, resultBitmap);
        rgba.release();
  //      frame.release();

        return resultBitmap;


        x = (int)minMaxLocResultBlur.maxLoc.x;
        y=(int)minMaxLocResultBlur.maxLoc.y;
        coord=Integer.toString(x)+"x"+Integer.toString(y);
            Log.e(coord,"coordinates in service of image"+coord);
            String get1 = CameraPreview.coord;
            int hofimage=bitmap.getHeight();
            int wofimage=bitmap.getWidth();

            String cords[]=coord.split("x");

            /*Display mdisp = mWindowManager.getDefaultDisplay();
            android.graphics.Point mdispSize = new android.graphics.Point();
            mdisp.getSize(mdispSize);
            int maxX = mdispSize.x;
            int maxY = mdispSize.y;
            if(hofimage!=0)
            {
                x=x*maxX/hofimage;
                y=y*maxY/wofimage;
                //callfunction
            }
                Intent intent1=new Intent("intentkey");
            intent1.putExtra("key",x+"x"+y);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent1);
        //Bitmap resultBitmap = Bitmap.createBitmap(mask.cols(), mask.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(mask, resultBitmap);
        //return resultBitmap;
        Bitmap resultBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, resultBitmap);
        return resultBitmap;
    }
*/
    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            stopPreviewAndFreeCamera();

            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e("CameraPreview", "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    boolean isSafeToTakePictureInternal() {
        return safeToTakePicture;
    }

    void takePictureInternal() {
        safeToTakePicture = false;
        if (mCamera != null) {
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] bytes, Camera camera) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Convert byte array to bitmap
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            Log.d("bitmapval",String.valueOf(bitmap.describeContents()));


                            Mat rgba = detectDocument(bitmap);
                            Bitmap bmp = null;
                            bmp = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(rgba, bmp);
                            bmp=detectLight(bmp,41);
                            //bitmap = detectDocument(bitmap);
                            //Utils.matToBitmap(rgba, bitmap);
                            Log.d("bitmapval",String.valueOf(bitmap.describeContents()));
                            rgba.release();
                                //Save image to the file.
                              /*  if(HiddenCameraUtils.saveImageFromFile(bitmap,
                                    mCameraConfig.getImageFile(),
                                    mCameraConfig.getImageFormat()))*/
                            if(HiddenCameraUtils.saveImageFromFile(bmp,
                                    mCameraConfig.getImageFile(),
                                    mCameraConfig.getImageFormat())){
                                //Post image file to the main thread
                                new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mCameraCallbacks.onImageCapture(mCameraConfig.getImageFile());
                                    }
                                });
                            } else {
                                //Post error to the main thread
                                new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mCameraCallbacks.onCameraError(CameraError.ERROR_IMAGE_WRITE_FAILED);
                                    }
                                });
                            }

                            safeToTakePicture = true;
                            //h=bitmap.getHeight();
                            //w=bitmap.getWidth();
                            mCamera.startPreview();
                            stopPreviewAndFreeCamera();
                            Log.d("stpprevnfre","free cam");

                        }
                    }).start();
                }
            });
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
            safeToTakePicture = true;
        }
    }

    /**
     * When this function returns, mCamera will be null.
     */
    void stopPreviewAndFreeCamera() {
        safeToTakePicture = false;
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d("ifcon","free cam");
        }
        Log.d("outsideifcon","free cam");

    }
}