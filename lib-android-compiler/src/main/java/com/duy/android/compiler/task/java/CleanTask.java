package com.duy.android.compiler.task.java;

import com.duy.android.compiler.builder.IBuilder;
import com.duy.android.compiler.task.ABuildTask;
import com.duy.android.compiler.file.java.JavaProject;

public class CleanTask extends ABuildTask<JavaProject> {

    public CleanTask(IBuilder<? extends JavaProject> builder) {
        super(builder);
    }

    @Override
    public String getTaskName() {
        return "Clean";
    }

    @Override
    public boolean run() throws Exception {
        project.clean();
        return true;
    }
}
