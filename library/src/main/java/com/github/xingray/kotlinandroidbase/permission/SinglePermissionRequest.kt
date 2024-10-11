package com.github.xingray.kotlinandroidbase.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

class SinglePermissionRequest(
    val mPermission: String
) {
    private var mResultLauncher: ActivityResultLauncher<String>? = null
    private var mPermissionDeniedCallback: ((String) -> Unit)? = null
    private var mPermissionPermittedCallback: (() -> Unit)? = null
    fun register(activity: ComponentActivity) {
        mResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    mPermissionPermittedCallback?.invoke()
                } else {
                    mPermissionDeniedCallback?.invoke(mPermission)
                }
            }
    }

    fun perform(
        context: Context,
        permissionDeniedCallback: ((String) -> Unit)?,
        permissionPermittedCallback: () -> Unit
    ) {
        this.mPermissionPermittedCallback = permissionPermittedCallback
        this.mPermissionDeniedCallback = permissionDeniedCallback
        checkPermission(context, mPermission)
    }

    private fun checkPermission(context: Context, permission: String) {
        val result = ActivityCompat.checkSelfPermission(context, permission)
        if (result != PackageManager.PERMISSION_GRANTED) {
            mResultLauncher?.launch(permission)
        } else {
            mPermissionPermittedCallback?.invoke()
        }
    }
}