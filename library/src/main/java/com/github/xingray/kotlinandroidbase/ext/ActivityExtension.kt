package com.github.xingray.kotlinandroidbase.ext

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.widget.Toast

private val TAG = "ActivityExtension"

fun Activity.gotoActivity(cls: Class<out Activity>) {
    val intent = Intent()
    intent.setClass(baseContext, cls)
    startActivity(intent)
}

//@Suppress("UnusedReceiverParameter")
//fun Activity.runOnIoThread(task: () -> Unit) {
//    TaskExecutor.io(task)
//}
//
//
//fun Activity.runOnUiThreadDelayed(timeInMills: Long, task: () -> Unit) {
//    TaskExecutor.uiHandler.postDelayed({
//        runOnUiThread {
//            task.invoke()
//        }
//    }, timeInMills)
//}

fun Activity.showToast(msg: String) {
    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
}

@Suppress("unused")
fun Activity.showLongToast(msg: String) {
    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
}

fun Activity.showMessage(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Activity.finishWithString(key: String, value: String) {
    val intent = Intent()
    intent.putExtra(key, value)
    setResult(RESULT_OK, intent)
    finish()
}

fun Activity.finishWithCancel() {
    setResult(RESULT_CANCELED)
    finish()
}

//fun Activity.runOnIoThreadWithLoading(task: () -> Unit) {
//    runOnIoThreadWithLoading("处理中", task)
//}

//fun Activity.runOnIoThreadWithLoading(msg: String, task: () -> Unit) {
//    val loadingDialog = LoadingDialog(this, msg)
//    loadingDialog.show()
//    runOnIoThread {
//        task()
//        runOnUiThread {
//            loadingDialog.dismiss()
//        }
//    }
//}
//
//fun Activity.showLoading(msg: String = "处理中"): Dialog {
//    val loadingDialog = LoadingDialog(this, msg)
//    loadingDialog.show()
//    return loadingDialog
//}

fun Activity.startActivityByClass(target: Class<out Activity>) {
    val intent = Intent()
    intent.setClass(baseContext, target)
    startActivity(intent)
}