package com.github.xingray.kotlinandroidbase.filechoose

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

private val TAG = StringChoose::class.simpleName


class StringChoose {

    companion object {
        val dataStringKey = "data_string_key"
    }

    private var mCancelCallback: ((Int, String) -> Unit)? = null
    private var mSuccessCallback: ((String) -> Unit)? = null

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
            val resultDataString = result.data?.getStringExtra(dataStringKey)?.takeIf { it.isNotEmpty() } ?: run {
                mCancelCallback?.invoke(resultCode, "result.data is null or empty")
                return@registerForActivityResult
            }

            Log.d(TAG, "resultDataString:${resultDataString}")
            mSuccessCallback?.invoke(resultDataString)
        }
    }

    fun perform(cancelCallback: ((Int, String) -> Unit)?, successCallback: (String) -> Unit) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        perform(intent, cancelCallback, successCallback)
    }

    fun perform(intent: Intent, cancelCallback: ((Int, String) -> Unit)?, successCallback: (String) -> Unit) {
        this.mSuccessCallback = successCallback
        this.mCancelCallback = cancelCallback
        mPickImageLauncher?.launch(intent)
    }
}