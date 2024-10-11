package com.github.xingray.kotlinandroidbase.permission

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

val TAG = MultiPermissionRequest::class.simpleName

class MultiPermissionRequest(val mPermissions: Array<out String>) {

    private var mMultiPermissionResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private var mPermissionDeniedCallback: ((permissions: Array<String>) -> Unit)? = null
    private var mPermissionPermittedCallback: (() -> Unit)? = null
    private var mSinglePermissionRequest: SinglePermissionRequest? = null

    init {
        if (mPermissions.size == 1) {
            mSinglePermissionRequest = SinglePermissionRequest(mPermissions[0])
        } else {
            Log.w(TAG, "mPermissions.size is ${mPermissions.size}")
        }
    }

    fun register(activity: ComponentActivity) {
        val permissions = this.mPermissions
        if (permissions.isEmpty()) {
            return
        }
        if (permissions.size == 1) {
            mSinglePermissionRequest?.register(activity)
            return
        }

        mMultiPermissionResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResult ->
                val deniedPermissions = mutableListOf<String>()
                for (result in permissionResult) {
                    val permission = result.key
                    val granted = result.value
                    if (!granted) {
                        deniedPermissions.add(permission)
                    }
                }

                if (deniedPermissions.isEmpty()) {
                    mPermissionPermittedCallback?.invoke()
                } else {
                    mPermissionDeniedCallback?.invoke(deniedPermissions.toTypedArray())
                }
            }
    }

    fun perform(
        context: Context,
        permissionDeniedCallback: ((Array<String>) -> Unit)?,
        permissionsPermittedCallback: () -> Unit
    ) {
        if (mPermissions.isEmpty()) {
            permissionsPermittedCallback.invoke()
            return
        } else if (mPermissions.size == 1) {
            mSinglePermissionRequest?.perform(context, { deniedPermission ->
                permissionDeniedCallback?.invoke(arrayOf(deniedPermission))
            }, permissionsPermittedCallback)
            return
        }

        this.mPermissionPermittedCallback = permissionsPermittedCallback
        this.mPermissionDeniedCallback = permissionDeniedCallback

        checkPermission(context)
    }

    private fun checkPermission(context: Context) {
        val permissionsNeedRequest = mutableListOf<String>()
        for (permission in mPermissions) {
            val result = ActivityCompat.checkSelfPermission(context, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionsNeedRequest.add(permission)
            }
        }

        if (permissionsNeedRequest.isEmpty()) {
            mPermissionPermittedCallback?.invoke()
        } else {
            mMultiPermissionResultLauncher?.launch(permissionsNeedRequest.toTypedArray())
        }
    }
}