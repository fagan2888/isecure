package com.infy.stg.isecure;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraUtil {

    private static final String TAG = CameraUtil.class.getName();

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    private final Activity mActivity;
    private final CameraView mCameraView;
    private final OverlayView mOverlayView;
    private final FaceDetectionCallback faceDetectionCallback;
    private final FaceCaptureCallback faceCaptureCallback;
    private OverlayResizeCallback overlayResizeCallback;

    private CameraManager mCameraManager;
    private String mCameraId;
    private StreamConfigurationMap mConfigMap;
    private Size mOverlaySize;
    private Size mPreviewSize;
    private Size mLargestSize;
    private Integer mSensorOrientation;
    private Integer mDisplayOrientation;
    private int mViewWidth;
    private int mViewHeight;


    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;


    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            mViewWidth = width;
            mViewHeight = height;
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            mViewWidth = width;
            mViewHeight = height;
            setupTransform();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
                    Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
                    if (faces != null && mode != null) {
                        mOverlayView.updateFace(faces);
                        faceDetectionCallback.onFace(mOverlayView.isCapturePossible());
                    }
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (null == aeState || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            try {
                createPreviewSession(mCameraView, mCaptureCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
//                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            if (null != mActivity) {
                mActivity.finish();
            }
        }
    };
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(() -> {
                Image mImage = reader.acquireNextImage();
                try {
                    ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    faceCaptureCallback.onFaceCaptured(bytes, mSensorOrientation);

                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    mImage.close();
                }
            });
        }
    };

    private int mState;
    private Integer mCardHeight;


    public CameraUtil(Activity activity, CameraView cameraView, OverlayView overlayView, FaceDetectionCallback faceDetectionCallback, FaceCaptureCallback faceCaptureCallback, OverlayResizeCallback overlayResizeCallback) {
        mActivity = activity;
        mCameraView = cameraView;
        mOverlayView = overlayView;
        this.faceDetectionCallback = faceDetectionCallback;
        this.faceCaptureCallback = faceCaptureCallback;
        this.overlayResizeCallback = overlayResizeCallback;
        init();
    }

    private void init() {
        mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                mCameraId = cameraId;
                CameraCharacteristics mCameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;
                mConfigMap = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (mConfigMap == null)
                    continue;

                mOverlaySize = mConfigMap.getOutputSizes(SurfaceTexture.class)[0];
                mDisplayOrientation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
                mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                mLargestSize = Collections.max(Arrays.asList(mConfigMap.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());

                break;
            }
        } catch (CameraAccessException e) {
//            e.printStackTrace();
        }
    }

    private void setupAspectRatio() {
        Point displaySize = new Point();
        mActivity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        int rotatedPreviewWidth = mViewWidth;
        int rotatedPreviewHeight = mViewHeight;
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;

        if (requiresDimensionSwap()) {
            rotatedPreviewWidth = mViewHeight;
            rotatedPreviewHeight = mViewWidth;
            maxPreviewWidth = displaySize.y;
            maxPreviewHeight = displaySize.x;
        }

        maxPreviewWidth = Math.min(maxPreviewWidth, MAX_PREVIEW_WIDTH);
        maxPreviewHeight = Math.min(maxPreviewHeight, MAX_PREVIEW_HEIGHT);

        mPreviewSize = chooseOptimalSize(mConfigMap.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, mLargestSize);

        if (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mCameraView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else {
            mCameraView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
        }
    }

    private void setupTransform() {
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, mViewWidth, mViewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) mViewHeight / mPreviewSize.getHeight(),
                    (float) mViewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mCameraView.setTransform(matrix);
    }

    private void setupOverlay() {
        if (mOverlayView != null) {


            ViewGroup.LayoutParams params = mOverlayView.getLayoutParams();
            params.width = mPreviewSize.getHeight();
            params.height = mPreviewSize.getWidth();
            mOverlayView.setLayoutParams(params);
            mOverlayView.setVisibility(View.VISIBLE);
            mOverlayView.updateSize(mOverlaySize);
            mOverlayView.requestLayout();
            overlayResizeCallback.onOverlayResized(params.width, params.height);
        }
    }


    private void createPreviewSession(CameraView cameraView, CameraCaptureSession.CaptureCallback mCaptureCallback, Handler mBackgroundHandler) throws CameraAccessException {
        SurfaceTexture texture = cameraView.getSurfaceTexture();
        assert texture != null;
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);

        mImageReader = ImageReader.newInstance(mLargestSize.getWidth(), mLargestSize.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

        mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewRequestBuilder.addTarget(surface);

        mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        if (null == mCameraDevice) {
                            return;
                        }
                        mCaptureSession = cameraCaptureSession;
                        try {

                            mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL);

                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                            mPreviewRequest = mPreviewRequestBuilder.build();
                            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                        } catch (CameraAccessException e) {
//                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
//                            showToast("Failed");
                    }
                }, null
        );
    }

    private void openCamera() {
        if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            return;

        setupAspectRatio();
        setupTransform();
        setupOverlay();

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            mCameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
//            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    protected void resume() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        if (mCameraView.isAvailable()) {
            openCamera();
        } else {
            mCameraView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    protected void pause() {
        closeCamera();
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void clickPicture() {
        Log.d(TAG, "ON CLICK");
        if (mOverlayView.isCapturePossible()) {
            shootSound();
            lockFocus();
        }

    }

    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
//            e.printStackTrace();
        }
    }

    private void captureStillPicture() {
        try {
            if (mCameraDevice == null)
                return;

            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));


            Log.e("FACE ORIENTATION", getOrientation(rotation) + "," + mSensorOrientation);

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void unlockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean requiresDimensionSwap() {
        switch (mDisplayOrientation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (mSensorOrientation == 90 || mSensorOrientation == 270)
                    return true;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (this.mSensorOrientation == 0 || this.mSensorOrientation == 180)
                    return true;
            default:
                Log.e(CameraUtil.class.getName(), "Display rotation is invalid: " + mDisplayOrientation);
                return false;
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(CameraUtil.class.getName(), "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }




    public void shootSound() {
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);
    }

    public interface FaceDetectionCallback {
        void onFace(boolean detected);
    }

    public interface FaceCaptureCallback {
        void onFaceCaptured(byte[] bytes, int orientation);
    }

    public interface OverlayResizeCallback {
        void onOverlayResized(int width, int Height);
    }

}
