package com.facebook.imagepipeline.animated.factory;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.Log;

import com.facebook.common.executors.DefaultSerialExecutorService;
import com.facebook.common.executors.SerialExecutorService;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.time.MonotonicClock;
import com.facebook.common.time.RealtimeSinceBootClock;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableOptions;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendImpl;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendImplSupport;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableCachingBackendImpl;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableCachingBackendImplProvider;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.core.ExecutorSupplier;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Android Studio.
 * User: Moki
 * Email: mosicou@gmail.com
 * Date: 2017/7/27
 * Time: 15:33
 * Desc: 在{@link AnimatedFactoryProvider}中被反射调用
 */

public class AnimatedFactoryImplSupport extends AnimatedFactoryImpl {
    private AnimatedDrawableBackendProvider mNormalAnimatedDrawableBackendProvider;
    private AnimatedDrawableBackendProvider mCircleAnimatedDrawableBackendProvider;
    private AnimatedDrawableUtil mAnimatedDrawableUtil;
    private AnimatedDrawableFactory mAnimatedDrawableFactory;
    private AnimatedImageFactory mAnimatedImageFactory;

    private ExecutorSupplier mExecutorSupplier;

    private PlatformBitmapFactory mPlatformBitmapFactory;

    public AnimatedFactoryImplSupport(PlatformBitmapFactory platformBitmapFactory,
                                      ExecutorSupplier executorSupplier) {
        super(platformBitmapFactory, executorSupplier);
        mPlatformBitmapFactory = platformBitmapFactory;
        this.mExecutorSupplier = executorSupplier;
    }

    private AnimatedDrawableFactory buildAnimatedDrawableFactory(
            final SerialExecutorService serialExecutorService,
            final ActivityManager activityManager,
            final AnimatedDrawableUtil animatedDrawableUtil,
            AnimatedDrawableBackendProvider normalAnimatedDrawableBackendProvider,
            AnimatedDrawableBackendProvider circleAnimatedDrawableBackendProvider,
            ScheduledExecutorService scheduledExecutorService,
            final MonotonicClock monotonicClock,
            Resources resources) {
        AnimatedDrawableCachingBackendImplProvider animatedDrawableCachingBackendImplProvider =
                new AnimatedDrawableCachingBackendImplProvider() {
                    @Override
                    public AnimatedDrawableCachingBackendImpl get(
                            AnimatedDrawableBackend animatedDrawableBackend,
                            AnimatedDrawableOptions options) {
                        return new AnimatedDrawableCachingBackendImpl(
                                serialExecutorService,
                                activityManager,
                                animatedDrawableUtil,
                                monotonicClock,
                                animatedDrawableBackend,
                                options);
                    }
                };

        return createAnimatedDrawableFactory(
                normalAnimatedDrawableBackendProvider,
                circleAnimatedDrawableBackendProvider,
                animatedDrawableCachingBackendImplProvider,
                animatedDrawableUtil,
                scheduledExecutorService,
                resources);
    }

    private AnimatedDrawableBackendProvider getNormalAnimatedDrawableBackendProvider() {
        if (mNormalAnimatedDrawableBackendProvider == null) {
            mNormalAnimatedDrawableBackendProvider = new AnimatedDrawableBackendProvider() {
                @Override
                public AnimatedDrawableBackend get(AnimatedImageResult animatedImageResult, Rect bounds) {
                    return new AnimatedDrawableBackendImpl(
                            getAnimatedDrawableUtil(),
                            animatedImageResult,
                            bounds);
                }
            };
        }
        return mNormalAnimatedDrawableBackendProvider;
    }

    private AnimatedDrawableBackendProvider getCircleAnimatedDrawableBackendProvider() {
        if (mCircleAnimatedDrawableBackendProvider == null) {
            mCircleAnimatedDrawableBackendProvider = new AnimatedDrawableBackendProvider() {
                @Override
                public AnimatedDrawableBackend get(AnimatedImageResult animatedImageResult, Rect bounds) {
                    return new AnimatedDrawableBackendImplSupport(
                            getAnimatedDrawableUtil(),
                            animatedImageResult,
                            bounds);
                }
            };
        }
        return mCircleAnimatedDrawableBackendProvider;
    }

    @Override
    public AnimatedDrawableFactory getAnimatedDrawableFactory(Context context) {
        if (mAnimatedDrawableFactory == null) {
            SerialExecutorService serialExecutorService =
                    new DefaultSerialExecutorService(mExecutorSupplier.forDecode());
            ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            mAnimatedDrawableFactory = buildAnimatedDrawableFactory(
                    serialExecutorService,
                    activityManager,
                    getAnimatedDrawableUtil(),
                    getNormalAnimatedDrawableBackendProvider(),
                    getCircleAnimatedDrawableBackendProvider(),
                    UiThreadImmediateExecutorService.getInstance(),
                    RealtimeSinceBootClock.get(),
                    context.getResources());
        }
        return mAnimatedDrawableFactory;
    }

    // We need some of these methods public for now so internal code can use them.

    private AnimatedDrawableUtil getAnimatedDrawableUtil() {
        if (mAnimatedDrawableUtil == null) {
            mAnimatedDrawableUtil = new AnimatedDrawableUtil();
        }
        return mAnimatedDrawableUtil;
    }

    private AnimatedImageFactory buildAnimatedImageFactory() {
        AnimatedDrawableBackendProvider normalAnimatedDrawableBackendProvider =
                new AnimatedDrawableBackendProvider() {
                    @Override
                    public AnimatedDrawableBackend get(AnimatedImageResult imageResult, Rect bounds) {
                        return new AnimatedDrawableBackendImpl(getAnimatedDrawableUtil(), imageResult, bounds);
                    }
                };

        AnimatedDrawableBackendProvider circleAnimatedDrawableBackendProvider =
                new AnimatedDrawableBackendProvider() {
                    @Override
                    public AnimatedDrawableBackend get(AnimatedImageResult imageResult, Rect bounds) {
                        return new AnimatedDrawableBackendImplSupport(getAnimatedDrawableUtil(), imageResult, bounds);
                    }
                };

        return new AnimatedImageFactoryImpl(normalAnimatedDrawableBackendProvider, mPlatformBitmapFactory);
    }

    @Override
    public AnimatedImageFactory getAnimatedImageFactory() {
        if (mAnimatedImageFactory == null) {
            mAnimatedImageFactory = buildAnimatedImageFactory();
        }
        return mAnimatedImageFactory;
    }

    protected AnimatedDrawableFactory createAnimatedDrawableFactory(
            AnimatedDrawableBackendProvider normalAnimatedDrawableBackendProvider,
            AnimatedDrawableBackendProvider circleAnimatedDrawableBackendProvider,
            AnimatedDrawableCachingBackendImplProvider animatedDrawableCachingBackendImplProvider,
            AnimatedDrawableUtil animatedDrawableUtil,
            ScheduledExecutorService scheduledExecutorService,
            Resources resources) {
        return new AnimatedDrawableFactoryImplSupport(
                normalAnimatedDrawableBackendProvider,
                circleAnimatedDrawableBackendProvider,
                animatedDrawableCachingBackendImplProvider,
                animatedDrawableUtil,
                scheduledExecutorService,
                resources);
    }
}
