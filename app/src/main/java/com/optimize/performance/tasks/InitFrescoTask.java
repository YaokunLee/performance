package com.optimize.performance.tasks;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.optimize.performance.launchstarter.task.Task;

public class InitFrescoTask extends Task {

    @Override
    public void run() {
        Fresco.initialize(mContext);
    }
}
