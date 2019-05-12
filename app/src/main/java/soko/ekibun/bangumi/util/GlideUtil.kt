package soko.ekibun.bangumi.util

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager

object GlideUtil {
    /**
     * Begin a load with Glide by passing in a context.
     *
     *
     *  Any requests started using a context will only have the application level options applied
     * and will not be started or stopped based on lifecycle events. In general, loads should be
     * started at the level the result will be used in. If the resource will be used in a view in a
     * child fragment, the load should be started with [.with]} using that
     * child fragment. Similarly, if the resource will be used in a view in the parent fragment, the
     * load should be started with [.with] using the parent fragment. In
     * the same vein, if the resource will be used in a view in an activity, the load should be
     * started with [.with]}.
     *
     *
     *  This method is appropriate for resources that will be used outside of the normal fragment
     * or activity lifecycle (For example in services, or for notification thumbnails).
     *
     * @param context Any context, will not be retained.
     * @return A RequestManager for the top level application that can be used to start a load.
     * @see .with
     * @see .with
     * @see .with
     * @see .with
     */
    fun with(context: Context): RequestManager? {
        return try{
            Glide.with(context)
        }catch(e: IllegalArgumentException){
            null
        }
    }

    /**
     * Begin a load with Glide that will be tied to the given [android.app.Activity]'s lifecycle
     * and that uses the given [Activity]'s default options.
     *
     * @param activity The activity to use.
     * @return A RequestManager for the given activity that can be used to start a load.
     */
    fun with(activity: Activity): RequestManager? {
        return try{
            Glide.with(activity)
        }catch(e: IllegalArgumentException){
            null
        }
    }

    /**
     * Begin a load with Glide that will tied to the give
     * [android.support.v4.app.FragmentActivity]'s lifecycle and that uses the given
     * [android.support.v4.app.FragmentActivity]'s default options.
     *
     * @param activity The activity to use.
     * @return A RequestManager for the given FragmentActivity that can be used to start a load.
     */
    fun with(activity: androidx.fragment.app.FragmentActivity): RequestManager? {
        return try{
            Glide.with(activity)
        }catch(e: IllegalArgumentException){
            null
        }
    }

    /**
     * Begin a load with Glide that will be tied to the given
     * [android.support.v4.app.Fragment]'s lifecycle and that uses the given
     * [android.support.v4.app.Fragment]'s default options.
     *
     * @param fragment The fragment to use.
     * @return A RequestManager for the given Fragment that can be used to start a load.
     */
    fun with(fragment: androidx.fragment.app.Fragment): RequestManager? {
        return try{
            Glide.with(fragment)
        }catch(e: IllegalArgumentException){
            null
        }
    }

    /**
     * Begin a load with Glide that will be tied to the given [android.app.Fragment]'s lifecycle
     * and that uses the given [android.app.Fragment]'s default options.
     *
     * @param fragment The fragment to use.
     * @return A RequestManager for the given Fragment that can be used to start a load.
     */
    @Deprecated("Prefer support Fragments and {@link #with(Fragment)} instead,\n" +
            "    {@link android.app.Fragment} will be deprecated. See\n" +
            "    https://github.com/android/android-ktx/pull/161#issuecomment-363270555.")
    fun with(fragment: android.app.Fragment): RequestManager? {
        return try{
            Glide.with(fragment)
        }catch(e: IllegalArgumentException){
            null
        }
    }

    /**
     * Begin a load with Glide that will be tied to the lifecycle of the [Fragment],
     * [android.app.Fragment], or [Activity] that contains the View.
     *
     *
     * A [Fragment] or [android.app.Fragment] is assumed to contain a View if the View
     * is a child of the View returned by the [Fragment.getView]} method.
     *
     *
     * This method will not work if the View is not attached. Prefer the Activity and Fragment
     * variants unless you're loading in a View subclass.
     *
     *
     * This method may be inefficient aways and is definitely inefficient for large hierarchies.
     * Consider memoizing the result after the View is attached or again, prefer the Activity and
     * Fragment variants whenever possible.
     *
     *
     * When used in Applications that use the non-support [android.app.Fragment] classes,
     * calling this method will produce noisy logs from [android.app.FragmentManager]. Consider
     * avoiding entirely or using the [Fragment]s from the support library instead.
     *
     *
     * If the support [FragmentActivity] class is used, this method will only attempt to
     * discover support [Fragment]s. Any non-support [android.app.Fragment]s attached
     * to the [FragmentActivity] will be ignored.
     *
     * @param view The view to search for a containing Fragment or Activity from.
     * @return A RequestManager that can be used to start a load.
     */
    fun with(view: View): RequestManager? {
        return try{
            Glide.with(view)
        }catch(e: IllegalArgumentException){
            null
        }
    }
}