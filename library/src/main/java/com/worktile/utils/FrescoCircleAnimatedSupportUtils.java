package com.worktile.utils;

import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.animated.factory.AnimatedDrawableFactoryImplSupport;

import java.lang.reflect.Field;

/**
 * Created by Android Studio.
 * User: Moki
 * Email: mosicou@gmail.com
 * Date: 2017/7/28
 * Time: 16:16
 * Desc:
 */

public class FrescoCircleAnimatedSupportUtils {
    private static Class pipelineDraweeControllerClass;
    private static Field mAnimatedDrawableFactoryField;

    public static void setGifCircle(DraweeController controller, boolean gifCircle) {
        if (controller instanceof PipelineDraweeController) {
            try {
                if (pipelineDraweeControllerClass == null) {
                    pipelineDraweeControllerClass = controller.getClass();
                }
                if (mAnimatedDrawableFactoryField == null) {
                    mAnimatedDrawableFactoryField = pipelineDraweeControllerClass.getDeclaredField("mAnimatedDrawableFactory");
                    mAnimatedDrawableFactoryField.setAccessible(true);
                }

                AnimatedDrawableFactoryImplSupport animatedDrawableFactory = (AnimatedDrawableFactoryImplSupport) mAnimatedDrawableFactoryField.get(controller);
                animatedDrawableFactory.setGifCircle(gifCircle);

            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
