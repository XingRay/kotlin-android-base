package com.github.xingray.kotlinandroidbase.filechoose

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

private val TAG = FileChoose::class.simpleName

class FileChoose {

    private var mCancelCallback: ((Int, String) -> Unit)? = null
    private var mSuccessCallback: ((Uri) -> Unit)? = null

    private var mPickImageLauncher: ActivityResultLauncher<Intent>? = null

    fun onCreate(activity: ComponentActivity) {
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
            val resultData = result.data ?: run {
                mCancelCallback?.invoke(resultCode, "result.data is null")
                return@registerForActivityResult
            }
            val uri = resultData.data ?: run {
                val cancelCallback = mCancelCallback
                if (cancelCallback == null) {
                    Log.d(TAG, "cancelCallback is null, resultCode:${resultCode}, uri is null")
                } else {
                    cancelCallback.invoke(resultCode, "uri is null")
                }
                return@registerForActivityResult
            }
            val successCallback = mSuccessCallback
            if (successCallback == null) {
                Log.d(TAG, "register: successCallback is null, uri:${uri}")
            } else {
                successCallback.invoke(uri)
            }
        }
    }

    fun perform(
        cancelCallback: ((Int, String) -> Unit)?,
        successCallback: (Uri) -> Unit
    ) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        perform(intent, cancelCallback, successCallback)
    }

    fun perform(
        intent: Intent,
        cancelCallback: ((Int, String) -> Unit)?,
        successCallback: (Uri) -> Unit
    ) {
        this.mSuccessCallback = successCallback
        this.mCancelCallback = cancelCallback
        mPickImageLauncher?.launch(intent)
    }
}