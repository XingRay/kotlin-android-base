package com.github.xingray.kotlinandroidbase.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

class SystemCamera {

    companion object {
        @JvmStatic
        private val TAG = SystemCamera::class.java.simpleName
    }

    private var mCancelCallback: ((Int, String) -> Unit)? = null
    private var mSuccessCallback: ((Uri) -> Unit)? = null
    private var mSuccessCallback2: ((Bitmap) -> Unit)? = null

    private var mPickImageLauncher: ActivityResultLauncher<Intent>? = null

    private lateinit var mContext: Context
    private var mImageUri: Uri? = null

    fun onCreate(activity: ComponentActivity) {
        mContext = activity.baseContext

        mPickImageLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "registerForActivityResult: onResult: result:${result}")
            val resultCode = result.resultCode
            if (resultCode != Activity.RESULT_OK) {
                val cancelCallback = mCancelCallback
                if (cancelCallback == null) {
                    Log.d(TAG, "cancelCallback is null, resultCode:${resultCode}")
                } else {
                    cancelCallback.invoke(resultCode, "result code is not Activity.RESULT_OK")
                }

                return@registerForActivityResult
            }

            val imageUri = mImageUri
            if (imageUri != null) {
                mSuccessCallback?.invoke(imageUri)
                return@registerForActivityResult
            }

            val resultData = result.data ?: run {
                mCancelCallback?.invoke(resultCode, "result.data is null")
                return@registerForActivityResult
            }
            Log.d(TAG, "onCreate: result.data:${resultData}")
            val bundle = resultData.extras
            bundle?.keySet()?.forEach { key ->
                val value = bundle.get(key)
                Log.d(TAG, "onCreate: key:${key}, value:${value}")
                if (value != null && value is Bitmap) {
                    mSuccessCallback2?.invoke(value)
                    return@registerForActivityResult
                }
            }

            val resultDataData = resultData.data
            Log.d(TAG, "onCreate: result.data.data:${resultDataData}")
            if (resultDataData == null) {
                val cancelCallback = mCancelCallback
                if (cancelCallback == null) {
                    Log.d(TAG, "cancelCallback is null, resultCode:${resultCode}, uri is null")
                } else {
                    cancelCallback.invoke(resultCode, "uri is null")
                }
                return@registerForActivityResult
            }

            val uri = resultDataData
            val successCallback = mSuccessCallback
            if (successCallback == null) {
                Log.d(TAG, "register: successCallback is null, uri:${uri}")
            } else {
                successCallback.invoke(uri)
            }
        }
    }

    fun perform(
        imageFile: File,
        cancelCallback: ((Int, String) -> Unit)?,
        successCallback: (Uri) -> Unit
    ) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        mImageUri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            imageFile.toUri()
        } else {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            FileProvider.getUriForFile(mContext, mContext.packageName + ".fileprovider", imageFile)
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri)
        perform(intent, cancelCallback, successCallback)
    }

    fun perform(
        intent: Intent,
        cancelCallback: ((Int, String) -> Unit)?,
        successCallback: (Uri) -> Unit,
        successCallback2: ((Bitmap) -> Unit)? = null
    ) {
        this.mSuccessCallback = successCallback
        this.mSuccessCallback2 = successCallback2
        this.mCancelCallback = cancelCallback
        mPickImageLauncher?.launch(intent)
    }
}