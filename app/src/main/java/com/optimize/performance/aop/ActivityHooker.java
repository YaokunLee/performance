package com.optimize.performance.aop;

import android.os.Bundle;

import com.optimize.performance.utils.LogUtils;

import me.ele.lancet.base.Origin;
import me.ele.lancet.base.Scope;
import me.ele.lancet.base.annotations.Insert;
import me.ele.lancet.base.annotations.Proxy;
import me.ele.lancet.base.annotations.TargetClass;

public class ActivityHooker {

    public static ActivityRecord sActivityRecord;

    static {
        sActivityRecord = new ActivityRecord();
    }

    @Insert(value = "onCreate",mayCreateSuper = true)
    @TargetClass(value = "android.support.v7.app.AppCompatActivity",scope = Scope.ALL)
    protected void onCreate(Bundle savedInstanceState) {
        sActivityRecord.mOnCreateTime = System.currentTimeMillis();
        Origin.callVoid();
    }

    @Insert(value = "onWindowFocusChanged",mayCreateSuper = true)
    @TargetClass(value = "android.support.v7.app.AppCompatActivity",scope = Scope.ALL)
    public void onWindowFocusChanged(boolean hasFocus) {
        sActivityRecord.mOnWindowsFocusChangedTime = System.currentTimeMillis();
        LogUtils.i("onWindowFocusChanged cost "+(sActivityRecord.mOnWindowsFocusChangedTime - sActivityRecord.mOnCreateTime));
        Origin.callVoid();
    }

    @Proxy("i")
    @TargetClass("android.util.Log")
    public static int i(String tag, String msg) {
        msg = msg + "";
        return (int) Origin.call();
    }


}
